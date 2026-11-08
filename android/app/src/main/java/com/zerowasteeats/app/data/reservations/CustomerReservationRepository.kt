package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import java.io.IOException
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

/**
 * A customer's reservations and what their pickups added up to: the backend in live mode
 * ([RemoteCustomerReservationRepository]), memory in demo mode
 * ([FakeCustomerReservationRepository]). One instance serves every screen of a signed-in
 * customer, so a reservation made on the detail shows at once in the orders.
 */
interface CustomerReservationRepository {
    /** Newest first, as the last load returned them, plus the ones made since. */
    val reservations: StateFlow<List<CustomerReservation>>

    /** Where loading [reservations] stands. */
    val loadState: StateFlow<CatalogLoadState>

    /** Loads the reservations again; on failure the ones already loaded stay. */
    suspend fun refresh()

    /** Reserves [quantity] units of offer [offerId]; on success it heads [reservations]. */
    suspend fun reserve(offerId: String, quantity: Int): ReserveResult

    /** The customer's impact as last loaded; null until it first arrives. */
    val impact: StateFlow<CustomerImpact?>

    /** Where loading [impact] stands. */
    val impactState: StateFlow<CatalogLoadState>

    /** Loads the impact again; on failure the figures already loaded stay. */
    suspend fun refreshImpact()
}

/**
 * What a customer's pickups added up to. Only picked-up reservations count. The food is an
 * estimate: offers carry no weight, so it is worked out from the units at a fixed average.
 */
data class CustomerImpact(
    val completedOrders: Int,
    /** Whole pesos: what the units would have cost without the discount, minus what was paid. */
    val moneySaved: Long,
    val foodRescuedKg: Double,
    /** Always true for now: [foodRescuedKg] is shown as approximate. */
    val foodIsEstimate: Boolean = true
) {
    companion object {
        /** The server's estimate for a bag or a portion. */
        const val ESTIMATED_KG_PER_UNIT = 0.4

        /** Worked out the way the server does it, for demo mode. */
        fun of(reservations: List<CustomerReservation>): CustomerImpact {
            val completed = reservations.filter { it.status == CustomerReservationStatus.COMPLETED }
            val units = completed.sumOf { it.quantity }
            return CustomerImpact(
                completedOrders = completed.size,
                moneySaved = completed.sumOf {
                    (it.originalUnitPrice - it.unitPrice).coerceAtLeast(0) * it.quantity
                },
                foodRescuedKg = Math.round(units * ESTIMATED_KG_PER_UNIT * 10) / 10.0
            )
        }
    }
}

/** Why a call to the backend failed, in the terms the customer screens already use. */
internal fun customerErrorOf(e: Exception): CatalogError = when (e) {
    is IOException -> CatalogError.NETWORK
    is HttpException -> when {
        e.code() == 401 -> CatalogError.SESSION
        e.code() == 429 || e.code() >= 500 -> CatalogError.NETWORK
        else -> CatalogError.UNEXPECTED
    }
    else -> CatalogError.UNEXPECTED
}
