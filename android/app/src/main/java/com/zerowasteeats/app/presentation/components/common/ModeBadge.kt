package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** Small pill that tells the person they are in the business space. */
@Composable
fun ModeBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = stringResource(R.string.mode_badge_merchant),
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.extraSmall),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ModeBadgePreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ModeBadge(modifier = Modifier.padding(AppSpacing.regular))
        }
    }
}

@Preview(name = "Light", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ModeBadgeLightPreview() {
    ModeBadgePreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ModeBadgeDarkPreview() {
    ModeBadgePreviewContent(ThemeMode.DARK)
}
