package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MainUiState
import com.example.ui.ScanUiState
import com.example.ui.components.DetectedLinesSection
import com.example.ui.components.InteractiveScreenshotViewer
import com.example.ui.components.OrderResultCard

@Composable
fun ScanScreen(
    uiState: MainUiState,
    onPickScreenshotClick: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onToggleIncludeLabel: (Boolean) -> Unit,
    onCopyPrimary: () -> Unit,
    onToggleTextBoxes: () -> Unit,
    onSelectCandidate: (String) -> Unit,
    onCopyRawLine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                )
            )
    ) {
        val isWide = maxWidth >= 720.dp

        when (val scanState = uiState.scanState) {
            ScanUiState.Idle -> {
                IdleScanContent(
                    onPickScreenshotClick = onPickScreenshotClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }

            ScanUiState.Scanning -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 420.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.scanning_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.scanning_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is ScanUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 460.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .size(32.dp)
                                )
                            }
                            Text(
                                text = scanState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onPickScreenshotClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("retry_pick_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_pick_screenshot),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            is ScanUiState.Success -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InteractiveScreenshotViewer(
                                    bitmap = scanState.bitmap,
                                    detectedLines = scanState.extraction.detectedLines,
                                    imageWidth = scanState.extraction.imageWidth,
                                    imageHeight = scanState.extraction.imageHeight,
                                    showTextBoxes = uiState.showTextBoxes,
                                    onToggleTextBoxes = onToggleTextBoxes,
                                    onLineTapped = { line ->
                                        onCopyRawLine(line.text)
                                    }
                                )
                                FilledTonalButton(
                                    onClick = onPickScreenshotClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("change_screenshot_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_change_screenshot))
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OrderResultCard(
                                    orderNumberDraft = uiState.orderNumberDraft,
                                    confidence = scanState.extraction.confidence,
                                    matchedLabel = scanState.extraction.matchedLabel,
                                    isEditing = uiState.isEditingOrder,
                                    includeLabelPrefix = uiState.includeLabelPrefix,
                                    justCopied = uiState.justCopiedBanner,
                                    onDraftChanged = onDraftChanged,
                                    onToggleEdit = onToggleEdit,
                                    onToggleIncludeLabel = onToggleIncludeLabel,
                                    onCopyClick = onCopyPrimary
                                )

                                DetectedLinesSection(
                                    candidates = scanState.extraction.allCandidates,
                                    selectedOrderNumber = uiState.orderNumberDraft,
                                    detectedLines = scanState.extraction.detectedLines,
                                    onCandidateSelected = onSelectCandidate,
                                    onCopyLine = onCopyRawLine
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 600.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OrderResultCard(
                                    orderNumberDraft = uiState.orderNumberDraft,
                                    confidence = scanState.extraction.confidence,
                                    matchedLabel = scanState.extraction.matchedLabel,
                                    isEditing = uiState.isEditingOrder,
                                    includeLabelPrefix = uiState.includeLabelPrefix,
                                    justCopied = uiState.justCopiedBanner,
                                    onDraftChanged = onDraftChanged,
                                    onToggleEdit = onToggleEdit,
                                    onToggleIncludeLabel = onToggleIncludeLabel,
                                    onCopyClick = onCopyPrimary
                                )

                                FilledTonalButton(
                                    onClick = onPickScreenshotClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("change_screenshot_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.btn_change_screenshot),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                InteractiveScreenshotViewer(
                                    bitmap = scanState.bitmap,
                                    detectedLines = scanState.extraction.detectedLines,
                                    imageWidth = scanState.extraction.imageWidth,
                                    imageHeight = scanState.extraction.imageHeight,
                                    showTextBoxes = uiState.showTextBoxes,
                                    onToggleTextBoxes = onToggleTextBoxes,
                                    onLineTapped = { line ->
                                        onCopyRawLine(line.text)
                                    }
                                )

                                DetectedLinesSection(
                                    candidates = scanState.extraction.allCandidates,
                                    selectedOrderNumber = uiState.orderNumberDraft,
                                    detectedLines = scanState.extraction.detectedLines,
                                    onCandidateSelected = onSelectCandidate,
                                    onCopyLine = onCopyRawLine
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleScanContent(
    onPickScreenshotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scan_hero),
                        contentDescription = stringResource(R.string.cd_hero_illustration),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.hero_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onPickScreenshotClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("pick_screenshot_button"),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.btn_pick_screenshot),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.tip_share_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.tip_share_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
