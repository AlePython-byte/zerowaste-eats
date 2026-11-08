package com.zerowasteeats.app.presentation.screens.offerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CustomerLocationSource
import com.zerowasteeats.app.data.catalog.OfferCatalogRepository
import com.zerowasteeats.app.data.catalog.OfferLookup
import com.zerowasteeats.app.data.reservations.CustomerReservation
import com.zerowasteeats.app.data.reservations.CustomerReservationRepository
import com.zerowasteeats.app.data.reservations.MAX_UNITS_PER_RESERVATION
import com.zerowasteeats.app.data.reservations.ReserveResult
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.model.toUiModel
import com.zerowasteeats.app.presentation.screens.feed.FavoriteProblem
import com.zerowasteeats.app.presentation.screens.feed.problemFor
import java.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where asking the server for the offer stands. */
sealed interface DetailStatus {
    data object Loading : DetailStatus
    data object Loaded : DetailStatus
    data object NotAvailable : DetailStatus
    data class Failed(val error: CatalogError) : DetailStatus
}

/** Reserving from the detail: how many units, and whether a reservation is on its way. */
data class ReserveState(
    /** As the person chose it; [OfferDetailState.quantity] keeps it within what is left. */
    val requestedQuantity: Int = 1,
    val isReserving: Boolean = false,
    /** The reservation just made: its code is shown until the person closes the sheet. */
    val reserved: CustomerReservation? = null
)

/** Why a reservation was not made, for the message on the detail. */
enum class ReserveProblem {
    /** Fewer units are left than were asked for: the offer was loaded again. */
    NOT_ENOUGH_UNITS,
    NETWORK,

    /** The session ended: only signing in again helps. */
    SESSION,
    UNEXPECTED
}

/**
 * The offer on the detail screen. What the list already knew shows at once, while the server
 * is asked for the latest; only with nothing known yet are there placeholders.
 */
data class OfferDetailState(
    val offer: OfferUiModel? = null,
    val status: DetailStatus = DetailStatus.Loading,
    val reserve: ReserveState = ReserveState()
) {
    val showSkeleton: Boolean = offer == null && status == DetailStatus.Loading

    /** The server says it can no longer be reserved. */
    val showNotAvailable: Boolean = status == DetailStatus.NotAvailable

    val loadError: CatalogError? = (status as? DetailStatus.Failed)?.error

    /** The most one reservation may take: what is left, and never more than the server allows. */
    val maxQuantity: Int = (offer?.remainingQuantity ?: 0).coerceIn(0, MAX_UNITS_PER_RESERVATION)

    /** The units that would be reserved now: the choice, within 1 and [maxQuantity]. */
    val quantity: Int = reserve.requestedQuantity.coerceIn(1, maxQuantity.coerceAtLeast(1))

    /** Units left and still on sale; the button waits while a reservation is on its way. */
    val canReserve: Boolean =
        offer != null && offer.isAvailable && maxQuantity > 0 && !reserve.isReserving

    val canDecrease: Boolean = canReserve && quantity > 1
    val canIncrease: Boolean = canReserve && quantity < maxQuantity

    /** What the chosen units cost together, in whole pesos. */
    val totalPrice: Long = (offer?.discountedPrice ?: 0L) * quantity
}

class OfferDetailViewModel(
    private val offerId: String,
    private val catalog: OfferCatalogRepository,
    private val reservations: CustomerReservationRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock
) : ViewModel() {

    private val status = MutableStateFlow<DetailStatus>(DetailStatus.Loading)
    private val reserve = MutableStateFlow(ReserveState())

    val state: StateFlow<OfferDetailState> = combine(
        catalog.observe(offerId),
        status,
        reserve
    ) { offer, status, reserve ->
        OfferDetailState(offer?.toUiModel(clock), status, reserve)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OfferDetailState()
    )

    private val favoriteProblems = Channel<FavoriteProblem>(Channel.BUFFERED)
    val favoriteFailures: Flow<FavoriteProblem> = favoriteProblems.receiveAsFlow()

    private val reserveProblems = Channel<ReserveProblem>(Channel.BUFFERED)
    val reserveFailures: Flow<ReserveProblem> = reserveProblems.receiveAsFlow()

    init {
        load()
    }

    fun retry() = load()

    fun increaseQuantity() = changeQuantity(+1)

    fun decreaseQuantity() = changeQuantity(-1)

    private fun changeQuantity(by: Int) {
        val current = state.value
        val next = (current.quantity + by).coerceIn(1, current.maxQuantity.coerceAtLeast(1))
        reserve.value = reserve.value.copy(requestedQuantity = next)
    }

    /** Reserves the chosen units. Ignored while a reservation is already on its way. */
    fun reserve() {
        val current = state.value
        if (!current.canReserve) return
        val quantity = current.quantity
        reserve.value = reserve.value.copy(isReserving = true)
        viewModelScope.launch {
            val result = reservations.reserve(offerId, quantity)
            reserve.value = reserve.value.copy(
                isReserving = false,
                reserved = (result as? ReserveResult.Reserved)?.reservation,
                // The next reservation starts again from one unit
                requestedQuantity = if (result is ReserveResult.Reserved) 1 else quantity
            )
            when (result) {
                // Fewer units are left now: show the new count
                is ReserveResult.Reserved -> refreshQuietly()
                ReserveResult.NotEnoughUnits -> {
                    reserveProblems.send(ReserveProblem.NOT_ENOUGH_UNITS)
                    refreshQuietly()
                }
                // The lookup finds it gone and shows the screen that says so
                ReserveResult.NotAvailable -> load()
                is ReserveResult.Failed -> reserveProblems.send(
                    when (result.error) {
                        CatalogError.NETWORK -> ReserveProblem.NETWORK
                        CatalogError.SESSION -> ReserveProblem.SESSION
                        CatalogError.UNEXPECTED -> ReserveProblem.UNEXPECTED
                    }
                )
            }
        }
    }

    /** The person closed the sheet with the code. */
    fun dismissReserved() {
        reserve.value = reserve.value.copy(reserved = null)
    }

    /** Asks for the offer again without placeholders; a failure keeps what is shown. */
    private suspend fun refreshQuietly() {
        if (catalog.lookUp(offerId, location.current()) == OfferLookup.NotAvailable) {
            status.value = DetailStatus.NotAvailable
        }
    }

    /** Saves the offer as a favorite, or removes it if it was one. */
    fun toggleFavorite() {
        viewModelScope.launch {
            val offer = catalog.observe(offerId).first() ?: return@launch
            changeFavorite(!offer.isFavorite)
        }
    }

    fun setFavorite(favorite: Boolean) {
        viewModelScope.launch { changeFavorite(favorite) }
    }

    private suspend fun changeFavorite(favorite: Boolean) {
        catalog.setFavorite(offerId, favorite).problemFor(offerId, favorite)
            ?.let { favoriteProblems.send(it) }
    }

    private fun load() {
        status.value = DetailStatus.Loading
        viewModelScope.launch {
            status.value = when (val result = catalog.lookUp(offerId, location.current())) {
                is OfferLookup.Found -> DetailStatus.Loaded
                OfferLookup.NotAvailable -> DetailStatus.NotAvailable
                is OfferLookup.Failed -> DetailStatus.Failed(result.error)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class OfferDetailViewModelFactory(
    private val offerId: String,
    private val catalog: OfferCatalogRepository,
    private val reservations: CustomerReservationRepository,
    private val location: CustomerLocationSource,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfferDetailViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OfferDetailViewModel(offerId, catalog, reservations, location, clock) as T
    }
}
