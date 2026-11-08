package com.zerowasteeats.app.presentation.model

import com.zerowasteeats.app.data.catalog.CatalogBusiness
import com.zerowasteeats.app.data.catalog.CatalogOffer
import com.zerowasteeats.app.data.catalog.TestClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferUiModelTest {

    // 15:00 UTC on 8 November 2026; the test clock's zone is UTC
    private val clock = TestClock(Instant.parse("2026-11-08T15:00:00Z"))

    private fun window(start: String, end: String) =
        pickupWindowOf(Instant.parse(start), Instant.parse(end), clock)

    @Test
    fun pickupWindow_namesTheDayRelativeToToday() {
        assertEquals(PickupDay.Today, window("2026-11-08T18:00:00Z", "2026-11-08T19:30:00Z").day)
        assertEquals(PickupDay.Tomorrow, window("2026-11-09T08:00:00Z", "2026-11-09T10:00:00Z").day)
        assertEquals(PickupDay.Yesterday, window("2026-11-07T08:00:00Z", "2026-11-07T10:00:00Z").day)
        assertEquals(
            PickupDay.On(LocalDate.of(2026, 11, 12)),
            window("2026-11-12T08:00:00Z", "2026-11-12T10:00:00Z").day
        )
    }

    @Test
    fun pickupWindow_keepsTheTimesInTheClocksZone() {
        val result = window("2026-11-08T18:00:00Z", "2026-11-08T19:30:00Z")

        assertEquals(LocalTime.of(18, 0), result.start)
        assertEquals(LocalTime.of(19, 30), result.end)
    }

    @Test
    fun address_addsTheCityOnce() {
        assertEquals("Cra. 27 #18-45, Pasto", addressOf("Cra. 27 #18-45", "Pasto"))
        assertEquals("Calle 18, Pasto, Nariño", addressOf("Calle 18, Pasto, Nariño", "Pasto"))
        assertEquals("Pasto", addressOf("  ", "Pasto"))
        assertEquals("Cra. 27", addressOf("Cra. 27", ""))
    }

    @Test
    fun toUiModel_carriesWhatTheScreensNeed() {
        val offer = CatalogOffer(
            id = "a",
            title = "Pan",
            description = " ",
            category = OfferCategory.BAKERY,
            photo = null,
            originalPrice = 12000,
            discountedPrice = 6000,
            discountPercent = 50,
            quantityAvailable = 3,
            pickupStart = Instant.parse("2026-11-08T18:00:00Z"),
            pickupEnd = Instant.parse("2026-11-08T19:00:00Z"),
            distanceKm = 0.8,
            isFavorite = true,
            business = CatalogBusiness("Panadería Sol", true, "Cra. 27", "Pasto", 1.215, -77.28)
        )

        val model = offer.toUiModel(clock)

        assertEquals("Panadería Sol", model.merchantName)
        assertEquals(6000L, model.discountedPrice)
        assertEquals(0.8, model.distanceKm!!, 0.0)
        assertEquals(3, model.remainingQuantity)
        assertEquals(PickupDay.Today, model.pickupWindow.day)
        assertEquals("Cra. 27, Pasto", model.address)
        assertEquals(1.215, model.latitude, 0.0)
        assertEquals(-77.28, model.longitude, 0.0)
        // A blank description is no description
        assertNull(model.description)
    }
}
