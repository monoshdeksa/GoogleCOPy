package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.OrderHistoryRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CopiedOrderEntity
import com.example.ocr.MatchConfidence
import com.example.ocr.OrderExtractionResult
import com.example.ocr.ScreenshotOcrEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    SCAN,
    HISTORY
}

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data class Success(
        val bitmap: Bitmap,
        val extraction: OrderExtractionResult
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class MainUiState(
    val selectedTab: AppTab = AppTab.SCAN,
    val scanState: ScanUiState = ScanUiState.Idle,
    val orderNumberDraft: String = "",
    val isEditingOrder: Boolean = false,
    val includeLabelPrefix: Boolean = false,
    val showTextBoxes: Boolean = true,
    val justCopiedBanner: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrEngine = ScreenshotOcrEngine(application)
    private val repository = OrderHistoryRepository(
        AppDatabase.getInstance(application).copiedOrderDao()
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val recentOrders: StateFlow<List<CopiedOrderEntity>> = repository.observeRecentOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private var copiedResetJob: Job? = null

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun processScreenshotUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedTab = AppTab.SCAN,
                    scanState = ScanUiState.Scanning,
                    isEditingOrder = false,
                    justCopiedBanner = false
                )
            }
            try {
                val output = ocrEngine.scanUri(uri)
                if (output.extraction.detectedLines.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            scanState = ScanUiState.Error(
                                getApplication<Application>().getString(R.string.error_no_text)
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            scanState = ScanUiState.Success(
                                bitmap = output.bitmap,
                                extraction = output.extraction
                            ),
                            orderNumberDraft = output.extraction.bestOrderNumber.orEmpty(),
                            isEditingOrder = output.extraction.bestOrderNumber.isNullOrBlank()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanState = ScanUiState.Error(
                            getApplication<Application>().getString(R.string.error_load_image)
                        )
                    )
                }
            }
        }
    }

    fun onOrderDraftChanged(value: String) {
        _uiState.update { it.copy(orderNumberDraft = value, justCopiedBanner = false) }
    }

    fun toggleEditingOrder() {
        _uiState.update { it.copy(isEditingOrder = !it.isEditingOrder) }
    }

    fun setIncludeLabelPrefix(include: Boolean) {
        _uiState.update { it.copy(includeLabelPrefix = include, justCopiedBanner = false) }
    }

    fun toggleShowTextBoxes() {
        _uiState.update { it.copy(showTextBoxes = !it.showTextBoxes) }
    }

    fun selectCandidate(candidate: String) {
        _uiState.update {
            it.copy(
                orderNumberDraft = candidate,
                isEditingOrder = false,
                justCopiedBanner = false
            )
        }
    }

    fun copyPrimaryOrder() {
        val current = _uiState.value
        val number = current.orderNumberDraft.trim()
        if (number.isEmpty()) return

        val formatted = if (current.includeLabelPrefix) {
            "No Pesanan Penerima: $number"
        } else {
            number
        }

        val matchedLabel = (current.scanState as? ScanUiState.Success)?.extraction?.matchedLabel
        copyToClipboard(
            orderNumber = number,
            textToCopy = formatted,
            matchedLabel = matchedLabel ?: getConfidenceLabel((current.scanState as? ScanUiState.Success)?.extraction?.confidence),
            recordHistory = true,
            isPrimary = true
        )
    }

    fun copyRawLineOrCandidate(text: String, recordHistory: Boolean = false) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        copyToClipboard(
            orderNumber = cleaned,
            textToCopy = cleaned,
            matchedLabel = "Teks Terdeteksi",
            recordHistory = recordHistory,
            isPrimary = false
        )
    }

    fun copyHistoryItem(item: CopiedOrderEntity) {
        copyToClipboard(
            orderNumber = item.orderNumber,
            textToCopy = item.copiedText,
            matchedLabel = item.matchedLabel,
            recordHistory = true,
            isPrimary = false
        )
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun resetToIdle() {
        _uiState.update {
            it.copy(
                scanState = ScanUiState.Idle,
                orderNumberDraft = "",
                isEditingOrder = false,
                justCopiedBanner = false
            )
        }
    }

    private fun copyToClipboard(
        orderNumber: String,
        textToCopy: String,
        matchedLabel: String?,
        recordHistory: Boolean,
        isPrimary: Boolean
    ) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("No Pesanan Penerima", textToCopy)
        clipboard.setPrimaryClip(clip)

        if (isPrimary) {
            copiedResetJob?.cancel()
            _uiState.update { it.copy(justCopiedBanner = true, isEditingOrder = false) }
            copiedResetJob = viewModelScope.launch {
                delay(2_200)
                _uiState.update { it.copy(justCopiedBanner = false) }
            }
        }

        viewModelScope.launch {
            if (recordHistory) {
                repository.recordCopy(
                    orderNumber = orderNumber,
                    copiedText = textToCopy,
                    matchedLabel = matchedLabel
                )
            }
            _snackbarEvents.emit(context.getString(R.string.msg_copied_prefix, textToCopy))
        }
    }

    private fun getConfidenceLabel(confidence: MatchConfidence?): String? {
        return when (confidence) {
            MatchConfidence.EXACT_LABEL -> "No. Pesanan Penerima"
            MatchConfidence.CANDIDATE_PATTERN -> "Kandidat Nomor"
            else -> null
        }
    }
}
