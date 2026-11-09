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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    SECONDARY,

    /** For actions that cannot be undone, such as deleting: the error color of the theme. */
    DESTRUCTIVE
}

/** Every style fades the same way when the button is disabled: the fill to this much... */
private const val DISABLED_CONTAINER_ALPHA = 0.4f

/** ...and the label and border to this much, so disabled reads the same on any style. */
private const val DISABLED_CONTENT_ALPHA = 0.6f

/**
 * A button with the app's three states, the same on every style: enabled; disabled, faded
 * (see [DISABLED_CONTAINER_ALPHA]); and [isLoading], which keeps the enabled look, shows a
 * spinner in place of the label and ignores taps. While it is enabled it shrinks a little
 * under the finger.
 */
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
    val containerAlpha = if (enabled) 1f else DISABLED_CONTAINER_ALPHA
    val contentAlpha = if (enabled) 1f else DISABLED_CONTENT_ALPHA

    val finalBackgroundColor = when (style) {
        AppButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        AppButtonStyle.SECONDARY -> SurfaceTokens.surface
        AppButtonStyle.DESTRUCTIVE -> MaterialTheme.colorScheme.error
    }.let { it.copy(alpha = it.alpha * containerAlpha) }

    val contentColor = when (style) {
        AppButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        AppButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
        AppButtonStyle.DESTRUCTIVE -> MaterialTheme.colorScheme.onError
    }.let { it.copy(alpha = it.alpha * contentAlpha) }

    val borderColor = when (style) {
        AppButtonStyle.SECONDARY -> SurfaceTokens.border.let { it.copy(alpha = it.alpha * contentAlpha) }
        else -> Color.Transparent
    }

    val interactionSource = remember { MutableInteractionSource() }

    val loadingDescription = stringResource(R.string.button_loading)

    // While loading the button looks the same as when enabled: only the click is off
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .pressable(
                interactionSource,
                scaleWhenPressed = PRESSED_CONTROL_SCALE,
                enabled = enabled && !isLoading
            )
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
                AppButton(text = "Ingresar", onClick = {}, enabled = false)
                AppButton(text = "Eliminar", onClick = {}, style = AppButtonStyle.DESTRUCTIVE)
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
