package com.zerowasteeats.app.presentation.screens.merchant.reservations

import com.zerowasteeats.app.data.merchant.LoadState
import com.zerowasteeats.app.data.merchant.MerchantDataError
import com.zerowasteeats.app.data.merchant.MerchantReservation
import com.zerowasteeats.app.data.merchant.MerchantReservationRepository
import com.zerowasteeats.app.data.merchant.MerchantReservationStatus
import com.zerowasteeats.app.data.merchant.RedeemResult

enum class ReservationTab(val status: MerchantReservationStatus) {
    PENDING(MerchantReservationStatus.PENDING),
    COMPLETED(MerchantReservationStatus.COMPLETED)
}

/** The reservations, the chosen tab, what the list shows from them, and how loading went. */
data class MerchantReservationListState(
    val reservations: List<MerchantReservation>,
    val tab: ReservationTab,
    val loadState: LoadState = LoadState.Loaded
) {
    val loadError: MerchantDataError? = (loadState as? LoadState.Failed)?.error

    /** Nothing to show yet and the first load is on its way. */
    val showSkeleton: Boolean = reservations.isEmpty() &&
        (loadState == LoadState.Idle || loadState == LoadState.Loading)

    /** Nothing to show because the load failed: the error takes the place of the list. */
    val showError: Boolean = reservations.isEmpty() && loadError != null

    val counts: Map<ReservationTab, Int> =
        ReservationTab.entries.associateWith { tab -> reservations.count { it.status == tab.status } }

    /** Pending: the soonest pickup first. Completed: the latest pickup first. */
    val visibleReservations: List<MerchantReservation> =
        reservations.filter { it.status == tab.status }.let { list ->
            when (tab) {
                ReservationTab.PENDING -> list.sortedBy { it.pickupTime }
                ReservationTab.COMPLETED -> list.sortedByDescending { it.pickupTime }
            }
        }
}

/**
 * Wrong pickup codes per reservation, counted on the device. After [maxFailures] wrong codes in
 * a row on one reservation it locks for [lockMillis]; the count starts over once the lock is
 * over, and a right code clears it. [nowMillis] is the clock. The server keeps its own count
 * and its answer wins; this one only applies when the server gives none (demo mode).
 */
class PickupCodeAttempts(
    val nowMillis: () -> Long,
    val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val lockMillis: Long = DEFAULT_LOCK_MILLIS
) {
    private val failures = mutableMapOf<String, Int>()
    private val lockedUntil = mutableMapOf<String, Long>()

    /** When the lock on [reservationId] ends, or null if it is not locked now. */
    fun lockedUntil(reservationId: String): Long? =
        lockedUntil[reservationId]?.takeIf { it > nowMillis() }

    /** Wrong codes left before the lock. */
    fun attemptsLeft(reservationId: String): Int = maxFailures - (failures[reservationId] ?: 0)

    /** Counts a wrong code. Returns when the lock ends if this one locked it, otherwise null. */
    fun recordFailure(reservationId: String): Long? {
        val count = (failures[reservationId] ?: 0) + 1
        return if (count >= maxFailures) {
            failures.remove(reservationId)
            (nowMillis() + lockMillis).also { lockedUntil[reservationId] = it }
        } else {
            failures[reservationId] = count
            null
        }
    }

    /** Locks [reservationId] until [untilMillis], as the server said; the local count starts over. */
    fun lockUntil(reservationId: String, untilMillis: Long) {
        failures.remove(reservationId)
        lockedUntil[reservationId] = untilMillis
    }

    fun recordSuccess(reservationId: String) {
        failures.remove(reservationId)
        lockedUntil.remove(reservationId)
    }

    companion object {
        const val DEFAULT_MAX_FAILURES = 5
        const val DEFAULT_LOCK_MILLIS = 30_000L
    }
}

sealed interface PickupConfirmResult {
    data class Confirmed(val reservation: MerchantReservation) : PickupConfirmResult
    data class WrongCode(val attemptsLeft: Int) : PickupConfirmResult
    data class Locked(val untilMillis: Long) : PickupConfirmResult
    data object AlreadyCompleted : PickupConfirmResult

    /** The code did not reach the server, or the server failed: nothing was counted. */
    data class Failed(val error: MerchantDataError) : PickupConfirmResult
}

/**
 * Checks the code the customer shows against one reservation. When the server says how many
 * tries are left, or how long the reservation is locked, that is what the person sees; when it
 * says nothing, wrong codes are counted in [attempts]. While locked, nothing reaches the
 * repository.
 */
class PickupConfirmation(
    private val repository: MerchantReservationRepository,
    private val attempts: PickupCodeAttempts
) {
    suspend fun confirm(reservationId: String, code: String): PickupConfirmResult {
        attempts.lockedUntil(reservationId)?.let { return PickupConfirmResult.Locked(it) }

        return when (val result = repository.redeem(reservationId, code)) {
            is RedeemResult.Success -> {
                attempts.recordSuccess(reservationId)
                PickupConfirmResult.Confirmed(result.reservation)
            }
            RedeemResult.AlreadyRedeemed -> PickupConfirmResult.AlreadyCompleted
            is RedeemResult.WrongCode -> PickupConfirmResult.WrongCode(result.attemptsLeft)
            is RedeemResult.Locked -> {
                val until = attempts.nowMillis() + result.retryAfterMillis
                attempts.lockUntil(reservationId, until)
                PickupConfirmResult.Locked(until)
            }
            is RedeemResult.Failed -> PickupConfirmResult.Failed(result.error)
            RedeemResult.InvalidCode -> {
                val lockedUntil = attempts.recordFailure(reservationId)
                if (lockedUntil != null) {
                    PickupConfirmResult.Locked(lockedUntil)
                } else {
                    PickupConfirmResult.WrongCode(attempts.attemptsLeft(reservationId))
                }
            }
        }
    }
}

/** Keeps what can be part of a pickup code, in capitals, at most [MAX_CODE_LENGTH] long. */
fun normalizePickupCodeInput(raw: String): String =
    raw.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(MAX_CODE_LENGTH)

/** "PAN-4821" is 8; a little room for codes typed with a space or dash in another place. */
const val MAX_CODE_LENGTH = 10
