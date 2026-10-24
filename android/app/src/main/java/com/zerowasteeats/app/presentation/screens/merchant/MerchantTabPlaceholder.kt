package com.zerowasteeats.app.presentation.screens.merchant

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

// TEMPORARY: each merchant tab is this empty screen until its real screen replaces it.
@Composable
fun MerchantTabPlaceholder(
    title: String,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    // TEMPORARY: the business tab shows the sign out button here until the real profile exists.
    signOut: (@Composable () -> Unit)? = null
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.regular)
        ) {
            ModeBadge()
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
                ) {
                    EmptyState(title = emptyTitle, message = emptyMessage)
                    signOut?.invoke()
                }
            }
        }
    }
}

@Composable
private fun MerchantTabPlaceholderPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantTabPlaceholder(
            title = "Mis ofertas",
            emptyTitle = "Aún no tienes ofertas",
            emptyMessage = "Publica tu primera oferta para que otros puedan rescatarla.",
            signOut = {
                AppButton(text = "Cerrar sesión", onClick = {}, style = AppButtonStyle.SECONDARY)
            }
        )
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MerchantTabPlaceholderLightPreview() {
    MerchantTabPlaceholderPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantTabPlaceholderDarkPreview() {
    MerchantTabPlaceholderPreviewContent(ThemeMode.DARK)
}
