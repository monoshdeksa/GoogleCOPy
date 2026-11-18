package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ocr.OcrLine
import kotlin.math.roundToInt

@Composable
fun InteractiveScreenshotViewer(
    bitmap: Bitmap,
    detectedLines: List<OcrLine>,
    imageWidth: Int,
    imageHeight: Int,
    showTextBoxes: Boolean,
    onToggleTextBoxes: () -> Unit,
    onLineTapped: (OcrLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val haptic = LocalHapticFeedback.current
    val aspectRatio = remember(imageWidth, imageHeight) {
        if (imageHeight > 0 && imageWidth > 0) {
            imageWidth.toFloat() / imageHeight.toFloat()
        } else {
            9f / 16f
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.section_preview),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showTextBoxes) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.hint_tap_box),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                FilterChip(
                    selected = showTextBoxes,
                    onClick = onToggleTextBoxes,
                    modifier = Modifier.testTag("toggle_text_boxes_chip"),
                    label = { Text(stringResource(R.string.toggle_text_boxes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showTextBoxes) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(aspectRatio)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val density = LocalDensity.current
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(R.string.cd_screenshot_preview),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (showTextBoxes && imageWidth > 0 && imageHeight > 0) {
                        val scaleX = containerWidthPx / imageWidth.toFloat()
                        val scaleY = containerHeightPx / imageHeight.toFloat()

                        detectedLines.forEach { line ->
                            val leftPx = (line.rect.left * scaleX).roundToInt()
                            val topPx = (line.rect.top * scaleY).roundToInt()
                            val widthDp = with(density) { (line.rect.width * scaleX).toDp() }
                            val heightDp = with(density) { (line.rect.height * scaleY).toDp() }

                            val borderColor = when {
                                line.isPrimaryMatch -> MaterialTheme.colorScheme.tertiary
                                line.isOrderCandidate -> MaterialTheme.colorScheme.primary
                                else -> Color.White.copy(alpha = 0.75f)
                            }
                            val fillColor = when {
                                line.isPrimaryMatch -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)
                                line.isOrderCandidate -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else -> Color.Black.copy(alpha = 0.10f)
                            }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(leftPx, topPx) }
                                    .size(
                                        width = widthDp.coerceAtLeast(16.dp),
                                        height = heightDp.coerceAtLeast(14.dp)
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(fillColor)
                                    .border(
                                        width = if (line.isPrimaryMatch || line.isOrderCandidate) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onLineTapped(line)
                                    }
                                    .semantics {
                                        contentDescription = line.text
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
