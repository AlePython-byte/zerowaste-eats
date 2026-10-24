package com.zerowasteeats.app.presentation.model

import androidx.annotation.StringRes
import com.zerowasteeats.app.R

enum class OrderStatus(@StringRes val labelRes: Int) {
    RESERVED(R.string.order_status_reserved),
    READY_FOR_PICKUP(R.string.order_status_ready_for_pickup),
    COMPLETED(R.string.order_status_completed),
    CANCELLED(R.string.order_status_cancelled)
}

data class OrderUiModel(
    val id: String,
    val offerTitle: String,
    val merchantName: String,
    val quantity: Int,
    val totalPrice: String,
    val pickupWindow: PickupWindow,
    val reservationCode: String,
    val status: OrderStatus
)
