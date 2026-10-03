package com.zerowasteeats.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppRadius {
    val small = 10.dp
    val medium = 16.dp
    val large = 22.dp
    val extraLarge = 28.dp
}

val ZeroWasteShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.small),
    small = RoundedCornerShape(AppRadius.medium),
    medium = RoundedCornerShape(AppRadius.large),
    large = RoundedCornerShape(AppRadius.extraLarge),
    extraLarge = RoundedCornerShape(32.dp)
)