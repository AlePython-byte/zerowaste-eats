package com.zerowasteeats.app.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.zerowasteeats.app.data.merchant.OfferPhoto
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What came of preparing a photo the merchant picked. */
sealed interface PhotoPreparation {
    data class Ready(val photo: OfferPhoto.Device, val sizeBytes: Int) : PhotoPreparation

    /** Not an image this device can read, or it could not be opened. */
    data object Unreadable : PhotoPreparation
}

/** Turns a picked photo into an offer photo. An interface so the editor can be tested. */
fun interface OfferPhotoPreparer {
    suspend fun prepare(uri: Uri): PhotoPreparation
}

/**
 * Prepares a picked photo for an offer: decoded with sampling (the full-resolution picture
 * never sits in memory), turned upright by its EXIF orientation, scaled to [PhotoScaling.MAX_SIDE]
 * on the longest side and saved as JPEG under [PhotoScaling.MAX_BYTES], in the app's cache.
 *
 * Android 9 and later decode with ImageDecoder, which already applies the orientation; Android
 * 8 uses BitmapFactory and reads the orientation with ExifInterface.
 */
class OfferPhotoProcessor(
    private val context: Context,
    private val outputDir: File = File(context.cacheDir, OUTPUT_DIR)
) : OfferPhotoPreparer {

    override suspend fun prepare(uri: Uri): PhotoPreparation = withContext(Dispatchers.IO) {
        try {
            val upright = decodeUpright(uri) ?: return@withContext PhotoPreparation.Unreadable
            val encoded = try {
                encode(upright)
            } finally {
                upright.recycle()
            }
                ?: return@withContext PhotoPreparation.Unreadable
            outputDir.mkdirs()
            val file = File(outputDir, "${UUID.randomUUID()}.jpg")
            file.writeBytes(encoded.bytes)
            PhotoPreparation.Ready(OfferPhoto.Device(file.absolutePath), encoded.bytes.size)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            PhotoPreparation.Unreadable
        } catch (_: SecurityException) {
            PhotoPreparation.Unreadable
        } catch (_: IllegalArgumentException) {
            PhotoPreparation.Unreadable
        } catch (_: OutOfMemoryError) {
            PhotoPreparation.Unreadable
        }
    }

    /** The picture upright and no larger than [PhotoScaling.MAX_SIDE] on its longest side. */
    private fun decodeUpright(uri: Uri): Bitmap? {
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri)
        } else {
            decodeWithBitmapFactory(uri)
        } ?: return null
        return scaleTo(decoded, PhotoScaling.targetSize(decoded.width, decoded.height))
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Compressing needs pixels in memory, not in a hardware buffer
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(
                PhotoScaling.sampleSize(info.size.width, info.size.height)
            )
        }
    }

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val orientation = resolver.openInputStream(uri)?.use { stream ->
            PhotoScaling.orientationFromExif(
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            )
        } ?: PhotoScaling.Orientation.UPRIGHT

        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoScaling.sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val sampled = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        if (orientation == PhotoScaling.Orientation.UPRIGHT) return sampled

        val matrix = Matrix().apply {
            postRotate(orientation.rotationDegrees.toFloat())
            if (orientation.mirrored) postScale(-1f, 1f)
        }
        val turned = Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
        if (turned !== sampled) sampled.recycle()
        return turned
    }

    private fun scaleTo(bitmap: Bitmap, size: PhotoScaling.Size): Bitmap {
        if (bitmap.width == size.width && bitmap.height == size.height) return bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, size.width, size.height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun encode(bitmap: Bitmap): PhotoScaling.Encoded? =
        PhotoScaling.encodeWithinLimit(PhotoScaling.Size(bitmap.width, bitmap.height)) { size, quality ->
            val source = if (size.width == bitmap.width && size.height == bitmap.height) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, size.width, size.height, true)
            }
            try {
                ByteArrayOutputStream().use { out ->
                    source.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
            } finally {
                if (source !== bitmap) source.recycle()
            }
        }

    companion object {
        /** Under the app's cache: offers live in memory, so their photos may be cleared with it. */
        const val OUTPUT_DIR = "offer_photos"
    }
}
