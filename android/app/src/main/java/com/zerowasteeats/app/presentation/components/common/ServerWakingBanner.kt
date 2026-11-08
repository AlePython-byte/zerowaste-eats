package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import kotlinx.coroutines.delay

/**
 * Says the server is waking up when its first answer is slow: it appears once a call has
 * waited [slowAfterMillis] since [waitingSince], and leaves as soon as the server answers.
 * Screen readers hear it once. It slides in unless animations are off.
 */
@Composable
fun ServerWakingBanner(
    waitingSince: Long?,
    slowAfterMillis: Long,
    nowMillis: () -> Long,
    modifier: Modifier = Modifier
) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(waitingSince) {
        slow = false
        if (waitingSince == null) return@LaunchedEffect
        delay((waitingSince + slowAfterMillis - nowMillis()).coerceAtLeast(0))
        slow = true
    }

    val animate = rememberAnimationsEnabled()
    AnimatedVisibility(
        visible = slow && waitingSince != null,
        modifier = modifier,
        enter = if (animate) fadeIn(tween(ENTRANCE_MILLIS)) + slideInVertically { -it / 2 } else EnterTransition.None,
        exit = if (animate) fadeOut(tween(ENTRANCE_MILLIS)) + slideOutVertically { -it / 2 } else ExitTransition.None
    ) {
        ServerWakingPill()
    }
}

@Composable
private fun ServerWakingPill() {
    Surface(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.regular, vertical = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(
                text = stringResource(R.string.server_waking),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ServerWakingPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.padding(AppSpacing.regular)) { ServerWakingPill() }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ServerWakingLightPreview() {
    ServerWakingPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ServerWakingDarkPreview() {
    ServerWakingPreviewContent(ThemeMode.DARK)
}
