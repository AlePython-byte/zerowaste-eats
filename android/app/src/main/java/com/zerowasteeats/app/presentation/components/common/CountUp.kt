package com.zerowasteeats.app.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Remembers which figures already counted up, so a figure counts from zero only the first time
 * it is shown and not every time its screen is opened again.
 */
class CountUpHistory {
    private val played = mutableSetOf<String>()

    /** True the first time it is called with this [key], false from then on. */
    fun claim(key: String): Boolean = played.add(key)
}

/** The history of the running app: a figure counts up once until the app is closed. */
private val AppCountUpHistory = CountUpHistory()

/**
 * A figure for statistics that counts from zero up to [target] the first time the [key] is
 * shown, and is simply [target] afterwards: when the screen is opened again, when the figure
 * changes later, and when the system animations are off.
 */
@Composable
fun rememberCountedValue(target: Double, key: String): Double {
    val animate = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val countsUp = remember(key) { animate && AppCountUpHistory.claim(key) }
    // How much of the way to the target it has counted: the target itself may change meanwhile
    val progress = remember(key) { Animatable(if (countsUp) 0f else 1f) }

    LaunchedEffect(countsUp) {
        if (countsUp) {
            progress.animateTo(1f, tween(AppMotion.LONG_MILLIS, easing = AppMotion.Emphasized))
        }
    }
    return target * progress.value
}
