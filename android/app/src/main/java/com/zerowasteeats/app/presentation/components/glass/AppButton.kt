package com.zerowasteeats.app.presentation.components.glass

import androidx.compose.foundation.BorderStroke
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
import com.zerowasteeats.app.presentation.theme.SurfaceTokens

enum class AppButtonStyle {
    PRIMARY,
    SECONDARY
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val backgroundColor = when (style) {
        AppButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f)
        AppButtonStyle.SECONDARY -> SurfaceTokens.surface
    }

    val finalBackgroundColor = if (style == AppButtonStyle.SECONDARY && !enabled) {
        backgroundColor.copy(alpha = backgroundColor.alpha * 0.5f)
    } else {
        backgroundColor
    }

    val contentColor = when (style) {
        AppButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.6f)
        AppButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.6f)
    }

    val borderColor = when (style) {
        AppButtonStyle.PRIMARY -> Color.Transparent
        AppButtonStyle.SECONDARY -> SurfaceTokens.border
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
        border = if (style == AppButtonStyle.SECONDARY) BorderStroke(SurfaceTokens.borderWidth, borderColor) else null,
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
