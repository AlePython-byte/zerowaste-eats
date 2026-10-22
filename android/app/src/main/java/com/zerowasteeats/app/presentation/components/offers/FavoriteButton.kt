package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import kotlinx.coroutines.launch

/** Heart toggle for offer cards that sit over a photo: bounces and vibrates when pressed. */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(RescateCarbon.copy(alpha = 0.55f))
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(
                    if (isFavorite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                )
                scope.launch {
                    heartScale.animateTo(
                        1.35f,
                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                    )
                    heartScale.animateTo(1f, spring())
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            fontSize = 22.sp,
            color = if (isFavorite) RescateCoral else RescateCream,
            modifier = Modifier.graphicsLayer {
                scaleX = heartScale.value
                scaleY = heartScale.value
            }
        )
    }
}
