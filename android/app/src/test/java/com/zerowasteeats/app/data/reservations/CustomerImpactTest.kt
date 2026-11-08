package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.catalog.TestClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerImpactTest {

    private val clock = TestClock()

    @Test
    fun onlyPickedUpReservations_count() {
        val impact = CustomerImpact.of(FakeCustomerReservationRepository.demoReservations(clock))

        // The demo has one picked-up order: one box of desserts, 20.000 down to 9.900
        assertEquals(1, impact.completedOrders)
        assertEquals(10_100L, impact.moneySaved)
        assertEquals(0.4, impact.foodRescuedKg, 0.0)
        assertTrue(impact.foodIsEstimate)
    }

    @Test
    fun theFood_isWorkedOutFromTheUnits_andRoundedToATenth() {
        val completed = FakeCustomerReservationRepository.demoReservations(clock).first()
            .copy(status = CustomerReservationStatus.COMPLETED, quantity = 7)

        val impact = CustomerImpact.of(listOf(completed))

        // 7 × 0.4 = 2.8 kg
        assertEquals(2.8, impact.foodRescuedKg, 0.0)
        assertEquals((completed.originalUnitPrice - completed.unitPrice) * 7, impact.moneySaved)
    }

    @Test
    fun withNothingPickedUp_everythingIsZero() {
        assertEquals(CustomerImpact(0, 0, 0.0), CustomerImpact.of(emptyList()))
    }

    @Test
    fun inDemoMode_theImpactLoads_fromTheLocalReservations() = runBlocking {
        val repository = FakeCustomerReservationRepository(
            initialReservations = FakeCustomerReservationRepository.demoReservations(clock),
            offerLookup = { null },
            clock = clock,
            latencyMillis = 0
        )

        repository.refreshImpact()

        assertEquals(1, repository.impact.value?.completedOrders)
        assertEquals(CatalogLoadState.Loaded, repository.impactState.value)
    }
}
