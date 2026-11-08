package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.FakeOfferCatalogRepository
import com.zerowasteeats.app.data.catalog.TestClock
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCustomerReservationRepositoryTest {

    private val clock = TestClock()
    private val offers = FakeOfferCatalogRepository.demoOffers(clock)

    private fun repository() = FakeCustomerReservationRepository(
        offerLookup = { id -> offers.firstOrNull { it.id == id } },
        clock = clock,
        latencyMillis = 0
    )

    @Test
    fun reserving_makesAPendingReservation_withACodeLikeTheServers() = runBlocking {
        val repository = repository()
        val offer = offers.first { it.quantityAvailable >= 2 }

        val result = repository.reserve(offer.id, 2)

        val reservation = (result as ReserveResult.Reserved).reservation
        assertTrue(reservation.code, Regex("ZW-[2-9A-HJ-NP-Z]{4}").matches(reservation.code))
        assertEquals(CustomerReservationStatus.PENDING, reservation.status)
        assertEquals(offer.discountedPrice * 2, reservation.totalPrice)
        assertEquals(offer.title, reservation.offerTitle)
        assertEquals(listOf(reservation), repository.reservations.value)
    }

    @Test
    fun moreThanWhatIsLeft_isNotEnoughUnits() = runBlocking {
        val offer = offers.first()

        val result = repository().reserve(offer.id, offer.quantityAvailable + 1)

        assertEquals(ReserveResult.NotEnoughUnits, result)
    }

    @Test
    fun anUnknownOffer_isNotAvailable() = runBlocking {
        assertEquals(ReserveResult.NotAvailable, repository().reserve("nope", 1))
    }

    @Test
    fun codes_neverUseCharactersThatReadAlike() {
        val random = Random(7)
        repeat(500) {
            val code = FakeCustomerReservationRepository.pickupCode(random)
            assertTrue(code, code.drop(3).none { it in "01OI" })
        }
    }
}
