package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

private const val BANNER_MILLIS = 200

/**
 * Form-level error. It expands in when [message] is set and collapses when it is null.
 * Screen readers announce the text when it appears.
 */
@Composable
fun FormErrorBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    // Keeps the last text on screen while the banner collapses
    var lastMessage by remember { mutableStateOf(message.orEmpty()) }
    if (message != null) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = if (animationsEnabled) {
            expandVertically(animationSpec = tween(BANNER_MILLIS)) +
                fadeIn(tween(BANNER_MILLIS))
        } else {
            EnterTransition.None
        },
        exit = if (animationsEnabled) {
            shrinkVertically(animationSpec = tween(BANNER_MILLIS)) +
                fadeOut(tween(BANNER_MILLIS))
        } else {
            ExitTransition.None
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(AppRadius.large),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = lastMessage,
                modifier = Modifier.padding(AppSpacing.regular),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FormErrorBannerPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.regular)) {
                FormErrorBanner(message = "Correo o contraseña incorrectos.")
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun FormErrorBannerLightPreview() {
    FormErrorBannerPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FormErrorBannerDarkPreview() {
    FormErrorBannerPreviewContent(ThemeMode.DARK)
}
