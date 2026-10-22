package com.zerowasteeats.app.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OfferFiltersTest {

    private fun offer(
        id: String,
        title: String = "Oferta $id",
        merchant: String = "Comercio $id",
        price: String = "$10.000",
        discount: Int = 40,
        distance: String = "1.0 km",
        category: OfferCategory = OfferCategory.BAKERY
    ) = OfferUiModel(
        id = id,
        title = title,
        merchantName = merchant,
        imageUrl = "",
        originalPrice = "$20.000",
        discountedPrice = price,
        discountPercent = discount,
        distance = distance,
        remainingQuantity = 1,
        pickupTime = "",
        isFavorite = false,
        category = category
    )

    private fun List<OfferUiModel>.ids() = map { it.id }

    @Test
    fun search_ignoresCaseAndAccents() {
        val offers = listOf(
            offer("1", merchant = "Panadería Artesanal"),
            offer("2", title = "Bowl de salmón", merchant = "Green Vida")
        )

        assertEquals(listOf("1"), offers.filterAndSort("panaderia", OfferFilters()).ids())
        assertEquals(listOf("1"), offers.filterAndSort("PANADERÍA", OfferFilters()).ids())
        assertEquals(listOf("2"), offers.filterAndSort("salmon", OfferFilters()).ids())
    }

    @Test
    fun search_matchesTitleOrMerchant() {
        val offers = listOf(
            offer("1", title = "Croissants", merchant = "Horno"),
            offer("2", title = "Pan", merchant = "Croissantería")
        )

        assertEquals(listOf("1", "2"), offers.filterAndSort("croissant", OfferFilters()).ids())
    }

    @Test
    fun blankQuery_keepsEverything() {
        val offers = listOf(offer("1"), offer("2"))

        assertEquals(listOf("1", "2"), offers.filterAndSort("", OfferFilters()).ids())
    }

    @Test
    fun categoryAll_doesNotFilter() {
        val offers = listOf(
            offer("1", category = OfferCategory.BAKERY),
            offer("2", category = OfferCategory.DESSERTS)
        )

        assertEquals(listOf("1", "2"), offers.filterAndSort("", OfferFilters(OfferCategory.ALL)).ids())
    }

    @Test
    fun category_keepsOnlyMatchingOffers() {
        val offers = listOf(
            offer("1", category = OfferCategory.BAKERY),
            offer("2", category = OfferCategory.DESSERTS)
        )

        assertEquals(listOf("2"), offers.filterAndSort("", OfferFilters(OfferCategory.DESSERTS)).ids())
    }

    @Test
    fun sortByDistance_isAscendingAndNumeric() {
        val offers = listOf(
            offer("1", distance = "10.5 km"),
            offer("2", distance = "0.5 km"),
            offer("3", distance = "2.5 km")
        )

        assertEquals(listOf("2", "3", "1"), offers.filterAndSort("", OfferFilters(sort = OfferSort.DISTANCE)).ids())
    }

    @Test
    fun sortByPrice_isAscendingAndIgnoresSeparators() {
        val offers = listOf(
            offer("1", price = "$19.000"),
            offer("2", price = "$8.900"),
            offer("3", price = "$9.900")
        )

        assertEquals(listOf("2", "3", "1"), offers.filterAndSort("", OfferFilters(sort = OfferSort.PRICE)).ids())
    }

    @Test
    fun sortByDiscount_isDescending() {
        val offers = listOf(
            offer("1", discount = 40),
            offer("2", discount = 50),
            offer("3", discount = 20)
        )

        assertEquals(listOf("2", "1", "3"), offers.filterAndSort("", OfferFilters(sort = OfferSort.DISCOUNT)).ids())
    }

    @Test
    fun ties_fallBackToIdRegardlessOfInputOrder() {
        val offers = listOf(
            offer("3", distance = "1.0 km", price = "$5.000", discount = 30),
            offer("1", distance = "1.0 km", price = "$5.000", discount = 30),
            offer("2", distance = "1.0 km", price = "$5.000", discount = 30)
        )

        OfferSort.entries.forEach { sort ->
            assertEquals(listOf("1", "2", "3"), offers.filterAndSort("", OfferFilters(sort = sort)).ids())
        }
    }

    @Test
    fun activeCount_countsNonDefaultCategoryAndSort() {
        assertEquals(0, OfferFilters().activeCount)
        assertEquals(1, OfferFilters(category = OfferCategory.BAKERY).activeCount)
        assertEquals(1, OfferFilters(sort = OfferSort.PRICE).activeCount)
        assertEquals(2, OfferFilters(OfferCategory.DESSERTS, OfferSort.DISCOUNT).activeCount)
    }
}
