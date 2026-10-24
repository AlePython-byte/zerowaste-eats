package com.zerowasteeats.app.presentation.screens.merchant.reservations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.merchant.MerchantReservationRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PickupCodeError { WRONG, ALREADY_COMPLETED }

/** The pickup sheet while it is open. */
data class PickupSheetState(
    val reservationId: String,
    val code: String = "",
    val error: PickupCodeError? = null,
    /** Wrong codes left before the lock, shown with [PickupCodeError.WRONG]. */
    val attemptsLeft: Int = PickupCodeAttempts.DEFAULT_MAX_FAILURES,
    /** While set and in the future, the field is locked. */
    val lockedUntilMillis: Long? = null,
    val isChecking: Boolean = false,
    /** The code was right: the sheet plays its check and closes. */
    val isConfirmed: Boolean = false
)

/**
 * Holds the list, the open sheet and the wrong-code count. All of it lives here, not in the
 * screen, so a rotation keeps the sheet open with what was typed, and the count goes on.
 */
class MerchantReservationsViewModel(
    repository: MerchantReservationRepository,
    /** The clock for the lock; tests replace it. */
    val nowMillis: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val tab = MutableStateFlow(ReservationTab.PENDING)

    val state: StateFlow<MerchantReservationListState> = combine(
        repository.reservations,
        tab,
        ::MerchantReservationListState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MerchantReservationListState(repository.reservations.value, tab.value)
    )

    private val attempts = PickupCodeAttempts(nowMillis)
    private val confirmation = PickupConfirmation(repository, attempts)

    var sheet: PickupSheetState? by mutableStateOf(null)
        private set

    // Once per wrong code, for the short vibration; a channel so a rotation does not repeat it
    private val wrongCodes = Channel<Unit>(Channel.CONFLATED)
    val wrongCode: Flow<Unit> = wrongCodes.receiveAsFlow()

    fun selectTab(value: ReservationTab) {
        tab.value = value
    }

    fun openSheet(reservationId: String) {
        sheet = PickupSheetState(
            reservationId = reservationId,
            attemptsLeft = attempts.attemptsLeft(reservationId),
            lockedUntilMillis = attempts.lockedUntil(reservationId)
        )
    }

    /** Closes the sheet without confirming, or after its check played. */
    fun closeSheet() {
        sheet = null
    }

    fun onCodeChange(raw: String) {
        // A wrong code stays flagged until something else is typed
        sheet = sheet?.copy(code = normalizePickupCodeInput(raw), error = null)
    }

    fun submit() {
        val current = sheet ?: return
        if (current.isChecking || current.isConfirmed || current.code.isBlank()) return
        sheet = current.copy(isChecking = true)

        viewModelScope.launch {
            val result = confirmation.confirm(current.reservationId, current.code)
            // The sheet may have been closed while checking
            val open = sheet?.takeIf { it.reservationId == current.reservationId } ?: return@launch
            sheet = when (result) {
                is PickupConfirmResult.Confirmed -> open.copy(isChecking = false, isConfirmed = true)
                is PickupConfirmResult.WrongCode -> {
                    wrongCodes.trySend(Unit)
                    open.copy(isChecking = false, error = PickupCodeError.WRONG, attemptsLeft = result.attemptsLeft)
                }
                is PickupConfirmResult.Locked -> {
                    wrongCodes.trySend(Unit)
                    open.copy(isChecking = false, error = null, code = "", lockedUntilMillis = result.untilMillis)
                }
                PickupConfirmResult.AlreadyCompleted ->
                    open.copy(isChecking = false, error = PickupCodeError.ALREADY_COMPLETED)
            }
        }
    }

    /** The lock is over: the field opens again with the full count of attempts. */
    fun onLockExpired() {
        val current = sheet ?: return
        sheet = current.copy(
            lockedUntilMillis = attempts.lockedUntil(current.reservationId),
            attemptsLeft = attempts.attemptsLeft(current.reservationId)
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class MerchantReservationsViewModelFactory(
    private val repository: MerchantReservationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MerchantReservationsViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MerchantReservationsViewModel(repository) as T
    }
}
