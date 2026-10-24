package com.zerowasteeats.app.presentation.format

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PickupFormatTest {

    private val english = Locale.forLanguageTag("en")
    private val spanish = Locale.forLanguageTag("es")

    // Some JDKs put a no-break space before the period of the day; compare with plain spaces
    private fun String.plainSpaces() = replace('\u00A0', ' ').replace('\u202F', ' ')

    @Test
    fun timeRange_inEnglishUsesUppercaseMeridiem() {
        assertEquals(
            "6:00 PM - 7:30 PM",
            PickupFormat.timeRange(LocalTime.of(18, 0), LocalTime.of(19, 30), english).plainSpaces()
        )
    }

    @Test
    fun timeRange_inSpanishUsesSpanishMeridiem() {
        assertEquals(
            "6:00 p. m. - 7:30 p. m.",
            PickupFormat.timeRange(LocalTime.of(18, 0), LocalTime.of(19, 30), spanish).plainSpaces()
        )
    }

    @Test
    fun timeRange_handlesMorningNoonAndMidnight() {
        assertEquals(
            "8:00 AM - 12:00 PM",
            PickupFormat.timeRange(LocalTime.of(8, 0), LocalTime.NOON, english).plainSpaces()
        )
        assertEquals(
            "12:00 AM - 12:30 AM",
            PickupFormat.timeRange(LocalTime.MIDNIGHT, LocalTime.of(0, 30), english).plainSpaces()
        )
    }

    @Test
    fun date_putsTheMonthFirstInEnglish() {
        assertEquals("Oct 12", PickupFormat.date(LocalDate.of(2025, 10, 12), english))
    }

    @Test
    fun date_putsTheDayFirstInSpanish() {
        val text = PickupFormat.date(LocalDate.of(2025, 10, 12), spanish)

        assertEquals("12 oct", text.trimEnd('.'))
    }
}
