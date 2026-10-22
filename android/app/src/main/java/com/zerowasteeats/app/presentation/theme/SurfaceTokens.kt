package com.zerowasteeats.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Opaque surfaces: a translucent Surface with a shadow lets the shadow show
// through the card as an inner rectangle, so surfaces follow the theme instead.
object SurfaceTokens {

    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val border: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    val borderWidth = 1.dp
    val elevation = 2.dp
    val barElevation = 4.dp
}
