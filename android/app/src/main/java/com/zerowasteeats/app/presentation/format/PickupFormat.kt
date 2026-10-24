package com.zerowasteeats.app.presentation.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.model.PickupDay
import com.zerowasteeats.app.presentation.model.PickupWindow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Locale-dependent parts of a pickup window. Pure, so they can be tested on the JVM. */
object PickupFormat {

    /** "6:00 PM - 7:30 PM" in English, "6:00 p. m. - 7:30 p. m." in Spanish. */
    fun timeRange(start: LocalTime, end: LocalTime, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", locale)
        return "${formatter.format(start)} - ${formatter.format(end)}"
    }

    /** "Oct 12" in English, "12 oct" in Spanish. */
    fun date(date: LocalDate, locale: Locale): String {
        val pattern = if (locale.language == "en") "MMM d" else "d MMM"
        return DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}

/**
 * The pickup window as text in the language of the app. With [withPickupPrefix] it reads as a
 * sentence ("Pickup today, 6:00 PM - 7:30 PM"), as on offers; without, it starts with the day
 * ("Today, 6:00 PM - 7:30 PM"), as on orders.
 */
@Composable
fun PickupWindow.formatted(withPickupPrefix: Boolean): String {
    // Reading the configuration makes this follow a language change
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val times = PickupFormat.timeRange(start, end, locale)
    val dayText = when (val day = day) {
        PickupDay.Today -> stringResource(if (withPickupPrefix) R.string.pickup_day_today else R.string.order_day_today)
        PickupDay.Tomorrow -> stringResource(if (withPickupPrefix) R.string.pickup_day_tomorrow else R.string.order_day_tomorrow)
        PickupDay.Yesterday -> stringResource(if (withPickupPrefix) R.string.pickup_day_yesterday else R.string.order_day_yesterday)
        is PickupDay.On -> {
            val date = PickupFormat.date(day.date, locale)
            if (withPickupPrefix) stringResource(R.string.pickup_day_date, date) else date
        }
    }
    return if (withPickupPrefix) {
        stringResource(R.string.pickup_offer_format, dayText, times)
    } else {
        stringResource(R.string.pickup_order_format, dayText, times)
    }
}
