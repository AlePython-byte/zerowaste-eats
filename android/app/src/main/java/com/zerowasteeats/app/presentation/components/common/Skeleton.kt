package com.zerowasteeats.app.presentation.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

private const val PULSE_MILLIS = 900

/**
 * The opacity of a loading placeholder: a slow breath between full and half when the system
 * animations are on, steady otherwise.
 */
@Composable
fun rememberSkeletonAlpha(): Float {
    if (!rememberAnimationsEnabled()) return 1f
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = AppMotion.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return alpha
}
