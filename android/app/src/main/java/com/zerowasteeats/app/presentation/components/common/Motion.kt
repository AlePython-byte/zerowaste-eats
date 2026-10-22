package com.zerowasteeats.app.presentation.components.common

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * False when the user turned system animations off (animator duration scale 0).
 * Decorative effects check this and skip themselves.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

private const val ENTRANCE_MILLIS = 300
private const val PLACEMENT_MILLIS = 300

/**
 * Modifier for an offer card inside a LazyColumn: it glides to its new position when the
 * list is filtered or reordered, and fades in with a slight rise the first time its id
 * shows up. [seen] is owned by the screen so cards scrolled back into view do not replay it.
 */
@Composable
fun LazyItemScope.offerListItem(seen: MutableSet<String>, id: String): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val alreadySeen = remember { id in seen }
    val progress = remember { Animatable(if (alreadySeen || !animationsEnabled) 1f else 0f) }

    LaunchedEffect(Unit) {
        seen.add(id)
        if (progress.value < 1f) {
            progress.animateTo(1f, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
        }
    }

    return Modifier
        .animateItem(
            fadeInSpec = null, // the entrance below already fades the card in
            placementSpec = if (animationsEnabled) {
                tween(PLACEMENT_MILLIS, easing = FastOutSlowInEasing)
            } else {
                null
            }
        )
        .graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 16.dp.toPx()
        }
}
