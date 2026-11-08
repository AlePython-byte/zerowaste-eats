package com.zerowasteeats.app.data.reservations

import com.zerowasteeats.app.data.catalog.CatalogError
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.POST

/** The backend's reservations of the signed-in customer. The access token travels in the client. */
interface ReservationsApi {
    @POST("reservations")
    suspend fun reserve(@Body body: ReserveBody): CustomerReservationResponse
}

@Serializable
data class ReserveBody(val offerId: String, val quantity: Int)

@Serializable
data class CustomerReservationResponse(
    val id: String,
    val code: String,
    /** PENDING, CONFIRMED, READY_FOR_PICKUP, COMPLETED or CANCELLED. */
    val status: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val createdAt: String,
    val offer: ReservedOfferResponse,
    val business: ReservedBusinessResponse
)

@Serializable
data class ReservedOfferResponse(
    val id: String,
    val title: String,
    val originalPrice: Double,
    val pickupStart: String,
    val pickupEnd: String
)

@Serializable
data class ReservedBusinessResponse(val name: String)

/**
 * The customer's reservations on the backend. A new one is added to [reservations] with the
 * server's answer, so the orders show it without asking again.
 */
class RemoteCustomerReservationRepository(
    private val api: ReservationsApi
) : CustomerReservationRepository {

    private val state = MutableStateFlow<List<CustomerReservation>>(emptyList())

    override val reservations: StateFlow<List<CustomerReservation>> = state

    override suspend fun reserve(offerId: String, quantity: Int): ReserveResult = try {
        val reservation = api.reserve(ReserveBody(offerId, quantity)).toReservation()
        if (reservation == null) {
            ReserveResult.Failed(CatalogError.UNEXPECTED)
        } else {
            state.update { listOf(reservation) + it.filterNot { old -> old.id == reservation.id } }
            ReserveResult.Reserved(reservation)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            // Units ran out, or fewer are left than were asked for
            409 -> ReserveResult.NotEnoughUnits
            404 -> ReserveResult.NotAvailable
            else -> ReserveResult.Failed(customerErrorOf(e))
        }
    } catch (e: Exception) {
        ReserveResult.Failed(customerErrorOf(e))
    }

    companion object {
        /** Null for a reservation the app cannot read. */
        internal fun CustomerReservationResponse.toReservation(): CustomerReservation? = try {
            CustomerReservation(
                id = id,
                code = code,
                status = statusOf(status),
                quantity = quantity,
                unitPrice = unitPrice.roundToLong(),
                totalPrice = totalPrice.roundToLong(),
                originalUnitPrice = offer.originalPrice.roundToLong(),
                createdAt = Instant.parse(createdAt),
                offerId = offer.id,
                offerTitle = offer.title,
                businessName = business.name,
                pickupStart = Instant.parse(offer.pickupStart),
                pickupEnd = Instant.parse(offer.pickupEnd)
            )
        } catch (_: DateTimeParseException) {
            null
        }

        /** A confirmed reservation is still waiting for its pickup, like a pending one. */
        private fun statusOf(name: String): CustomerReservationStatus = when (name) {
            "READY_FOR_PICKUP" -> CustomerReservationStatus.READY_FOR_PICKUP
            "COMPLETED" -> CustomerReservationStatus.COMPLETED
            "CANCELLED" -> CustomerReservationStatus.CANCELLED
            else -> CustomerReservationStatus.PENDING
        }
    }
}
