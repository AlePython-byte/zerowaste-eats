package com.zerowasteeats.app.presentation.format

import kotlin.math.roundToLong

/** A whole count as the statistics show it, rounded from a figure that may be on its way up. */
fun formatCount(value: Double): String = value.roundToLong().toString()
