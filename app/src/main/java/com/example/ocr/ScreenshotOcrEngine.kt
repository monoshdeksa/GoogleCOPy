package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

data class ScreenshotScanOutput(
    val bitmap: Bitmap,
    val extraction: OrderExtractionResult
)

class ScreenshotOcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scanUri(uri: Uri): ScreenshotScanOutput = withContext(Dispatchers.IO) {
        val bitmap = decodeScaledBitmap(uri, maxDimension = 2048)
            ?: throw IllegalStateException("Gagal membaca gambar dari URI")

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizeText(inputImage)

        val lines = mutableListOf<OcrLine>()
        var lineIndex = 0
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                lines.add(
                    OcrLine(
                        id = "line_$lineIndex",
                        text = line.text,
                        rect = TextRect(
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom
                        )
                    )
                )
                lineIndex++
            }
        }

        val extraction = OrderTextExtractor.extract(
            rawLines = lines,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )

        ScreenshotScanOutput(
            bitmap = bitmap,
            extraction = extraction
        )
    }

    private suspend fun recognizeText(image: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }

    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var inSampleSize = 1
        val largestSide = max(srcWidth, srcHeight)
        while (largestSide / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
            readExifRotation(stream)
        } ?: 0f

        if (rotationDegrees == 0f) return decoded

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) {
            decoded.recycle()
        }
        return rotated
    }

    private fun readExifRotation(inputStream: InputStream): Float {
        return try {
            val exif = ExifInterface(inputStream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
    }
}
