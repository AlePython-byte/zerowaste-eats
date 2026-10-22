package com.zerowasteeats.app.presentation.components.glass

import androidx.compose.foundation.BorderStroke
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
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.regular),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.large),
        color = GlassTokens.surface,
        border = BorderStroke(
            width = GlassTokens.borderWidth,
            color = GlassTokens.border
        ),
        shadowElevation = GlassTokens.elevation
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}