package com.zerowasteeats.app.presentation.data

import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferUiModel

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
            pickupTime = "Recoge hoy, 6:00 p. m. - 7:30 p. m.",
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
            pickupTime = "Recoge hoy, 8:00 p. m. - 9:00 p. m.",
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
            pickupTime = "Recoge mañana, 10:00 a. m. - 12:00 p. m.",
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
            pickupTime = "Recoge hoy, 7:00 p. m. - 8:30 p. m.",
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
            pickupTime = "Recoge mañana, 8:00 a. m. - 10:00 a. m.",
            isFavorite = false,
            category = OfferCategory.PRODUCE
        )
    )
}