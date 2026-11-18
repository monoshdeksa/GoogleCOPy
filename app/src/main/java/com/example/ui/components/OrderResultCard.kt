package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ocr.MatchConfidence
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun OrderResultCard(
    orderNumberDraft: String,
    confidence: MatchConfidence,
    matchedLabel: String?,
    isEditing: Boolean,
    includeLabelPrefix: Boolean,
    justCopied: Boolean,
    onDraftChanged: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onToggleIncludeLabel: (Boolean) -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val buttonContainerColor by animateColorAsState(
        targetValue = if (justCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = spring(),
        label = "copyButtonColor"
    )

    val badgeContainerColor = when (confidence) {
        MatchConfidence.EXACT_LABEL -> MaterialTheme.colorScheme.tertiaryContainer
        MatchConfidence.CANDIDATE_PATTERN -> MaterialTheme.colorScheme.primaryContainer
        MatchConfidence.NOT_FOUND -> MaterialTheme.colorScheme.surfaceVariant
    }

    val badgeContentColor = when (confidence) {
        MatchConfidence.EXACT_LABEL -> MaterialTheme.colorScheme.onTertiaryContainer
        MatchConfidence.CANDIDATE_PATTERN -> MaterialTheme.colorScheme.onPrimaryContainer
        MatchConfidence.NOT_FOUND -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val badgeText = when (confidence) {
        MatchConfidence.EXACT_LABEL -> stringResource(R.string.badge_auto_detected)
        MatchConfidence.CANDIDATE_PATTERN -> stringResource(R.string.badge_candidate)
        MatchConfidence.NOT_FOUND -> stringResource(R.string.badge_manual)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = matchedLabel ?: stringResource(R.string.label_order_number),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = badgeContainerColor,
                        contentColor = badgeContentColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (confidence == MatchConfidence.EXACT_LABEL) {
                                    Icons.Filled.CheckCircle
                                } else if (confidence == MatchConfidence.CANDIDATE_PATTERN) {
                                    Icons.Outlined.AutoAwesome
                                } else {
                                    Icons.Outlined.Info
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_order_button")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_edit_order),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = orderNumberDraft,
                    onValueChange = onDraftChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_number_input"),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_order_number)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    SelectionContainer {
                        Text(
                            text = orderNumberDraft.ifBlank { "—" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .testTag("detected_order_number_text"),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !includeLabelPrefix,
                    onClick = { onToggleIncludeLabel(false) },
                    label = { Text(stringResource(R.string.chip_number_only)) },
                    modifier = Modifier.testTag("chip_number_only"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                FilterChip(
                    selected = includeLabelPrefix,
                    onClick = { onToggleIncludeLabel(true) },
                    label = { Text(stringResource(R.string.chip_with_label)) },
                    modifier = Modifier.testTag("chip_with_label"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCopyClick()
                },
                enabled = orderNumberDraft.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("copy_order_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                AnimatedContent(
                    targetState = justCopied,
                    label = "copyButtonContent"
                ) { copied ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (copied) {
                                stringResource(R.string.btn_copied)
                            } else {
                                stringResource(R.string.btn_copy_order)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
