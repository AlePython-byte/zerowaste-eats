package com.zerowasteeats.app.presentation.model

enum class OrderStatus(val label: String) {
    RESERVED("Reservado"),
    READY_FOR_PICKUP("Listo para recoger"),
    COMPLETED("Recogido"),
    CANCELLED("Cancelado")
}

data class OrderUiModel(
    val id: String,
    val offerTitle: String,
    val merchantName: String,
    val quantity: Int,
    val totalPrice: String,
    val pickupTime: String,
    val reservationCode: String,
    val status: OrderStatus
)
