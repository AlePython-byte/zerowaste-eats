package com.zerowasteeats.app.presentation.model

data class OfferUiModel(
    val id: String,
    val title: String,
    val merchantName: String,
    val imageUrl: String,
    val originalPrice: String,
    val discountedPrice: String,
    val discountPercentage: String,
    val distance: String,
    val remainingQuantityText: String,
    val pickupTime: String,
    val isFavorite: Boolean
)
