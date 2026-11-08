package com.zerowasteeats.app.presentation.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import java.text.NumberFormat
import java.util.Locale

/** One decimal with the language's separator: "0,5" in Spanish, "0.5" in English. */
fun formatKilometres(km: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(km)

/** "0,5 km" in the language of the app, or null when the distance is unknown. */
@Composable
fun distanceText(km: Double?): String? {
    if (km == null) return null
    // Observable, so the text follows a language change
    val locale = LocalLocale.current.platformLocale
    return stringResource(R.string.offer_distance_km, formatKilometres(km, locale))
}
