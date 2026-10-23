package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

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
    isLoading: Boolean = false,
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

    val loadingDescription = stringResource(R.string.button_loading)

    // While loading the button looks the same as when enabled: only the click is off
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .then(
                if (isLoading) Modifier.semantics { stateDescription = loadingDescription }
                else Modifier
            ),
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
        // The label stays in the layout (invisible) so the button keeps its width
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                verticalAlignment = Alignment.CenterVertically
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}


@Composable
private fun AppButtonPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                AppButton(text = "Ingresar", onClick = {})
                AppButton(text = "Ingresar", onClick = {}, isLoading = true)
                AppButton(text = "Crear cuenta", onClick = {}, style = AppButtonStyle.SECONDARY)
                AppButton(
                    text = "Crear cuenta",
                    onClick = {},
                    style = AppButtonStyle.SECONDARY,
                    isLoading = true
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun AppButtonLightPreview() {
    AppButtonPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppButtonDarkPreview() {
    AppButtonPreviewContent(ThemeMode.DARK)
}
