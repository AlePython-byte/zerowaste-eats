package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogError
import java.io.IOException
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

/**
 * A customer's reservations: the backend in live mode ([RemoteCustomerReservationRepository]),
 * memory in demo mode ([FakeCustomerReservationRepository]). One instance serves every screen
 * of a signed-in customer, so a reservation made on the detail shows at once in the orders.
 */
interface CustomerReservationRepository {
    /** Newest first. */
    val reservations: StateFlow<List<CustomerReservation>>

    /** Reserves [quantity] units of offer [offerId]; on success it heads [reservations]. */
    suspend fun reserve(offerId: String, quantity: Int): ReserveResult
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
