package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalinOrderApp(
    sharedImageUri: Uri?,
    onSharedImageConsumed: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentOrders by viewModel.recentOrders.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.processScreenshotUri(it) }
        }
    )

    val launchPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            viewModel.processScreenshotUri(sharedImageUri)
            onSharedImageConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    BackHandler(enabled = uiState.selectedTab == AppTab.HISTORY || uiState.scanState != ScanUiState.Idle) {
        if (uiState.selectedTab == AppTab.HISTORY) {
            viewModel.selectTab(AppTab.SCAN)
        } else if (uiState.scanState != ScanUiState.Idle) {
            viewModel.resetToIdle()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.SCAN,
                            onClick = { viewModel.selectTab(AppTab.SCAN) },
                            modifier = Modifier.testTag("nav_scan"),
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == AppTab.SCAN) {
                                        Icons.Filled.DocumentScanner
                                    } else {
                                        Icons.Outlined.DocumentScanner
                                    },
                                    contentDescription = stringResource(R.string.nav_scan)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_scan)) }
                        )
                        NavigationBarItem(
                            selected = uiState.selectedTab == AppTab.HISTORY,
                            onClick = { viewModel.selectTab(AppTab.HISTORY) },
                            modifier = Modifier.testTag("nav_history"),
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == AppTab.HISTORY) {
                                        Icons.Filled.History
                                    } else {
                                        Icons.Outlined.History
                                    },
                                    contentDescription = stringResource(R.string.nav_history)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_history)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationRailItem(
                            selected = uiState.selectedTab == AppTab.SCAN,
                            onClick = { viewModel.selectTab(AppTab.SCAN) },
                            modifier = Modifier.testTag("nav_scan"),
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == AppTab.SCAN) {
                                        Icons.Filled.DocumentScanner
                                    } else {
                                        Icons.Outlined.DocumentScanner
                                    },
                                    contentDescription = stringResource(R.string.nav_scan)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_scan)) }
                        )
                        NavigationRailItem(
                            selected = uiState.selectedTab == AppTab.HISTORY,
                            onClick = { viewModel.selectTab(AppTab.HISTORY) },
                            modifier = Modifier.testTag("nav_history"),
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == AppTab.HISTORY) {
                                        Icons.Filled.History
                                    } else {
                                        Icons.Outlined.History
                                    },
                                    contentDescription = stringResource(R.string.nav_history)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_history)) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState.selectedTab) {
                        AppTab.SCAN -> {
                            ScanScreen(
                                uiState = uiState,
                                onPickScreenshotClick = launchPicker,
                                onDraftChanged = viewModel::onOrderDraftChanged,
                                onToggleEdit = viewModel::toggleEditingOrder,
                                onToggleIncludeLabel = viewModel::setIncludeLabelPrefix,
                                onCopyPrimary = viewModel::copyPrimaryOrder,
                                onToggleTextBoxes = viewModel::toggleShowTextBoxes,
                                onSelectCandidate = viewModel::selectCandidate,
                                onCopyRawLine = { lineText ->
                                    viewModel.copyRawLineOrCandidate(lineText, recordHistory = false)
                                }
                            )
                        }

                        AppTab.HISTORY -> {
                            HistoryScreen(
                                orders = recentOrders,
                                onCopyItem = viewModel::copyHistoryItem,
                                onDeleteItem = viewModel::deleteHistoryItem,
                                onClearAll = viewModel::clearHistory
                            )
                        }
                    }
                }
            }
        }
    }
}
