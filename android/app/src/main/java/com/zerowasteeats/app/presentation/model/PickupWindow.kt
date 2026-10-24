package com.zerowasteeats.app.presentation.model

import java.time.LocalDate
import java.time.LocalTime

/** The day of a pickup, relative to the day the screen is shown, or a fixed date. */
sealed interface PickupDay {
    data object Today : PickupDay
    data object Tomorrow : PickupDay
    data object Yesterday : PickupDay
    data class On(val date: LocalDate) : PickupDay
}

/**
 * When an order or an offer can be picked up. Holds data, not text: the screens format it in
 * the language of the app.
 */
data class PickupWindow(
    val day: PickupDay,
    val start: LocalTime,
    val end: LocalTime
)
