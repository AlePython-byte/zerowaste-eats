package com.zerowasteeats.app.presentation.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** The check is drawn on a square of this many units and scaled to the size of the canvas. */
private const val CHECK_UNITS = 24f

private const val CHECK_STROKE_UNITS = 2.4f

/** The three points of the check: the start of the short arm, the elbow and the end of the long arm. */
private val CheckStart = Offset(5f, 12.5f)
private val CheckElbow = Offset(9.5f, 17f)
private val CheckEnd = Offset(19f, 7.5f)

/**
 * A check mark that draws itself: the stroke grows from the short arm along to the end of the
 * long one as [progress] goes from 0 to 1. It has no description of its own.
 */
@Composable
fun DrawnCheck(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val scale = size.minDimension / CHECK_UNITS
        val full = Path().apply {
            moveTo(CheckStart.x * scale, CheckStart.y * scale)
            lineTo(CheckElbow.x * scale, CheckElbow.y * scale)
            lineTo(CheckEnd.x * scale, CheckEnd.y * scale)
        }
        val measure = PathMeasure().apply { setPath(full, forceClosed = false) }
        val shown = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), shown, startWithMoveTo = true)
        drawPath(
            path = shown,
            color = color,
            style = Stroke(
                width = CHECK_STROKE_UNITS * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
