package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OfferCategory

/** Sample preferences for the profile, until customers can choose their own. */
object MockProfile {
    val favoriteCategories: List<OfferCategory> = listOf(
        OfferCategory.BAKERY,
        OfferCategory.PREPARED_FOOD,
        OfferCategory.DESSERTS
    )
}
