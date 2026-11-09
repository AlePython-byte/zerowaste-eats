package com.zerowasteeats.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Opacity of the 1 dp border of cards: a little more in dark mode, where shadows hardly show. */
internal fun surfaceBorderAlpha(isDark: Boolean): Float = if (isDark) 0.14f else 0.10f

/** Shadow height of cards: none in dark mode, where the border does the separating. */
internal fun surfaceElevation(isDark: Boolean): Dp = if (isDark) 0.dp else 2.dp

// Opaque surfaces: a translucent Surface with a shadow lets the shadow show
// through the card as an inner rectangle, so surfaces follow the theme instead.
object SurfaceTokens {

    private val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val border: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = surfaceBorderAlpha(isDark))

    val borderWidth = 1.dp

    /** Soft shadow shared by all cards; flat in dark mode, where [border] separates them. */
    val elevation: Dp
        @Composable get() = surfaceElevation(isDark)

    val barElevation = 4.dp
}
