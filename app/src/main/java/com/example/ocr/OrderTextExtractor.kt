package com.example.ocr

import kotlin.math.abs
import kotlin.math.max

object OrderTextExtractor {

    private val recipientOrderLabelRegex = Regex(
        pattern = """(?i)\b(?:n[o0]\.?|nomor|nomer)?\s*pesanan\s*pener[i1l]ma\b"""
    )

    private val generalOrderLabelRegex = Regex(
        pattern = """(?i)\b(?:(?:n[o0]\.?|nomor|nomer|id)\s*(?:pesanan|order)|order\s*id|kode\s*(?:pesanan|pengambilan))\b"""
    )

    private val recipientWordRegex = Regex("""(?i)\bpener[i1l]ma\b""")
    private val phoneRegex = Regex("""^(?:\+?62|0)8\d{7,12}$""")
    private val timeRegex = Regex("""^\d{1,2}[:.]\d{2}(?:[:.]\d{2})?$""")
    private val dateRegex = Regex("""^\d{1,2}[/-]\d{1,2}[/-]\d{2,4}$""")
    private val currencyRegex = Regex("""(?i)^rp\.?\s*[\d.,]+$""")
    private val distanceRegex = Regex("""(?i)^\d+(?:[.,]\d+)?\s*(?:km|m)$""")

    private val hashtagCodeRegex = Regex("""#[A-Za-z0-9][A-Za-z0-9\-]{1,19}""")
    private val orderTokenRegex = Regex("""\b[A-Za-z0-9][A-Za-z0-9\-]{4,24}\b""")

    fun extract(
        rawLines: List<OcrLine>,
        imageWidth: Int,
        imageHeight: Int
    ): OrderExtractionResult {
        val sortedLines = rawLines
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy<OcrLine> { it.rect.top }.thenBy { it.rect.left })

        if (sortedLines.isEmpty()) {
            return OrderExtractionResult(
                bestOrderNumber = null,
                matchedLabel = null,
                confidence = MatchConfidence.NOT_FOUND,
                allCandidates = emptyList(),
                detectedLines = emptyList(),
                fullRawText = "",
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        }

        var bestOrder: String? = null
        var bestLabel: String? = null
        var bestLineId: String? = null
        var bestScore = -1

        // Pass 1: Look for explicit anchor labels ("No Pesanan Penerima", "No Pesanan", "Order ID", etc.)
        for (line in sortedLines) {
            val text = line.text.trim()
            val recipientMatch = recipientOrderLabelRegex.find(text)
            val generalMatch = generalOrderLabelRegex.find(text)

            val matchResult = recipientMatch ?: generalMatch
            val baseScore = when {
                recipientMatch != null -> 100
                generalMatch != null -> 80
                else -> 0
            }

            if (matchResult != null && baseScore > bestScore) {
                val remainder = text.substring(matchResult.range.last + 1)
                    .trimStart(' ', ':', '-', '=', '.', '>')
                    .trim()

                val inlineToken = extractOrderTokenFromValue(remainder, allowShortCode = true)
                if (inlineToken != null) {
                    bestOrder = inlineToken
                    bestLabel = matchResult.value.trim()
                    bestLineId = line.id
                    bestScore = baseScore
                    continue
                }

                // Look horizontally to the right on the same visual row
                val rightNeighbor = findRightNeighbor(line, sortedLines)
                if (rightNeighbor != null) {
                    val neighborToken = extractOrderTokenFromValue(rightNeighbor.text, allowShortCode = true)
                    if (neighborToken != null) {
                        bestOrder = neighborToken
                        bestLabel = matchResult.value.trim()
                        bestLineId = rightNeighbor.id
                        bestScore = baseScore
                        continue
                    }
                }

                // Look directly below the label
                val belowNeighbor = findBelowNeighbor(line, sortedLines)
                if (belowNeighbor != null) {
                    val belowToken = extractOrderTokenFromValue(belowNeighbor.text, allowShortCode = true)
                    if (belowToken != null) {
                        bestOrder = belowToken
                        bestLabel = matchResult.value.trim()
                        bestLineId = belowNeighbor.id
                        bestScore = baseScore
                    }
                }
            }
        }

        // Collect all plausible candidates from the entire screenshot
        val candidates = linkedSetOf<String>()
        if (bestOrder != null) {
            candidates.add(bestOrder)
        }

        val candidateLineIds = mutableSetOf<String>()
        for (line in sortedLines) {
            val found = findAllCandidatesInText(line.text)
            if (found.isNotEmpty()) {
                candidates.addAll(found)
                candidateLineIds.add(line.id)
            }
        }

        val confidence: MatchConfidence
        if (bestOrder != null) {
            confidence = MatchConfidence.EXACT_LABEL
        } else if (candidates.isNotEmpty()) {
            bestOrder = candidates.first()
            confidence = MatchConfidence.CANDIDATE_PATTERN
            bestLineId = sortedLines.firstOrNull { line ->
                findAllCandidatesInText(line.text).contains(bestOrder)
            }?.id
        } else {
            confidence = MatchConfidence.NOT_FOUND
        }

        val annotatedLines = sortedLines.map { line ->
            line.copy(
                isOrderCandidate = candidateLineIds.contains(line.id),
                isPrimaryMatch = line.id == bestLineId
            )
        }

        return OrderExtractionResult(
            bestOrderNumber = bestOrder,
            matchedLabel = bestLabel,
            confidence = confidence,
            allCandidates = candidates.toList(),
            detectedLines = annotatedLines,
            fullRawText = sortedLines.joinToString("\n") { it.text.trim() },
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    }

    private fun findRightNeighbor(anchor: OcrLine, allLines: List<OcrLine>): OcrLine? {
        val maxRowDelta = max(anchor.rect.height, 24) * 0.9f
        return allLines
            .filter { candidate ->
                candidate.id != anchor.id &&
                    candidate.rect.centerX > anchor.rect.centerX &&
                    candidate.rect.left >= anchor.rect.left &&
                    abs(candidate.rect.centerY - anchor.rect.centerY) <= maxRowDelta &&
                    !isPureFieldLabel(candidate.text)
            }
            .minByOrNull { it.rect.left }
    }

    private fun findBelowNeighbor(anchor: OcrLine, allLines: List<OcrLine>): OcrLine? {
        val maxVerticalGap = max(anchor.rect.height, 24) * 2.8f
        return allLines
            .filter { candidate ->
                val verticalGap = candidate.rect.top - anchor.rect.bottom
                candidate.id != anchor.id &&
                    candidate.rect.top >= anchor.rect.top &&
                    verticalGap in -10..maxVerticalGap.toInt() &&
                    abs(candidate.rect.centerX - anchor.rect.centerX) <= max(anchor.rect.width, candidate.rect.width) * 1.4f &&
                    !isPureFieldLabel(candidate.text)
            }
            .minByOrNull { it.rect.top }
    }

    private fun extractOrderTokenFromValue(raw: String, allowShortCode: Boolean): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || isNoiseToken(trimmed)) return null

