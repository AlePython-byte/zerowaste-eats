package com.zerowasteeats.app.data.photos

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.merchant.PickedPhoto
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Publishes a picked photo to Supabase Storage, in the signed-in business's own folder:
 * `offer-photos/<uid>/<random uuid>.jpg`, the only place the bucket policies let it write and
 * the only kind of URL the backend accepts as an offer's photo.
 *
 * [client] adds the user's access token to the call (and renews it on a 401); the publishable
 * key goes in `apikey`. Storage does not always answer an expired token with a 401, so
 * [beforeUpload] gets a chance to renew a token about to expire first.
 */
class SupabaseOfferPhotoUploader(
    private val client: OkHttpClient,
    supabaseUrl: String,
    private val publishableKey: String,
    private val userId: String,
    private val beforeUpload: suspend () -> Unit = {},
    private val newFileName: () -> String = { UUID.randomUUID().toString() }
) : OfferPhotoUploader {

    private val baseUrl = supabaseUrl.trimEnd('/')

    override suspend fun upload(photo: PickedPhoto): PhotoUpload {
        val file = File(photo.path)
        if (!file.isFile) return PhotoUpload.Failed(PhotoUploadError.UNEXPECTED)
        return try {
            beforeUpload()
            val objectPath = "$BUCKET/$userId/${newFileName()}.jpg"
            val request = Request.Builder()
                .url("$baseUrl/storage/v1/object/$objectPath")
                .put(file.asRequestBody(JPEG))
                .header("apikey", publishableKey)
                .build()
            // Blocking, but interruptible: leaving the screen cancels the upload
            val code = runInterruptible(Dispatchers.IO) {
                client.newCall(request).execute().use { it.code }
            }
            when (code) {
                in 200..299 -> PhotoUpload.Uploaded(
                    OfferPhoto.Remote("$baseUrl/storage/v1/object/public/$objectPath")
                )
                429, in 500..599 -> PhotoUpload.Failed(PhotoUploadError.NETWORK)
                in 400..499 -> PhotoUpload.Failed(PhotoUploadError.REJECTED)
                else -> PhotoUpload.Failed(PhotoUploadError.UNEXPECTED)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            PhotoUpload.Failed(PhotoUploadError.NETWORK)
        } catch (_: IllegalArgumentException) {
            PhotoUpload.Failed(PhotoUploadError.UNEXPECTED)
        }
    }

    companion object {
        /** The bucket created by docs/supabase/storage-offer-photos.sql. */
        const val BUCKET = "offer-photos"
        private val JPEG = "image/jpeg".toMediaType()
    }
}
