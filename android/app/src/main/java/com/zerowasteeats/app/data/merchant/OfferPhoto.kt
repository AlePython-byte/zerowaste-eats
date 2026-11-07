package com.zerowasteeats.app.data.merchant

import androidx.annotation.DrawableRes
import com.zerowasteeats.app.R

/** The photo of an offer: one the merchant took or picked, or one of the app's own. */
sealed interface OfferPhoto {

    /** The app's photos, for an offer without a photo of its own. */
    enum class Stock(@param:DrawableRes val drawableRes: Int) : OfferPhoto {
        CROISSANT(R.drawable.offer_croissant),
        SALMON_BOWL(R.drawable.offer_bowl_salmon),
        DESSERTS(R.drawable.offer_postres),
        BURGER(R.drawable.offer_burger),
        VEGETABLES(R.drawable.offer_verduras)
    }

    /**
     * A photo from the device, already rotated upright and compressed to JPEG (see
     * [com.zerowasteeats.app.data.photos.OfferPhotoProcessor]). [path] is a file in the app's
     * cache: offers live in memory for now, so the file only has to outlive the process.
     */
    data class Device(val path: String) : OfferPhoto

    companion object {
        private const val STOCK_PREFIX = "stock:"
        private const val DEVICE_PREFIX = "device:"

        /** A text form for saved state; [decode] reads it back. */
        fun encode(photo: OfferPhoto): String = when (photo) {
            is Stock -> STOCK_PREFIX + photo.name
            is Device -> DEVICE_PREFIX + photo.path
        }

        /** Null for anything [encode] did not write. */
        fun decode(text: String): OfferPhoto? = when {
            text.startsWith(STOCK_PREFIX) ->
                Stock.entries.firstOrNull { it.name == text.removePrefix(STOCK_PREFIX) }
            text.startsWith(DEVICE_PREFIX) ->
                text.removePrefix(DEVICE_PREFIX).takeIf { it.isNotEmpty() }?.let(::Device)
            else -> null
        }
    }
}
