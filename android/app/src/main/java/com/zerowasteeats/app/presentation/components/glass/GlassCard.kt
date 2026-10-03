package com.zerowasteeats.app.presentation.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.regular),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = if (isDarkTheme) {
        GlassTokens.darkSurface
    } else {
        GlassTokens.lightSurface
    }

    val borderColor = if (isDarkTheme) {
        GlassTokens.darkBorder
    } else {
        GlassTokens.lightBorder
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.large),
        color = backgroundColor,
        border = BorderStroke(
            width = GlassTokens.borderWidth,
            color = borderColor
        ),
        shadowElevation = GlassTokens.elevation
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}