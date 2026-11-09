package com.zerowasteeats.app.presentation.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How much a card shrinks while it is pressed. */
const val PRESSED_CARD_SCALE = 0.98f

/** How much a button, chip or other small control shrinks while it is pressed. */
const val PRESSED_CONTROL_SCALE = 0.96f

/** How far a pressed card lifts off the page, so it reads as being held. */
val PRESSED_CARD_RAISE = 4.dp

/** The scale at [progress] of the way from rest (0) to fully pressed (1). */
internal fun pressedScale(progress: Float, scaleWhenPressed: Float): Float =
    1f - (1f - scaleWhenPressed) * progress

/**
 * Press feedback shared by the cards and buttons of the app: while the finger is down the
 * element shrinks a little to [scaleWhenPressed] and, if [raise] is given, lifts with a soft
 * shadow of that height shaped like [shape]. It follows the finger with [AppMotion.pressSpring],
 * which never bounces, and does nothing when the system animations are off or [enabled] is
 * false. Pass the same [interactionSource] to the clickable it decorates.
 */
@Composable
fun Modifier.pressable(
    interactionSource: InteractionSource,
    shape: Shape = RectangleShape,
    scaleWhenPressed: Float = PRESSED_CARD_SCALE,
    raise: Dp = 0.dp,
    enabled: Boolean = true
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = rememberAnimationsEnabled()
    val progress by animateFloatAsState(
        targetValue = if (isPressed && enabled && animationsEnabled) 1f else 0f,
        animationSpec = AppMotion.pressSpring(),
        label = "pressableProgress"
    )
    return graphicsLayer {
        val scale = pressedScale(progress, scaleWhenPressed)
        scaleX = scale
        scaleY = scale
        if (raise > 0.dp) {
            shadowElevation = raise.toPx() * progress
            this.shape = shape
            clip = false
        }
    }
}
