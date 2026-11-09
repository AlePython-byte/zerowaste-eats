package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.AppMotion
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import kotlinx.coroutines.launch

/**
 * Heart toggle for offers that sit over a photo: vibrates when pressed and, when the system
 * animations are on, the heart bounces and marking it sets off a small burst of sparks.
 * The visible circle is 44dp; the touch area is 48dp.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartScale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val animationsEnabled = rememberAnimationsEnabled()

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(
                    if (isFavorite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                )
                if (animationsEnabled) {
                    scope.launch {
                        heartScale.animateTo(1.35f, AppMotion.bounceSpring())
                        heartScale.animateTo(1f, AppMotion.restSpring())
                    }
                    // Only marking is rewarded; removing a favorite is quiet
                    if (!isFavorite) scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, tween(AppMotion.MEDIUM_MILLIS, easing = AppMotion.Emphasized))
                        burst.snapTo(0f)
                    }
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RescateCarbon.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            HeartBurst(
                progress = burst.value,
                color = RescateCoral,
                modifier = Modifier.fillMaxSize()
            )
            HeartIcon(
                filled = isFavorite,
                color = if (isFavorite) RescateCoral else RescateCream,
                size = 22.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                }
            )
        }
    }
}
