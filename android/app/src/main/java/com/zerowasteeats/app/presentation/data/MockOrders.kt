package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.model.OrderUiModel

object MockOrders {
    fun getList(): List<OrderUiModel> = listOf(
        OrderUiModel(
            id = "o1",
            offerTitle = "Croissants del día",
            merchantName = "Panadería Artesanal",
            quantity = 2,
            totalPrice = "$17.800",
            pickupTime = "Hoy, 6:00 p. m. - 7:30 p. m.",
            reservationCode = "ZW-A82J",
            status = OrderStatus.READY_FOR_PICKUP
        ),
        OrderUiModel(
            id = "o2",
            offerTitle = "Bowl saludable de salmón",
            merchantName = "Green Vida",
            quantity = 1,
            totalPrice = "$19.000",
            pickupTime = "Hoy, 8:00 p. m. - 9:00 p. m.",
            reservationCode = "ZW-99XQ",
            status = OrderStatus.RESERVED
        ),
        OrderUiModel(
            id = "o3",
            offerTitle = "Caja de postres surtidos",
            merchantName = "Dulce Rincón",
            quantity = 1,
            totalPrice = "$9.900",
            pickupTime = "Ayer, 10:00 a. m. - 12:00 p. m.",
            reservationCode = "ZW-74PQ",
            status = OrderStatus.COMPLETED
        ),
        OrderUiModel(
            id = "o4",
            offerTitle = "Pizza artesanal personal",
            merchantName = "La Trattoria",
            quantity = 3,
            totalPrice = "$24.000",
            pickupTime = "12 Oct, 9:00 p. m. - 10:00 p. m.",
            reservationCode = "ZW-11BZ",
            status = OrderStatus.CANCELLED
        )
    )
}
