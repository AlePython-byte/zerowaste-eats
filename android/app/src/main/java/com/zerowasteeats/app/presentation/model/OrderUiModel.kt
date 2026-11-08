package com.zerowasteeats.app.presentation.model

import androidx.annotation.StringRes
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.reservations.CustomerReservation
import com.zerowasteeats.app.data.reservations.CustomerReservationStatus
import com.zerowasteeats.app.presentation.format.formatPesos
import java.time.Clock

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

/** The reservation as an order card, with its pickup day relative to today on [clock]. */
fun CustomerReservation.toOrderUiModel(clock: Clock): OrderUiModel = OrderUiModel(
    id = id,
    offerTitle = offerTitle,
    merchantName = businessName,
    quantity = quantity,
    totalPrice = formatPesos(totalPrice),
    pickupWindow = pickupWindowOf(pickupStart, pickupEnd, clock),
    reservationCode = code,
    status = when (status) {
        CustomerReservationStatus.PENDING -> OrderStatus.RESERVED
        CustomerReservationStatus.READY_FOR_PICKUP -> OrderStatus.READY_FOR_PICKUP
        CustomerReservationStatus.COMPLETED -> OrderStatus.COMPLETED
        CustomerReservationStatus.CANCELLED -> OrderStatus.CANCELLED
    }
)
