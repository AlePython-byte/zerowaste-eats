package com.zerowasteeats.app.presentation.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A price in whole pesos as the app shows it everywhere: "$8.900", with a dot between thousands,
 * whatever language the app is in.
 */
fun formatPesos(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
    return "$" + DecimalFormat("#,##0", symbols).format(amount)
}
