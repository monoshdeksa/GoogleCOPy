package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

class ImageLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OcrProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ScreenshotScanOutput(
    val bitmap: Bitmap,
    val extraction: OrderExtractionResult
)

class ScreenshotOcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scanUri(uri: Uri): ScreenshotScanOutput = withContext(Dispatchers.IO) {
        val bitmap = try {
            decodeScaledBitmap(uri, maxDimension = 2560)
                ?: throw ImageLoadException("Format gambar tidak didukung atau tidak dapat dibaca.")
        } catch (e: ImageLoadException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            throw ImageLoadException(
                e.localizedMessage ?: e::class.java.simpleName,
                e
            )
        }

        if (bitmap.width < 32 || bitmap.height < 32) {
            throw ImageLoadException("Ukuran gambar terlalu kecil (${bitmap.width}x${bitmap.height}px).")
        }

        val visionText = try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizeText(inputImage)
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit OCR failed for URI: $uri", e)
            throw OcrProcessingException(
                e.localizedMessage ?: e::class.java.simpleName,
                e
            )
        }

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

    fun close() {
        runCatching { recognizer.close() }
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
        // Strategy 1: ImageDecoder on API 28+ (opens URI once, handles EXIF & HEIF/WebP/PNG/JPEG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    // ML Kit InputImage.fromBitmap requires software-backed bitmap (not HARDWARE)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false

                    val srcWidth = info.size.width
                    val srcHeight = info.size.height
                    val largestSide = max(srcWidth, srcHeight)
                    if (largestSide > maxDimension && srcWidth > 0 && srcHeight > 0) {
                        val scale = maxDimension.toFloat() / largestSide.toFloat()
                        val targetWidth = (srcWidth * scale).roundToInt().coerceAtLeast(32)
                        val targetHeight = (srcHeight * scale).roundToInt().coerceAtLeast(32)
                        decoder.setTargetSize(targetWidth, targetHeight)
                    }
                }
                return ensureArgb8888(decoded)
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder failed for $uri, falling back to stream decode", e)
            }
        }

        // Strategy 2: Read URI bytes ONCE into memory, then decode bounds, bitmap, and EXIF safely
        val imageBytes = readUriBytesOnce(uri)
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val bitmap = decodeFromBytes(imageBytes, maxDimension)
            if (bitmap != null) {
                return ensureArgb8888(bitmap)
            }
        }

        // Strategy 3: File descriptor fallback (single read; avoids EOF on non-seekable descriptors)
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val decodeOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val raw = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, decodeOptions)
                    ?: return null
                val argb = ensureArgb8888(raw)
                scaleDownIfNeeded(argb, maxDimension)
            }
        } catch (e: Exception) {
            Log.e(TAG, "FileDescriptor decode fallback failed for $uri", e)
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(32)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(32)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun readUriBytesOnce(uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        try {
            resolver.openInputStream(uri)?.use { stream ->
                return stream.readBytes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "openInputStream failed for $uri, trying openAssetFileDescriptor", e)
        }

        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream().use { it.readBytes() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "openAssetFileDescriptor failed for $uri", e)
            null
        }
    }

    private fun decodeFromBytes(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(srcWidth, srcHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return null

        val rotationDegrees = ByteArrayInputStream(bytes).use { stream ->
            readExifRotation(stream)
        }

        if (rotationDegrees == 0f) return decoded

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) {
            decoded.recycle()
        }
        return rotated
    }

    private fun computeInSampleSize(srcWidth: Int, srcHeight: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val largestSide = max(srcWidth, srcHeight)
        while (largestSide / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun ensureArgb8888(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.ARGB_8888) return bitmap
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (copy != null && copy !== bitmap) {
            bitmap.recycle()
            return copy
        }
        return bitmap
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

    companion object {
        private const val TAG = "ScreenshotOcrEngine"
    }
}
