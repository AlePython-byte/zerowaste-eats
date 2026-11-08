package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing

private const val PULSE_MILLIS = 900

/**
 * The shape of a [MerchantOfferCard] while the offers load: the photo square and a few lines
 * of text, in the theme's quiet surface color. It breathes slowly only when the system
 * animations are on. Hidden from screen readers; the list announces the loading instead.
 */
@Composable
fun MerchantOfferCardSkeleton(modifier: Modifier = Modifier) {
    val alpha = if (rememberAnimationsEnabled()) {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(PULSE_MILLIS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeletonAlpha"
        )
        value
    } else {
        1f
    }
    val block = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(AppRadius.medium)

    AppCard(modifier = modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Row(
            modifier = Modifier.alpha(alpha),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Box(modifier = Modifier.size(72.dp).clip(shape).background(block))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(shape).background(block))
                Box(Modifier.fillMaxWidth(0.4f).height(16.dp).clip(shape).background(block))
                Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(shape).background(block))
            }
        }
    }
}
