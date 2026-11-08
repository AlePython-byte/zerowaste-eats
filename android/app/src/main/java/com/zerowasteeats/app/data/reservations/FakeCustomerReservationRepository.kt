package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.catalog.CatalogOffer
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Reservations for demo mode, kept in memory: nothing leaves the device. A reservation is made
 * from what the app knows of the offer through [offerLookup], with a code like the server's,
 * and the impact is worked out from the picked-up ones the way the server does it. Every call
 * waits [latencyMillis] to look like a real request.
 */
class FakeCustomerReservationRepository(
    initialReservations: List<CustomerReservation> = emptyList(),
    private val offerLookup: suspend (offerId: String) -> CatalogOffer?,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS,
    private val random: Random = Random.Default
) : CustomerReservationRepository {

    private val state = MutableStateFlow(initialReservations)

    override val reservations: StateFlow<List<CustomerReservation>> = state

    /** Memory is always there. */
    override val loadState: StateFlow<CatalogLoadState> = MutableStateFlow(CatalogLoadState.Loaded)

    private val impactValue = MutableStateFlow<CustomerImpact?>(null)
    private val impactLoad = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Idle)

    override val impact: StateFlow<CustomerImpact?> = impactValue
    override val impactState: StateFlow<CatalogLoadState> = impactLoad

    override suspend fun refresh() = Unit

    override suspend fun refreshImpact() {
        impactLoad.value = CatalogLoadState.Loading
        delay(latencyMillis)
        impactValue.value = CustomerImpact.of(state.value)
        impactLoad.value = CatalogLoadState.Loaded
    }

    override suspend fun reserve(offerId: String, quantity: Int): ReserveResult {
        delay(latencyMillis)
        val offer = offerLookup(offerId)?.takeIf { it.isAvailable } ?: return ReserveResult.NotAvailable
        if (quantity !in 1..MAX_UNITS_PER_RESERVATION || quantity > offer.quantityAvailable) {
            return ReserveResult.NotEnoughUnits
        }
        val reservation = CustomerReservation(
            id = UUID.randomUUID().toString(),
            code = pickupCode(random),
            status = CustomerReservationStatus.PENDING,
            quantity = quantity,
            unitPrice = offer.discountedPrice,
            totalPrice = offer.discountedPrice * quantity,
            originalUnitPrice = offer.originalPrice,
            createdAt = clock.instant(),
            offerId = offer.id,
            offerTitle = offer.title,
            businessName = offer.business.name,
            pickupStart = offer.pickupStart,
            pickupEnd = offer.pickupEnd
        )
        state.update { listOf(reservation) + it }
        return ReserveResult.Reserved(reservation)
    }

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 600L

        /** The server's alphabet: no 0/O and no 1/I, so codes read aloud without doubt. */
        private const val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val CODE_LENGTH = 4

        /** A code such as ZW-7KQ3. */
        internal fun pickupCode(random: Random): String =
            "ZW-" + (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
                .joinToString("")

        /** [daysAgo] days before today at [hour]:[minute] in the clock's time zone. */
        private fun at(clock: Clock, daysAgo: Long, hour: Int, minute: Int): Instant =
            clock.instant().atZone(clock.zone).toLocalDate().minusDays(daysAgo)
                .atTime(LocalTime.of(hour, minute))
                .atZone(clock.zone)
                .toInstant()

        /** What the demo customer has reserved before: two to pick up, one picked up, one cancelled. */
        fun demoReservations(clock: Clock): List<CustomerReservation> = listOf(
            CustomerReservation(
                id = "demo-order-1",
                code = "ZW-A82J",
                status = CustomerReservationStatus.READY_FOR_PICKUP,
                quantity = 2,
                unitPrice = 8_900,
                totalPrice = 17_800,
                originalUnitPrice = 15_000,
                createdAt = at(clock, 0, 9, 10),
                offerId = "demo-catalog-order-1",
                offerTitle = "Croissants del día",
                businessName = "Panadería Artesanal",
                pickupStart = at(clock, 0, 18, 0),
                pickupEnd = at(clock, 0, 19, 30)
            ),
            CustomerReservation(
                id = "demo-order-2",
                code = "ZW-99XQ",
                status = CustomerReservationStatus.PENDING,
                quantity = 1,
                unitPrice = 19_000,
                totalPrice = 19_000,
                originalUnitPrice = 32_000,
                createdAt = at(clock, 0, 8, 40),
                offerId = "demo-catalog-order-2",
                offerTitle = "Bowl saludable de salmón",
                businessName = "Green Vida",
                pickupStart = at(clock, 0, 20, 0),
                pickupEnd = at(clock, 0, 21, 0)
            ),
            CustomerReservation(
                id = "demo-order-3",
                code = "ZW-74PQ",
                status = CustomerReservationStatus.COMPLETED,
                quantity = 1,
                unitPrice = 9_900,
                totalPrice = 9_900,
                originalUnitPrice = 20_000,
                createdAt = at(clock, 1, 8, 0),
                offerId = "demo-catalog-order-3",
                offerTitle = "Caja de postres surtidos",
                businessName = "Dulce Rincón",
                pickupStart = at(clock, 1, 10, 0),
                pickupEnd = at(clock, 1, 12, 0)
            ),
            CustomerReservation(
                id = "demo-order-4",
                code = "ZW-11BZ",
                status = CustomerReservationStatus.CANCELLED,
                quantity = 3,
                unitPrice = 8_000,
                totalPrice = 24_000,
                originalUnitPrice = 16_000,
                createdAt = at(clock, 6, 18, 0),
                offerId = "demo-catalog-order-4",
                offerTitle = "Pizza artesanal personal",
                businessName = "La Trattoria",
                pickupStart = at(clock, 6, 21, 0),
                pickupEnd = at(clock, 6, 22, 0)
            )
        )
    }
}
