package com.zerowasteeats.app.presentation.model

data class OfferUiModel(
    val id: String,
    val title: String,
    val merchantName: String,
    val imageUrl: String,
    val originalPrice: String,
    val discountedPrice: String,
    val discountPercent: Int,
    val distance: String,
    val remainingQuantity: Int,
    val pickupTime: String,
    val isFavorite: Boolean,
    val category: OfferCategory
)
