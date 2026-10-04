package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.ProfileUiModel

object MockProfile {
    fun getProfile(): ProfileUiModel = ProfileUiModel(
        displayName = "Alejandro",
        roleLabel = "Cliente",
        city = "Pasto, Nariño",
        initials = "AP",
        completedOrders = 7,
        rescuedFoodKg = 4.2,
        estimatedSavings = "$86.400",
        favoriteCategories = listOf(
            "Panadería",
            "Comida preparada",
            "Postres"
        )
    )
}
