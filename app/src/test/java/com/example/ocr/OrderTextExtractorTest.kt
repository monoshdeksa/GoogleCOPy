package com.example.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTextExtractorTest {

    @Test
    fun `extracts inline No Pesanan Penerima`() {
        val lines = listOf(
            OcrLine("1", "Rincian Pesanan", TextRect(40, 100, 400, 140)),
            OcrLine("2", "No. Pesanan Penerima: 2411188X9K2M1P", TextRect(40, 200, 900, 240)),
            OcrLine("3", "Total Rp45.000", TextRect(40, 300, 400, 340))
        )

        val result = OrderTextExtractor.extract(lines, 1080, 1920)
        assertEquals("2411188X9K2M1P", result.bestOrderNumber)
        assertEquals(MatchConfidence.EXACT_LABEL, result.confidence)
    }

    @Test
    fun `extracts two-column No Pesanan Penerima from right neighbor`() {
        val lines = listOf(
            OcrLine("1", "No. Pesanan Penerima", TextRect(40, 500, 460, 540)),
            OcrLine("2", "241118ABCD9876", TextRect(620, 498, 1020, 538)),
            OcrLine("3", "Metode Pembayaran", TextRect(40, 580, 420, 620)),
            OcrLine("4", "ShopeePay", TextRect(780, 580, 1020, 620))
        )

        val result = OrderTextExtractor.extract(lines, 1080, 1920)
        assertEquals("241118ABCD9876", result.bestOrderNumber)
        assertEquals(MatchConfidence.EXACT_LABEL, result.confidence)
    }

    @Test
    fun `extracts stacked No Pesanan Penerima from line directly below`() {
        val lines = listOf(
            OcrLine("1", "No Pesanan Penerima", TextRect(40, 300, 420, 336)),
            OcrLine("2", "#F-204", TextRect(40, 355, 210, 395)),
            OcrLine("3", "081234567890", TextRect(40, 460, 350, 500))
        )

        val result = OrderTextExtractor.extract(lines, 1080, 1920)
        assertEquals("#F-204", result.bestOrderNumber)
        assertEquals(MatchConfidence.EXACT_LABEL, result.confidence)
    }

    @Test
    fun `falls back to alphanumeric candidate when label is cropped out`() {
        val lines = listOf(
            OcrLine("1", "14:35", TextRect(40, 40, 150, 80)),
            OcrLine("2", "Rp32.500", TextRect(40, 140, 280, 180)),
            OcrLine("3", "081298765432", TextRect(40, 220, 380, 260)),
            OcrLine("4", "2411189Q7W6E5R", TextRect(40, 300, 520, 340))
        )

        val result = OrderTextExtractor.extract(lines, 1080, 1920)
        assertEquals("2411189Q7W6E5R", result.bestOrderNumber)
        assertEquals(MatchConfidence.CANDIDATE_PATTERN, result.confidence)
        assertTrue(result.allCandidates.contains("2411189Q7W6E5R"))
    }
}
