package com.zerowasteeats.app.presentation.format

import org.junit.Assert.assertEquals
import org.junit.Test

class CountTest {

    @Test
    fun `a whole figure shows as it is`() {
        assertEquals("12", formatCount(12.0))
    }

    @Test
    fun `a figure on its way up rounds to the nearest whole`() {
        assertEquals("4", formatCount(3.6))
        assertEquals("3", formatCount(3.4))
    }

    @Test
    fun `zero shows as zero`() {
        assertEquals("0", formatCount(0.0))
    }
}
