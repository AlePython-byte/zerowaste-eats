package com.zerowasteeats.app.presentation.data

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
            discountPercentage = "40% menos",
            distance = "0.5 km",
            remainingQuantityText = "Quedan 2",
            pickupTime = "Recoge hoy, 6:00 p. m. - 7:30 p. m.",
            isFavorite = false,
            category = "Panadería"
        ),
        OfferUiModel(
            id = "2",
            title = "Bowl saludable de salmón",
            merchantName = "Green Vida",
            imageUrl = "",
            originalPrice = "$32.000",
            discountedPrice = "$19.000",
            discountPercentage = "40% menos",
            distance = "1.2 km",
            remainingQuantityText = "Queda 1",
            pickupTime = "Recoge hoy, 8:00 p. m. - 9:00 p. m.",
            isFavorite = true,
            category = "Comida preparada"
        ),
        OfferUiModel(
            id = "3",
            title = "Caja de postres surtidos",
            merchantName = "Dulce Rincón",
            imageUrl = "",
            originalPrice = "$20.000",
            discountedPrice = "$9.900",
            discountPercentage = "50% menos",
            distance = "2.5 km",
            remainingQuantityText = "Quedan 3",
            pickupTime = "Recoge mañana, 10:00 a. m. - 12:00 p. m.",
            isFavorite = false,
            category = "Postres"
        )
    )
}