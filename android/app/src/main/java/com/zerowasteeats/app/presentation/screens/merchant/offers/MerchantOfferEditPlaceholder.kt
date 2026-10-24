package com.zerowasteeats.app.presentation.screens.merchant.offers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

// TEMPORARY: stands in for the offer editor until the next commit replaces it.
@Composable
fun MerchantOfferEditPlaceholder(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(AppSpacing.regular),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(
                title = stringResource(R.string.merchant_offer_editor_soon_title),
                message = stringResource(R.string.merchant_offer_editor_soon_message)
            )
            AppButton(
                text = stringResource(R.string.common_back),
                onClick = onBack,
                style = AppButtonStyle.SECONDARY
            )
        }
    }
}

@Composable
private fun MerchantOfferEditPlaceholderPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantOfferEditPlaceholder(onBack = {})
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MerchantOfferEditPlaceholderLightPreview() {
    MerchantOfferEditPlaceholderPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MerchantOfferEditPlaceholderDarkPreview() {
    MerchantOfferEditPlaceholderPreviewContent(ThemeMode.DARK)
}
