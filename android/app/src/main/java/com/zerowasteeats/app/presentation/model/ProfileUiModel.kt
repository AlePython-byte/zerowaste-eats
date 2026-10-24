package com.zerowasteeats.app.presentation.model

/** The customer's activity. Who they are comes from the session, not from here. */
data class ProfileUiModel(
    val completedOrders: Int,
    val rescuedFoodKg: Double,
    val estimatedSavings: String,
    val favoriteCategories: List<OfferCategory>
)
