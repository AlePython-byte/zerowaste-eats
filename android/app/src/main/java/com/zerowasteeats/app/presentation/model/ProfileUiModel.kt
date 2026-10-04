package com.zerowasteeats.app.presentation.model

data class ProfileUiModel(
    val displayName: String,
    val roleLabel: String,
    val city: String,
    val initials: String,
    val completedOrders: Int,
    val rescuedFoodKg: Double,
    val estimatedSavings: String,
    val favoriteCategories: List<String>
)
