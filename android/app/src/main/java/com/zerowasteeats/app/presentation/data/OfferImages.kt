package com.zerowasteeats.app.presentation.data

import androidx.annotation.DrawableRes
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.model.OfferUiModel

// Temporary bridge: local photos for the mock offers.
// When offers come from the API, replace this with an image loader (Coil)
// using offer.imageUrl, and keep a placeholder for missing images.
@DrawableRes
fun offerPhotoRes(offer: OfferUiModel): Int? = when (offer.id) {
    "1" -> R.drawable.offer_croissant
    "2" -> R.drawable.offer_bowl_salmon
    "3" -> R.drawable.offer_postres
    "4" -> R.drawable.offer_burger
    "5" -> R.drawable.offer_verduras
    else -> null
}
