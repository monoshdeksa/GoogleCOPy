package com.example.ocr

data class TextRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(1)
    val height: Int get() = (bottom - top).coerceAtLeast(1)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class OcrLine(
    val id: String,
    val text: String,
    val rect: TextRect,
    val isOrderCandidate: Boolean = false,
    val isPrimaryMatch: Boolean = false
)

enum class MatchConfidence {
    EXACT_LABEL,
    CANDIDATE_PATTERN,
    NOT_FOUND
}

data class OrderExtractionResult(
    val bestOrderNumber: String?,
    val matchedLabel: String?,
    val confidence: MatchConfidence,
    val allCandidates: List<String>,
    val detectedLines: List<OcrLine>,
    val fullRawText: String,
    val imageWidth: Int,
    val imageHeight: Int
)
