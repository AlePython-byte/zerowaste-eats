package com.zerowasteeats.app.presentation.components.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** One sweep of the light across a placeholder: three long beats, with a calm pause at each end. */
private const val SHIMMER_MILLIS = AppMotion.LONG_MILLIS * 3

/** How wide the band of light is, as a share of the placeholder's width. */
private const val SHIMMER_BAND_SHARE = 0.6f

private const val SHIMMER_HIGHLIGHT_ALPHA = 0.55f

/**
 * Where the left edge of the shimmer band is, in pixels, for a sweep [progress] from 0 to 1:
 * it starts fully off the left side and ends fully off the right side.
 */
internal fun shimmerBandStart(progress: Float, width: Float, bandWidth: Float): Float =
    -bandWidth + progress.coerceIn(0f, 1f) * (width + bandWidth)

/** The sweep progress shared by every placeholder under a [ProvideSkeletonShimmer]; null means still. */
private val LocalSkeletonShimmer = staticCompositionLocalOf<State<Float>?> { null }

/**
 * Runs the one clock that all the loading placeholders of a screen share, so their lights sweep
 * together and the screen keeps a single infinite animation. It runs only while [active] (the
 * placeholders are on screen) and when the system animations are on; otherwise placeholders
 * stay still.
 */
@Composable
fun ProvideSkeletonShimmer(active: Boolean, content: @Composable () -> Unit) {
    val progress = if (active && rememberAnimationsEnabled()) {
        val transition = rememberInfiniteTransition(label = "skeletonShimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(SHIMMER_MILLIS, easing = AppMotion.Standard),
                repeatMode = RepeatMode.Restart
            ),
            label = "skeletonShimmerProgress"
        )
    } else {
        null
    }
    CompositionLocalProvider(LocalSkeletonShimmer provides progress, content = content)
}

/**
 * Sweeps a soft band of light over the placeholder it is applied to. Place it before the
 * background so the light covers the background and the lines on it. Draws nothing extra when
 * there is no [ProvideSkeletonShimmer] above or the animations are off. The progress is read
 * while drawing, so the sweep never recomposes anything.
 */
@Composable
fun Modifier.skeletonShimmer(): Modifier {
    val progress = LocalSkeletonShimmer.current ?: return this
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA)
    return drawWithContent {
        drawContent()
        val bandWidth = size.width * SHIMMER_BAND_SHARE
        val start = shimmerBandStart(progress.value, size.width, bandWidth)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                startX = start,
                endX = start + bandWidth
            )
        )
    }
}
