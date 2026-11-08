package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * A small pill that says the app runs on demo data, so it is never mistaken for the real
 * thing. It sits at the top of every signed-in screen, slightly see-through and out of the
 * way of the controls in the corners; it rises in once, unless animations are off.
 */
@Composable
fun DemoModeBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.demo_badge_cd)
    Surface(
        modifier = modifier
            .entranceRise(rise = (-6).dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
            Text(
                text = stringResource(R.string.demo_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DemoModeBadgePreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DemoModeBadge(modifier = Modifier.padding(AppSpacing.regular))
        }
    }
}

@Preview(name = "Light", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DemoModeBadgeLightPreview() {
    DemoModeBadgePreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DemoModeBadgeDarkPreview() {
    DemoModeBadgePreviewContent(ThemeMode.DARK)
}
