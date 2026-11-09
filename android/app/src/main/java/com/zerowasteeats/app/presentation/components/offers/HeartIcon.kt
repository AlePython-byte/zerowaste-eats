package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The heart is drawn on a square of this many units and scaled to the icon's size. */
private const val HEART_UNITS = 24f

/** The outline's thickness, in the same units. */
private const val HEART_OUTLINE_UNITS = 2f

/** Builds the heart on a [HEART_UNITS] square, scaled by [scale]. */
private fun heartPath(scale: Float): Path = Path().apply {
    fun x(value: Float) = value * scale
    moveTo(x(12f), x(21.35f))
    relativeLineTo(x(-1.45f), x(-1.32f))
    cubicTo(x(5.4f), x(15.36f), x(2f), x(12.28f), x(2f), x(8.5f))
    cubicTo(x(2f), x(5.42f), x(4.42f), x(3f), x(7.5f), x(3f))
    relativeCubicTo(x(1.74f), 0f, x(3.41f), x(0.81f), x(4.5f), x(2.09f))
    cubicTo(x(13.09f), x(3.81f), x(14.76f), x(3f), x(16.5f), x(3f))
    cubicTo(x(19.58f), x(3f), x(22f), x(5.42f), x(22f), x(8.5f))
    relativeCubicTo(0f, x(3.78f), x(-3.4f), x(6.86f), x(-8.55f), x(11.54f))
    lineTo(x(12f), x(21.35f))
    close()
}

/**
 * A heart drawn as a vector: solid in [color] when [filled], only its outline otherwise. It
 * has no description of its own; the control around it speaks for it.
 */
@Composable
fun HeartIcon(
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier.size(size)) {
        val path = heartPath(this.size.width / HEART_UNITS)
        if (filled) {
            drawPath(path, color, style = Fill)
        } else {
            val outline = HEART_OUTLINE_UNITS * this.size.width / HEART_UNITS
            drawPath(path, color, style = Stroke(width = outline, join = StrokeJoin.Round))
        }
    }
}

/** How many sparks fly out of the heart when it is marked. */
internal const val BURST_SPARKS = 8

/** Where the burst has reached, from 0 (at the heart) to 1 (all the way out and gone). */
internal fun burstRadius(progress: Float, maxRadius: Float): Float = maxRadius * progress.coerceIn(0f, 1f)

/** The sparks stay solid for the first part of the burst and then fade out. */
internal fun burstAlpha(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p <= BURST_FADE_START) 1f else 1f - (p - BURST_FADE_START) / (1f - BURST_FADE_START)
}

private const val BURST_FADE_START = 0.4f

/** The centre of spark [index] of [BURST_SPARKS] around [center]; the first one points up. */
internal fun burstSparkCenter(index: Int, center: Offset, radius: Float): Offset {
    val angle = 2.0 * PI * index / BURST_SPARKS - PI / 2
    return Offset(
        x = center.x + radius * cos(angle).toFloat(),
        y = center.y + radius * sin(angle).toFloat()
    )
}

/**
 * The burst that rewards marking a favorite: sparks fly out from the middle and fade. Nothing is
 * drawn at rest ([progress] 0) or once it is over (1).
 */
@Composable
fun HeartBurst(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        drawBurst(progress, color)
    }
}

private fun DrawScope.drawBurst(progress: Float, color: Color) {
    val radius = burstRadius(progress, maxRadius = size.minDimension * BURST_REACH)
    val spark = size.minDimension * SPARK_SIZE * (1f - progress / 2f)
    val tint = color.copy(alpha = color.alpha * burstAlpha(progress))
    repeat(BURST_SPARKS) { index ->
        drawCircle(tint, radius = spark, center = burstSparkCenter(index, this.center, radius))
    }
}

/** How far the sparks go and how big they start, as a share of the burst's area. */
private const val BURST_REACH = 0.42f
private const val SPARK_SIZE = 0.05f
