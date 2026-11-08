package com.zerowasteeats.app.presentation.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {

    private val spanish = Locale.forLanguageTag("es-CO")

    @Test
    fun oneDecimal_withTheLanguagesSeparator() {
        assertEquals("0,5", formatKilometres(0.5, spanish))
        assertEquals("0.5", formatKilometres(0.5, Locale.ENGLISH))
    }

    @Test
    fun wholeKilometres_stillShowTheDecimal() {
        assertEquals("3,0", formatKilometres(3.0, spanish))
    }

    @Test
    fun roundsToOneDecimal() {
        assertEquals("1.3", formatKilometres(1.26, Locale.ENGLISH))
    }
}
