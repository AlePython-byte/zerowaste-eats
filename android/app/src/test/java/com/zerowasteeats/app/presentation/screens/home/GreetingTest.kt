package com.zerowasteeats.app.presentation.screens.home

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test
    fun morning_runsFromFiveToElevenFiftyNine() {
        assertEquals(Greeting.MORNING, Greeting.at(LocalTime.of(5, 0)))
        assertEquals(Greeting.MORNING, Greeting.at(LocalTime.of(11, 59)))
    }

    @Test
    fun afternoon_runsFromNoonToSixFiftyNine() {
        assertEquals(Greeting.AFTERNOON, Greeting.at(LocalTime.of(12, 0)))
        assertEquals(Greeting.AFTERNOON, Greeting.at(LocalTime.of(18, 59)))
    }

    @Test
    fun evening_coversTheRestIncludingMidnightAndTheSmallHours() {
        assertEquals(Greeting.EVENING, Greeting.at(LocalTime.of(19, 0)))
        assertEquals(Greeting.EVENING, Greeting.at(LocalTime.of(23, 59)))
        assertEquals(Greeting.EVENING, Greeting.at(LocalTime.MIDNIGHT))
        assertEquals(Greeting.EVENING, Greeting.at(LocalTime.of(4, 59)))
    }

    @Test
    fun now_readsTheInjectedClock() {
        val clock = Clock.fixed(Instant.parse("2026-03-10T15:30:00Z"), ZoneOffset.UTC)

        assertEquals(Greeting.AFTERNOON, Greeting.now(clock))
    }

    @Test
    fun now_usesTheClockZone() {
        val instant = Instant.parse("2026-03-10T03:00:00Z")

        assertEquals(Greeting.EVENING, Greeting.now(Clock.fixed(instant, ZoneOffset.UTC)))
        assertEquals(Greeting.MORNING, Greeting.now(Clock.fixed(instant, ZoneOffset.ofHours(5))))
    }
}
