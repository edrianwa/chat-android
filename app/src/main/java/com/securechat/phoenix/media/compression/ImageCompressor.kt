package com.securechat.phoenix.media.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Compresses images for efficient upload.
 * Resizes large images and converts to WebP format.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1920
    private const val THUMBNAIL_DIMENSION = 100
    private const val QUALITY_FULL = 80
    private const val QUALITY_THUMBNAIL = 30
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB target

    /**
     * Compress an image from Uri to WebP bytes.
     * Resizes if larger than MAX_DIMENSION and compresses to target size.
     */
    fun compressImage(context: Context, uri: Uri): CompressedImage? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBytes = inputStream.readBytes()
            inputStream.close()

            compressImageBytes(originalBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compress raw image bytes.
     */
    fun compressImageBytes(originalBytes: ByteArray): CompressedImage? {
        return try {
            // Decode bounds first
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // Calculate sample size for memory efficiency
            val sampleSize = calculateSampleSize(origWidth, origHeight, MAX_DIMENSION)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, decodeOptions)
                ?: return null

            // Scale down if still too large
            val scaledBitmap = scaleBitmap(bitmap, MAX_DIMENSION)

            // Compress to WebP
            val fullBytes = compressBitmapToWebP(scaledBitmap, QUALITY_FULL)

            // Generate thumbnail
            val thumbnailBitmap = scaleBitmap(scaledBitmap, THUMBNAIL_DIMENSION)
            val thumbnailBytes = compressBitmapToWebP(thumbnailBitmap, QUALITY_THUMBNAIL)

            if (scaledBitmap !== bitmap) bitmap.recycle()
            if (thumbnailBitmap !== scaledBitmap) scaledBitmap.recycle()
            thumbnailBitmap.recycle()

            CompressedImage(
                fullBytes = fullBytes,
                thumbnailBytes = thumbnailBytes,
                width = scaledBitmap.width,
                height = scaledBitmap.height,
                mimeType = "image/webp"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a blurred thumbnail from image bytes.
     */
    fun generateThumbnail(imageBytes: ByteArray): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                ?: return null
            val thumb = scaleBitmap(bitmap, THUMBNAIL_DIMENSION)
            val bytes = compressBitmapToWebP(thumb, QUALITY_THUMBNAIL)
            bitmap.recycle()
            if (thumb !== bitmap) thumb.recycle()
            bytes
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        val largerDim = max(width, height)
        while (largerDim / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    @Suppress("DEPRECATION")
    private fun compressBitmapToWebP(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
        return outputStream.toByteArray()
    }
}

data class CompressedImage(
    val fullBytes: ByteArray,
    val thumbnailBytes: ByteArray,
    val width: Int,
    val height: Int,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompressedImage) return false
        return fullBytes.contentEquals(other.fullBytes) && thumbnailBytes.contentEquals(other.thumbnailBytes)
    }

    override fun hashCode(): Int = fullBytes.contentHashCode()
}