        // First check hashtag tokens like #F-123 or #1234
        hashtagCodeRegex.find(trimmed)?.let { match ->
            val clean = match.value.trim()
            if (clean.any { it.isDigit() }) return clean
        }

        // Check alphanumeric tokens (e.g., 2411188X9K2M1P, SF-2411181234, 17829304129384)
        val tokens = orderTokenRegex.findAll(trimmed).map { it.value }.toList()
        val best = tokens.firstOrNull { token ->
            val digitCount = token.count { it.isDigit() }
            !isNoiseToken(token) &&
                !phoneRegex.matches(token) &&
                (digitCount >= 3 || (allowShortCode && digitCount >= 1 && token.length >= 2))
        }
        if (best != null) return best

        // If user has short numeric code right next to label (e.g., "42" or "1234")
        if (allowShortCode) {
            val shortCode = Regex("""\b[A-Za-z0-9\-]{2,24}\b""").find(trimmed)?.value
            if (shortCode != null && shortCode.any { it.isDigit() } && !isNoiseToken(shortCode)) {
                return shortCode
            }
        }
        return null
    }

    private fun findAllCandidatesInText(text: String): List<String> {
        val result = mutableListOf<String>()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return result

        hashtagCodeRegex.findAll(trimmed).forEach { match ->
            val token = match.value
            if (token.any { it.isDigit() } && !isNoiseToken(token)) {
                result.add(token)
            }
        }

        orderTokenRegex.findAll(trimmed).forEach { match ->
            val token = match.value
            if (isStrongOrderCandidate(token)) {
                result.add(token)
            }
        }
        return result.distinct()
    }

    private fun isStrongOrderCandidate(token: String): Boolean {
        if (isNoiseToken(token)) return false
        if (phoneRegex.matches(token)) return false

        val digits = token.count { it.isDigit() }
        val letters = token.count { it.isLetter() }
        val hasUppercase = token.any { it.isUpperCase() }

        // Typical ShopeeFood order IDs:
        // 1) Long numeric IDs (10-22 digits) e.g. 1839201938472910
        // 2) Mixed alphanumeric uppercase codes (8-24 chars, >= 4 digits, >= 1 uppercase letter) e.g. 2411188X9K2M1P
        // 3) Prefixed codes like SF-123456 or SHP-12345
        return when {
            digits >= 10 && letters == 0 -> true
            token.length in 8..24 && digits >= 4 && hasUppercase -> true
            token.contains('-') && digits >= 4 && letters >= 1 -> true
            else -> false
        }
    }

    private fun isPureFieldLabel(text: String): Boolean {
        val lower = text.trim().lowercase()
        val labelKeywords = listOf(
            "waktu", "tanggal", "metode", "pembayaran", "alamat",
            "catatan", "subtotal", "total", "ongkir", "diskon",
            "rincian", "restoran", "driver", "tunai", "shopeepay"
        )
        return labelKeywords.any { lower == it || lower.startsWith("$it:") } ||
            recipientWordRegex.matches(lower)
    }

    private fun isNoiseToken(token: String): Boolean {
        return timeRegex.matches(token) ||
            dateRegex.matches(token) ||
            currencyRegex.matches(token) ||
            distanceRegex.matches(token) ||
            token.equals("ShopeeFood", ignoreCase = true) ||
            token.equals("Shopee", ignoreCase = true) ||
            token.equals("Driver", ignoreCase = true) ||
            token.equals("Penerima", ignoreCase = true) ||
            token.equals("Pesanan", ignoreCase = true)
    }
}
