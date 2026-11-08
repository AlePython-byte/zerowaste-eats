package com.zerowasteeats.app.presentation.components.brand

import androidx.compose.animation.core.FastOutSlowInEasing
import com.zerowasteeats.app.presentation.components.common.LOGO_HANDLE_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HANDLE_START_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HEART_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HEART_START_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_LEAF_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_LEAF_START_MILLIS
import kotlin.math.PI
import kotlin.math.sin

/**
 * What the logo shows at a given moment of its animation. Pure arithmetic, so the phases can
 * be tested without drawing. The bag is not here: it springs in on a clock of its own.
 */
data class LogoFrame(
    /** 0 to 1: how much of the handle is drawn, from one end to the other. */
    val handleProgress: Float,
    /** 0 to 1: the leaf grows from its base, turning slightly as it goes. */
    val leafProgress: Float,
    /** Scale of the heart; 1 at rest, a little more at the peak of the single beat. */
    val heartScale: Float
) {
    companion object {
        const val HEART_BEAT_AMPLITUDE = 0.08f

        /** Leaf angle at the start, in degrees; it ends upright. */
        const val LEAF_START_ANGLE = -25f

        /** Fully drawn, as the static logo is. */
        val Complete = LogoFrame(handleProgress = 1f, leafProgress = 1f, heartScale = 1f)

        /** Nothing drawn yet, apart from the bag. */
        val Start = LogoFrame(handleProgress = 0f, leafProgress = 0f, heartScale = 1f)

        fun at(elapsedMillis: Float): LogoFrame = LogoFrame(
            handleProgress = FastOutSlowInEasing.transform(
                phase(elapsedMillis, LOGO_HANDLE_START_MILLIS, LOGO_HANDLE_MILLIS)
            ),
            leafProgress = FastOutSlowInEasing.transform(
                phase(elapsedMillis, LOGO_LEAF_START_MILLIS, LOGO_LEAF_MILLIS)
            ),
            heartScale = 1f + HEART_BEAT_AMPLITUDE *
                sin(PI.toFloat() * phase(elapsedMillis, LOGO_HEART_START_MILLIS, LOGO_HEART_MILLIS))
        )

        /** Progress 0 to 1 of the step that starts at [start] and lasts [duration]. */
        fun phase(elapsedMillis: Float, start: Int, duration: Int): Float =
            ((elapsedMillis - start) / duration).coerceIn(0f, 1f)
    }
}
