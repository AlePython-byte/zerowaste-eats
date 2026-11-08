package com.zerowasteeats.app.data.merchant

import androidx.annotation.DrawableRes
import com.zerowasteeats.app.R

/**
 * What the offer editor holds as the photo: one an offer already has ([OfferPhoto]), or one
 * just picked from the device ([PickedPhoto]) that still has to be published.
 */
sealed interface PhotoSelection {
    companion object {
        private const val BUNDLED_PREFIX = "bundled:"
        private const val REMOTE_PREFIX = "remote:"
        private const val PICKED_PREFIX = "picked:"

        /** A text form for saved state; [decode] reads it back. */
        fun encode(photo: PhotoSelection): String = when (photo) {
            is OfferPhoto.Bundled -> BUNDLED_PREFIX + photo.name
            is OfferPhoto.Remote -> REMOTE_PREFIX + photo.url
            is PickedPhoto -> PICKED_PREFIX + photo.path
        }

        /** Null for anything [encode] did not write. */
        fun decode(text: String): PhotoSelection? = when {
            text.startsWith(BUNDLED_PREFIX) ->
                OfferPhoto.Bundled.entries.firstOrNull { it.name == text.removePrefix(BUNDLED_PREFIX) }
            text.startsWith(REMOTE_PREFIX) ->
                text.removePrefix(REMOTE_PREFIX).takeIf { it.isNotEmpty() }?.let(OfferPhoto::Remote)
            text.startsWith(PICKED_PREFIX) ->
                text.removePrefix(PICKED_PREFIX).takeIf { it.isNotEmpty() }?.let(::PickedPhoto)
            else -> null
        }
    }
}

/** The photo of a published offer. */
sealed interface OfferPhoto : PhotoSelection {

    /**
     * A photo at [url]: in Supabase Storage for a live offer, or a `file://` URL in the app's
     * cache for a demo offer, which never leaves the device.
     */
    data class Remote(val url: String) : OfferPhoto

    /** One of the app's own photos, for the demo offers. */
    enum class Bundled(@param:DrawableRes val drawableRes: Int) : OfferPhoto {
        CROISSANT(R.drawable.offer_croissant),
        SALMON_BOWL(R.drawable.offer_bowl_salmon),
        DESSERTS(R.drawable.offer_postres),
        BURGER(R.drawable.offer_burger),
        VEGETABLES(R.drawable.offer_verduras)
    }
}

/**
 * A photo picked from the device, already upright and compressed to JPEG (see
 * [com.zerowasteeats.app.data.photos.OfferPhotoProcessor]). [path] is a file in the app's
 * cache. It only lives in the editor: publishing turns it into an [OfferPhoto.Remote].
 */
data class PickedPhoto(val path: String) : PhotoSelection {
    /** The same file as a URL, for a photo that is kept on the device (demo mode). */
    val fileUrl: String get() = "file://$path"
}
