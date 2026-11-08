package com.zerowasteeats.app.data.photos

import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.merchant.PickedPhoto

/** Why a photo could not be published. */
enum class PhotoUploadError {
    NETWORK,

    /** The storage refused it (expired session, a rule it breaks). */
    REJECTED,
    UNEXPECTED
}

sealed interface PhotoUpload {
    data class Uploaded(val photo: OfferPhoto.Remote) : PhotoUpload
    data class Failed(val error: PhotoUploadError) : PhotoUpload
}

/** Publishes a picked photo so an offer can point at it. */
fun interface OfferPhotoUploader {
    suspend fun upload(photo: PickedPhoto): PhotoUpload
}

/**
 * For demo mode: nothing is sent anywhere, the offer keeps pointing at the file in the app's
 * cache. Demo offers live in memory, so the file only has to outlive the process.
 */
class LocalOfferPhotoUploader : OfferPhotoUploader {
    override suspend fun upload(photo: PickedPhoto): PhotoUpload =
        PhotoUpload.Uploaded(OfferPhoto.Remote(photo.fileUrl))
}
