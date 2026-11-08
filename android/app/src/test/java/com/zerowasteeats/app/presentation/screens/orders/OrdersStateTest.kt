package com.zerowasteeats.app.presentation.screens.orders

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.catalog.TestClock
import com.zerowasteeats.app.data.reservations.FakeCustomerReservationRepository
import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.model.toOrderUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersStateTest {

    private val clock = TestClock()
    private val orders = FakeCustomerReservationRepository.demoReservations(clock)
        .map { it.toOrderUiModel(clock) }

    @Test
    fun beforeTheFirstLoad_showsPlaceholders() {
        assertTrue(OrdersState(emptyList(), CatalogLoadState.Idle).showSkeleton)
        assertTrue(OrdersState(emptyList(), CatalogLoadState.Loading).showSkeleton)
    }

    @Test
    fun reloading_keepsTheOrdersOnScreen() {
        val state = OrdersState(orders, CatalogLoadState.Loading)

        assertFalse(state.showSkeleton)
        assertFalse(state.showError)
    }

    @Test
    fun aFailureWithNothingLoaded_takesTheScreen() {
        val state = OrdersState(emptyList(), CatalogLoadState.Failed(CatalogError.NETWORK))

        assertTrue(state.showError)
        assertFalse(state.showSkeleton)
        assertEquals(CatalogError.NETWORK, state.loadError)
    }

    @Test
    fun aFailureWithOrdersLoaded_keepsThemAndOnlySaysWhy() {
        val state = OrdersState(orders, CatalogLoadState.Failed(CatalogError.SESSION))

        assertFalse(state.showError)
        assertEquals(CatalogError.SESSION, state.loadError)
    }

    @Test
    fun loadedWithNoOrders_isTheEmptyState_notAnError() {
        val state = OrdersState(emptyList(), CatalogLoadState.Loaded)

        assertFalse(state.showSkeleton)
        assertFalse(state.showError)
        assertTrue(state.ordersIn(OrdersTab.ACTIVE).isEmpty())
    }

    @Test
    fun theTabs_splitWhatIsToPickUp_fromWhatIsOver() {
        val state = OrdersState(orders, CatalogLoadState.Loaded)

        assertEquals(
            setOf(OrderStatus.RESERVED, OrderStatus.READY_FOR_PICKUP),
            state.ordersIn(OrdersTab.ACTIVE).map { it.status }.toSet()
        )
        assertEquals(
            setOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
            state.ordersIn(OrdersTab.HISTORY).map { it.status }.toSet()
        )
    }

    @Test
    fun anOrder_carriesItsCodeAndPrice() {
        val order = orders.first()

        assertEquals("ZW-A82J", order.reservationCode)
        assertEquals(OrderStatus.READY_FOR_PICKUP, order.status)
        assertTrue(order.totalPrice, order.totalPrice.contains("17"))
    }
}
