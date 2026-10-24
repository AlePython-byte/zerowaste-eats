package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.model.PickupDay
import com.zerowasteeats.app.presentation.model.PickupWindow
import java.time.LocalTime

object MockOffers {
    fun getList(): List<OfferUiModel> = listOf(
        OfferUiModel(
            id = "1",
            title = "Croissants del día",
            merchantName = "Panadería Artesanal",
            imageUrl = "",
            originalPrice = "$15.000",
            discountedPrice = "$8.900",
            discountPercent = 40,
            distance = "0.5 km",
            remainingQuantity = 2,
            pickupWindow = PickupWindow(PickupDay.Today, LocalTime.of(18, 0), LocalTime.of(19, 30)),
            isFavorite = false,
            category = OfferCategory.BAKERY
        ),
        OfferUiModel(
            id = "2",
            title = "Bowl saludable de salmón",
            merchantName = "Green Vida",
            imageUrl = "",
            originalPrice = "$32.000",
            discountedPrice = "$19.000",
            discountPercent = 40,
            distance = "1.2 km",
            remainingQuantity = 1,
            pickupWindow = PickupWindow(PickupDay.Today, LocalTime.of(20, 0), LocalTime.of(21, 0)),
            isFavorite = true,
            category = OfferCategory.PREPARED_FOOD
        ),
        OfferUiModel(
            id = "3",
            title = "Caja de postres surtidos",
            merchantName = "Dulce Rincón",
            imageUrl = "",
            originalPrice = "$20.000",
            discountedPrice = "$9.900",
            discountPercent = 50,
            distance = "2.5 km",
            remainingQuantity = 3,
            pickupWindow = PickupWindow(PickupDay.Tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0)),
            isFavorite = false,
            category = OfferCategory.DESSERTS
        ),
        OfferUiModel(
            id = "4",
            title = "Hamburguesa artesanal con papas",
            merchantName = "Burger Taller",
            imageUrl = "",
            originalPrice = "$24.000",
            discountedPrice = "$14.400",
            discountPercent = 40,
            distance = "1.8 km",
            remainingQuantity = 1,
            pickupWindow = PickupWindow(PickupDay.Today, LocalTime.of(19, 0), LocalTime.of(20, 30)),
            isFavorite = false,
            category = OfferCategory.PREPARED_FOOD
        ),
        OfferUiModel(
            id = "5",
            title = "Canasta de verduras frescas",
            merchantName = "Mercado Verde",
            imageUrl = "",
            originalPrice = "$15.000",
            discountedPrice = "$7.500",
            discountPercent = 50,
            distance = "3.1 km",
            remainingQuantity = 4,
            pickupWindow = PickupWindow(PickupDay.Tomorrow, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            isFavorite = false,
            category = OfferCategory.PRODUCE
        )
    )
}