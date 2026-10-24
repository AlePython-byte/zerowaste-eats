package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.ProfileUiModel

object MockProfile {
    fun getProfile(): ProfileUiModel = ProfileUiModel(
        completedOrders = 7,
        rescuedFoodKg = 4.2,
        estimatedSavings = "$86.400",
        favoriteCategories = listOf(
            OfferCategory.BAKERY,
            OfferCategory.PREPARED_FOOD,
            OfferCategory.DESSERTS
        )
    )
}
