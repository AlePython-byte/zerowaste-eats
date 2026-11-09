package com.zerowasteeats.app.presentation.components.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountUpHistoryTest {

    @Test
    fun `a figure counts up the first time it is shown`() {
        assertTrue(CountUpHistory().claim("profile.orders"))
    }

    @Test
    fun `it does not count up again when shown again`() {
        val history = CountUpHistory()
        history.claim("profile.orders")

        assertFalse(history.claim("profile.orders"))
    }

    @Test
    fun `each figure keeps its own turn`() {
        val history = CountUpHistory()
        history.claim("profile.orders")

        assertTrue(history.claim("dashboard.rescued"))
    }
}
