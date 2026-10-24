package com.zerowasteeats.app.presentation.screens.home

import androidx.annotation.StringRes
import com.zerowasteeats.app.R
import java.time.Clock
import java.time.LocalTime

/** Part of the day the home greeting follows: 5:00 to 11:59, 12:00 to 18:59, the rest. */
enum class Greeting(@param:StringRes val textRes: Int) {
    MORNING(R.string.home_greeting_morning),
    AFTERNOON(R.string.home_greeting_afternoon),
    EVENING(R.string.home_greeting_evening);

    companion object {
        private const val MORNING_START_HOUR = 5
        private const val AFTERNOON_START_HOUR = 12
        private const val EVENING_START_HOUR = 19

        fun at(time: LocalTime): Greeting = when (time.hour) {
            in MORNING_START_HOUR until AFTERNOON_START_HOUR -> MORNING
            in AFTERNOON_START_HOUR until EVENING_START_HOUR -> AFTERNOON
            else -> EVENING
        }

        /** The clock is injected so the greeting can be tested at any hour. */
        fun now(clock: Clock): Greeting = at(LocalTime.now(clock))
    }
}
