package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.model.OrderUiModel
import com.zerowasteeats.app.presentation.model.PickupDay
import com.zerowasteeats.app.presentation.model.PickupWindow
import java.time.LocalDate
import java.time.LocalTime

object MockOrders {
    fun getList(): List<OrderUiModel> = listOf(
        OrderUiModel(
            id = "o1",
            offerTitle = "Croissants del día",
            merchantName = "Panadería Artesanal",
            quantity = 2,
            totalPrice = "$17.800",
            pickupWindow = PickupWindow(PickupDay.Today, LocalTime.of(18, 0), LocalTime.of(19, 30)),
            reservationCode = "ZW-A82J",
            status = OrderStatus.READY_FOR_PICKUP
        ),
        OrderUiModel(
            id = "o2",
            offerTitle = "Bowl saludable de salmón",
            merchantName = "Green Vida",
            quantity = 1,
            totalPrice = "$19.000",
            pickupWindow = PickupWindow(PickupDay.Today, LocalTime.of(20, 0), LocalTime.of(21, 0)),
            reservationCode = "ZW-99XQ",
            status = OrderStatus.RESERVED
        ),
        OrderUiModel(
            id = "o3",
            offerTitle = "Caja de postres surtidos",
            merchantName = "Dulce Rincón",
            quantity = 1,
            totalPrice = "$9.900",
            pickupWindow = PickupWindow(PickupDay.Yesterday, LocalTime.of(10, 0), LocalTime.of(12, 0)),
            reservationCode = "ZW-74PQ",
            status = OrderStatus.COMPLETED
        ),
        OrderUiModel(
            id = "o4",
            offerTitle = "Pizza artesanal personal",
            merchantName = "La Trattoria",
            quantity = 3,
            totalPrice = "$24.000",
            pickupWindow = PickupWindow(PickupDay.On(LocalDate.of(2025, 10, 12)), LocalTime.of(21, 0), LocalTime.of(22, 0)),
            reservationCode = "ZW-11BZ",
            status = OrderStatus.CANCELLED
        )
    )
}
