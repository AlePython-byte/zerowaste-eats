package com.zerowasteeats.app.data.merchant

import androidx.annotation.DrawableRes
import com.zerowasteeats.app.R

/**
 * The photos a merchant can pick for an offer, until offers can upload their own. Each one is
 * an image already in the app.
 */
enum class OfferPhoto(@param:DrawableRes val drawableRes: Int) {
    CROISSANT(R.drawable.offer_croissant),
    SALMON_BOWL(R.drawable.offer_bowl_salmon),
    DESSERTS(R.drawable.offer_postres),
    BURGER(R.drawable.offer_burger),
    VEGETABLES(R.drawable.offer_verduras)
}
