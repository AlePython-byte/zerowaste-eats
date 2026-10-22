package com.zerowasteeats.app.presentation.screens.explore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExploreViewRequestTest {

    @Test
    fun noRequest_consumesNothing() {
        assertNull(ExploreViewRequest().consume())
    }

    @Test
    fun request_isConsumedOnlyOnce() {
        val request = ExploreViewRequest()
        request.request(ExploreViewMode.MAP)

        assertEquals(ExploreViewMode.MAP, request.consume())
        assertNull(request.consume())
        assertNull(request.pending)
    }

    @Test
    fun request_staysPendingUntilExploreConsumesIt() {
        val request = ExploreViewRequest()
        request.request(ExploreViewMode.MAP)

        // Nothing reads it while Explore does not exist yet
        assertEquals(ExploreViewMode.MAP, request.pending)
        assertEquals(ExploreViewMode.MAP, request.pending)

        assertEquals(ExploreViewMode.MAP, request.consume())
    }

    @Test
    fun laterRequest_replacesAnUnconsumedOne() {
        val request = ExploreViewRequest()
        request.request(ExploreViewMode.LIST)
        request.request(ExploreViewMode.MAP)

        assertEquals(ExploreViewMode.MAP, request.consume())
        assertNull(request.consume())
    }

    @Test
    fun newRequest_afterConsuming_isDeliveredAgain() {
        val request = ExploreViewRequest()
        request.request(ExploreViewMode.MAP)
        request.consume()

        request.request(ExploreViewMode.MAP)

        assertEquals(ExploreViewMode.MAP, request.consume())
    }

    @Test
    fun consuming_appliesTheModeWithoutTouchingSearchOrFilters() {
        val state = ExploreState(offers = emptyList(), viewMode = ExploreViewMode.LIST)
        state.query = "pan"
        val request = ExploreViewRequest()
        request.request(ExploreViewMode.MAP)

        request.consume()?.let { state.viewMode = it }

        assertEquals(ExploreViewMode.MAP, state.viewMode)
        assertEquals("pan", state.query)
    }
}
