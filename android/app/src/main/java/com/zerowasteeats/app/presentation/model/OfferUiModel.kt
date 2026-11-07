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
    val pickupWindow: PickupWindow,
    val isFavorite: Boolean,
    val category: OfferCategory,
    /** What the business wrote about the offer; null when it wrote nothing. */
    val description: String? = null
)
