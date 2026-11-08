package com.zerowasteeats.app.presentation.screens.orders

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.reservations.CustomerReservationRepository
import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.model.OrderUiModel
import com.zerowasteeats.app.presentation.model.toOrderUiModel
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OrdersTab(@StringRes val labelRes: Int) {
    ACTIVE(R.string.orders_tab_active),
    HISTORY(R.string.orders_tab_history)
}

/** The orders and how loading them went: placeholders, an error with a retry, or the list. */
data class OrdersState(
    val orders: List<OrderUiModel> = emptyList(),
    val loadState: CatalogLoadState = CatalogLoadState.Idle
) {
    val loadError: CatalogError? = (loadState as? CatalogLoadState.Failed)?.error

    /** Nothing to show yet and the first load is on its way. */
    val showSkeleton: Boolean = orders.isEmpty() &&
        (loadState == CatalogLoadState.Idle || loadState == CatalogLoadState.Loading)

    /** Nothing to show because the load failed: the error takes the whole screen. */
    val showError: Boolean = orders.isEmpty() && loadError != null

    /** To pick up first: the soonest pickup on top. Past ones: the latest on top. */
    fun ordersIn(tab: OrdersTab): List<OrderUiModel> = when (tab) {
        OrdersTab.ACTIVE -> orders.filter {
            it.status == OrderStatus.RESERVED || it.status == OrderStatus.READY_FOR_PICKUP
        }
        OrdersTab.HISTORY -> orders.filter {
            it.status == OrderStatus.COMPLETED || it.status == OrderStatus.CANCELLED
        }
    }
}

/** Follows the customer's reservations, and loads them again each time the orders show. */
class OrdersViewModel(
    private val repository: CustomerReservationRepository,
    private val clock: Clock
) : ViewModel() {

    val state: StateFlow<OrdersState> = combine(
        repository.reservations,
        repository.loadState
    ) { reservations, loadState ->
        OrdersState(reservations.map { it.toOrderUiModel(clock) }, loadState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OrdersState(
            repository.reservations.value.map { it.toOrderUiModel(clock) },
            repository.loadState.value
        )
    )

    /** A business may have handed one over since: showing the orders is the moment to catch up. */
    fun refresh() {
        if (repository.loadState.value == CatalogLoadState.Loading) return
        viewModelScope.launch { repository.refresh() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

class OrdersViewModelFactory(
    private val repository: CustomerReservationRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OrdersViewModel::class.java) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OrdersViewModel(repository, clock) as T
    }
}
