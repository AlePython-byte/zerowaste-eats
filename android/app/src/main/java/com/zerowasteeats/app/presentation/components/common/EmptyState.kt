package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * Placeholder for a list or screen with nothing to show. The icon is decorative; [actionText]
 * and [onActionClick] add an optional button underneath.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_empty_bag,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyStateIcon(icon)
        Spacer(modifier = Modifier.height(AppSpacing.regular))
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(AppSpacing.large))
            AppButton(text = actionText, onClick = onActionClick)
        }
    }
}

/** The drawing at the top of an empty state: decorative, in the theme's secondary text color. */
@Composable
fun EmptyStateIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyStatePreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                title = "Aún no tienes ofertas",
                message = "Publica tu primera oferta para que otros puedan rescatarla.",
                actionText = "Publicar oferta",
                onActionClick = {}
            )
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun EmptyStateLightPreview() {
    EmptyStatePreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStateDarkPreview() {
    EmptyStatePreviewContent(ThemeMode.DARK)
}
