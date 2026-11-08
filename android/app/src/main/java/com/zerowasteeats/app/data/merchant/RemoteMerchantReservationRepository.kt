package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.remote.parseApiError
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** The backend's reservations on the signed-in business's offers. */
interface MerchantReservationsApi {
    /** Pending ones first, the oldest at the top; then the rest, newest first. */
    @GET("merchant/reservations")
    suspend fun list(): List<BusinessReservationResponse>

    @POST("merchant/reservations/{id}/redeem")
    suspend fun redeem(@Path("id") id: String, @Body body: RedeemBody): BusinessReservationResponse
}

@Serializable
data class RedeemBody(val code: String)

/** What the business sees of a reservation: never the pickup code. */
@Serializable
data class BusinessReservationResponse(
    val id: String,
    /** PENDING, CONFIRMED, READY_FOR_PICKUP, COMPLETED or CANCELLED. */
    val status: String,
    val quantity: Int,
    val customer: BusinessReservationCustomer,
    val offer: BusinessReservationOffer
)

@Serializable
data class BusinessReservationCustomer(val displayName: String)

@Serializable
data class BusinessReservationOffer(
    val id: String,
    val title: String,
    val pickupStart: String,
    val pickupEnd: String
)

/**
 * The business's reservations from the backend. A pickup is confirmed by sending the code the
 * customer shows; the server checks it and counts wrong ones, so its answer decides: how many
 * tries are left, and for how long the reservation is locked. Cancelled reservations are not
 * listed: there is nothing left to do with them.
 */
class RemoteMerchantReservationRepository(
    private val api: MerchantReservationsApi
) : MerchantReservationRepository {

    private val state = MutableStateFlow<List<MerchantReservation>>(emptyList())
    private val load = MutableStateFlow<LoadState>(LoadState.Idle)

    override val reservations: StateFlow<List<MerchantReservation>> = state
    override val loadState: StateFlow<LoadState> = load

    override suspend fun refresh() {
        load.value = LoadState.Loading
        load.value = try {
            state.value = api.list().mapNotNull { it.toReservation() }
            LoadState.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadState.Failed(errorOf(e))
        }
    }

    override suspend fun redeem(reservationId: String, code: String): RedeemResult = try {
        val redeemed = api.redeem(reservationId, RedeemBody(code.trim())).toReservation()
        if (redeemed == null) {
            refreshQuietly()
            RedeemResult.Failed(MerchantDataError.UNEXPECTED)
        } else {
            state.update { list -> list.map { if (it.id == redeemed.id) redeemed else it } }
            RedeemResult.Success(redeemed)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            400 -> attemptsLeftIn(messageOf(e))?.let(RedeemResult::WrongCode) ?: RedeemResult.InvalidCode
            409 -> {
                // Handed over already, or cancelled: show what the server holds now
                refreshQuietly()
                RedeemResult.AlreadyRedeemed
            }
            429 -> RedeemResult.Locked(retryAfterMillisIn(messageOf(e)))
            404 -> {
                refreshQuietly()
                RedeemResult.Failed(MerchantDataError.UNEXPECTED)
            }
            else -> RedeemResult.Failed(statusError(e.code()))
        }
    } catch (e: Exception) {
        RedeemResult.Failed(errorOf(e))
    }

    /** Loads again; a failure here changes nothing, the result already says what happened. */
    private suspend fun refreshQuietly() {
        try {
            state.value = api.list().mapNotNull { it.toReservation() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep what is on screen
        }
    }

    private fun errorOf(e: Exception): MerchantDataError = when (e) {
        is IOException -> MerchantDataError.NETWORK
        is HttpException -> statusError(e.code())
        else -> MerchantDataError.UNEXPECTED
    }

    private fun statusError(code: Int): MerchantDataError = when {
        code == 401 -> MerchantDataError.SESSION
        code == 429 || code >= 500 -> MerchantDataError.NETWORK
        else -> MerchantDataError.UNEXPECTED
    }

    companion object {
        /** When the server does not say how long the lock lasts, try again after a minute. */
        private const val DEFAULT_LOCK_MILLIS = 60_000L
        private const val MINUTE_MILLIS = 60_000L

        private fun messageOf(e: HttpException): String? =
            e.response()?.errorBody()?.string()?.let(::parseApiError)?.message

        /** "… Te quedan 3 intentos." or "… Te queda 1 intento." → 3 or 1. */
        internal fun attemptsLeftIn(message: String?): Int? =
            message?.let { Regex("""(\d+)\s+intento""").find(it) }?.groupValues?.get(1)?.toIntOrNull()

        /** "… Intenta de nuevo en 7 minutos." → 7 minutes in milliseconds. */
        internal fun retryAfterMillisIn(message: String?): Long =
            message?.let { Regex("""(\d+)\s+minuto""").find(it) }?.groupValues?.get(1)?.toLongOrNull()
                ?.let { it * MINUTE_MILLIS }
                ?: DEFAULT_LOCK_MILLIS

        /** Null for a cancelled reservation, which is not listed, or one the app cannot read. */
        internal fun BusinessReservationResponse.toReservation(): MerchantReservation? {
            val appStatus = when (status) {
                "COMPLETED" -> MerchantReservationStatus.COMPLETED
                "CANCELLED" -> return null
                // Confirmed or ready: still waiting for the customer to come
                else -> MerchantReservationStatus.PENDING
            }
            return try {
                MerchantReservation(
                    id = id,
                    offerTitle = offer.title,
                    customerName = customer.displayName,
                    quantity = quantity,
                    pickupTime = Instant.parse(offer.pickupStart),
                    code = "",
                    status = appStatus
                )
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
