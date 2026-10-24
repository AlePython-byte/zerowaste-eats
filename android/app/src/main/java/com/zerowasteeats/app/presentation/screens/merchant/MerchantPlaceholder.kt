package com.zerowasteeats.app.presentation.screens.merchant

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

// TEMPORARY: stands in for the merchant space until its real navigation replaces it.
@Composable
fun MerchantPlaceholder(
    businessName: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(AppSpacing.extraLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = businessName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = stringResource(R.string.merchant_placeholder_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.section))
            AppButton(
                text = stringResource(R.string.common_sign_out),
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.SECONDARY
            )
        }
    }
}

@Composable
private fun MerchantPlaceholderPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantPlaceholder(businessName = "Panadería Demo", onSignOut = {})
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MerchantPlaceholderLightPreview() {
    MerchantPlaceholderPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantPlaceholderDarkPreview() {
    MerchantPlaceholderPreviewContent(ThemeMode.DARK)
}
