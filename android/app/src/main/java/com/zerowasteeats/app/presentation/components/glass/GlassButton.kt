package com.zerowasteeats.app.presentation.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

enum class GlassButtonStyle {
    PRIMARY,
    SECONDARY
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = when (style) {
        GlassButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.85f else 0.4f)
        GlassButtonStyle.SECONDARY -> if (isDarkTheme) GlassTokens.darkSurface else GlassTokens.lightSurface
    }

    val finalBackgroundColor = if (style == GlassButtonStyle.SECONDARY && !enabled) {
        backgroundColor.copy(alpha = backgroundColor.alpha * 0.5f)
    } else {
        backgroundColor
    }

    val contentColor = when (style) {
        GlassButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.6f)
        GlassButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.6f)
    }

    val borderColor = when (style) {
        GlassButtonStyle.PRIMARY -> Color.Transparent
        GlassButtonStyle.SECONDARY -> if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = finalBackgroundColor,
            contentColor = contentColor,
            disabledContainerColor = finalBackgroundColor,
            disabledContentColor = contentColor
        ),
        border = if (style == GlassButtonStyle.SECONDARY) BorderStroke(GlassTokens.borderWidth, borderColor) else null,
        contentPadding = PaddingValues(
            horizontal = AppSpacing.extraLarge,
            vertical = AppSpacing.medium
        )
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(AppSpacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
