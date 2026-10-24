package com.zerowasteeats.app.data.merchant

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The reservations customers made at this business. */
interface MerchantReservationRepository {
    val reservations: StateFlow<List<MerchantReservation>>

    /**
     * Hands over the reservation with this [code], ignoring case, spaces and dashes. A code
     * works once: the second time it is [RedeemResult.AlreadyRedeemed].
     */
    suspend fun redeem(code: String): RedeemResult

    /**
     * Hands over the reservation [reservationId] if [code] is that reservation's own code,
     * compared the same way as [redeem]. A valid code of another reservation is
     * [RedeemResult.InvalidCode] here, and so is an unknown id.
     */
    suspend fun redeem(reservationId: String, code: String): RedeemResult
}

/** In-memory reservations. Every call waits [latencyMillis] to look like a real request. */
class FakeMerchantReservationRepository(
    initialReservations: List<MerchantReservation> = emptyList(),
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS
) : MerchantReservationRepository {

    private val state = MutableStateFlow(initialReservations)

    override val reservations: StateFlow<List<MerchantReservation>> = state

    override suspend fun redeem(code: String): RedeemResult =
        redeemMatching(code) { reservation, wanted -> normalize(reservation.code) == wanted }

    override suspend fun redeem(reservationId: String, code: String): RedeemResult =
        redeemMatching(code) { reservation, wanted ->
            reservation.id == reservationId && normalize(reservation.code) == wanted
        }

    /** Hands over the first reservation that [matches] the typed code, normalized. */
    private suspend fun redeemMatching(
        code: String,
        matches: (MerchantReservation, String) -> Boolean
    ): RedeemResult {
        delay(latencyMillis)
        val wanted = normalize(code)
        var result: RedeemResult = RedeemResult.InvalidCode
        // Look up and complete in one atomic step, so two redeems of one code cannot both win
        state.update { list ->
            val match = if (wanted.isEmpty()) {
                null
            } else {
                list.firstOrNull { matches(it, wanted) }
            }
            when {
                match == null -> {
                    result = RedeemResult.InvalidCode
                    list
                }
                match.status == MerchantReservationStatus.COMPLETED -> {
                    result = RedeemResult.AlreadyRedeemed
                    list
                }
                else -> {
                    val completed = match.copy(status = MerchantReservationStatus.COMPLETED)
                    result = RedeemResult.Success(completed)
                    list.map { if (it.id == match.id) completed else it }
                }
            }
        }
        return result
    }

    private fun normalize(code: String) = code.filter { it.isLetterOrDigit() }.uppercase()

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 300L
    }
}
