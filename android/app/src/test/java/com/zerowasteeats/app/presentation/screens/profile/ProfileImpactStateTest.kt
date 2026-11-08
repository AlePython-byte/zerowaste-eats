package com.zerowasteeats.app.presentation.screens.profile

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.reservations.CustomerImpact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileImpactStateTest {

    private val impact = CustomerImpact(completedOrders = 2, moneySaved = 12_000, foodRescuedKg = 1.2)

    @Test
    fun beforeTheFiguresArrive_showsPlaceholders() {
        assertTrue(ProfileImpactState(null, CatalogLoadState.Idle).showSkeleton)
        assertTrue(ProfileImpactState(null, CatalogLoadState.Loading).showSkeleton)
    }

    @Test
    fun reloading_keepsTheFigures() {
        val state = ProfileImpactState(impact, CatalogLoadState.Loading)

        assertFalse(state.showSkeleton)
        assertFalse(state.showError)
    }

    @Test
    fun aFailureWithNoFigures_saysSo() {
        val state = ProfileImpactState(null, CatalogLoadState.Failed(CatalogError.NETWORK))

        assertTrue(state.showError)
        assertEquals(CatalogError.NETWORK, state.loadError)
    }

    @Test
    fun aFailureAfterLoading_keepsTheFigures() {
        val state = ProfileImpactState(impact, CatalogLoadState.Failed(CatalogError.NETWORK))

        assertFalse(state.showError)
        assertEquals(impact, state.impact)
    }
}
