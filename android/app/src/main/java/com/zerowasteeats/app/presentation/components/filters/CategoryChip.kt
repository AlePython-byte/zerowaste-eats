package com.zerowasteeats.app.presentation.components.filters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Highly refined, restrained chips
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDarkTheme) GlassTokens.darkSurface.copy(alpha = 0.5f) 
        else GlassTokens.lightSurface.copy(alpha = 0.5f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        Color.Transparent
    } else {
        if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(AppRadius.extraLarge))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = containerColor,
        border = if (!isSelected) BorderStroke(GlassTokens.borderWidth, borderColor) else null,
        shadowElevation = 0.dp // Flatter chips to let cards pop
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.large)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
