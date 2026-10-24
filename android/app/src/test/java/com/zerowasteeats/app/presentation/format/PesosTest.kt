package com.zerowasteeats.app.presentation.format

import org.junit.Assert.assertEquals
import org.junit.Test

class PesosTest {

    @Test
    fun smallAmounts_haveNoSeparator() {
        assertEquals("$0", formatPesos(0))
        assertEquals("$950", formatPesos(950))
    }

    @Test
    fun thousands_areSeparatedWithADot() {
        assertEquals("$8.900", formatPesos(8_900))
        assertEquals("$15.000", formatPesos(15_000))
    }

    @Test
    fun millions_haveTwoSeparators() {
        assertEquals("$1.250.000", formatPesos(1_250_000))
    }
}
