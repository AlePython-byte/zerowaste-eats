package com.zerowasteeats.app.presentation.screens.merchant.business

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.components.settings.AppearanceSettingCard
import com.zerowasteeats.app.presentation.components.settings.LanguageSettingCard
import com.zerowasteeats.app.presentation.components.settings.SignOutDialog
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

@Composable
fun MerchantBusinessScreen(
    session: Session.Merchant,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: MerchantBusinessViewModel = viewModel(
        factory = MerchantBusinessViewModelFactory(container.merchantRepositories(session).profile)
    )
    val state by viewModel.state.collectAsState()

    MerchantBusinessContent(state = state, onSignOut = onSignOut, modifier = modifier)
}

@Composable
fun MerchantBusinessContent(
    state: MerchantBusinessState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignOutDialogOpen by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.regular),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                ModeBadge()
                Text(
                    text = state.businessName,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                VerificationPill(isVerified = state.isVerified)
                if (!state.isVerified) {
                    // Informative only: an unverified business can publish all the same
                    Text(
                        text = stringResource(R.string.merchant_review_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BusinessDetailsCard(state = state)

            AppearanceSettingCard()
            LanguageSettingCard()

            AppButton(
                text = stringResource(R.string.common_sign_out),
                onClick = { isSignOutDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.SECONDARY
            )
        }
    }

    if (isSignOutDialogOpen) {
        SignOutDialog(
            onConfirm = {
                isSignOutDialogOpen = false
                onSignOut()
            },
            onDismiss = { isSignOutDialogOpen = false }
        )
    }
}

/** Verified or under review, in words as well as colour. */
@Composable
private fun VerificationPill(isVerified: Boolean) {
    val container = if (isVerified) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isVerified) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = container) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            if (isVerified) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = content
                )
            } else {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = content) {}
            }
            Text(
                text = stringResource(
                    if (isVerified) R.string.merchant_verified else R.string.merchant_review_title
                ),
                style = MaterialTheme.typography.labelMedium,
                color = content
            )
        }
    }
}

@Composable
private fun BusinessDetailsCard(state: MerchantBusinessState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.merchant_business_details_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.padding(top = AppSpacing.regular),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
        ) {
            DetailRow(stringResource(R.string.merchant_business_description), state.description)
            DetailRow(stringResource(R.string.merchant_business_address), state.address)
            DetailRow(stringResource(R.string.signup_field_city), state.city)
        }
    }
}

/** A labelled value, or "Sin completar" in a muted tone when there is none yet. */
@Composable
private fun DetailRow(label: String, value: String?) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: stringResource(R.string.merchant_business_missing),
            style = MaterialTheme.typography.bodyLarge,
            color = if (value != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
    }
}

private val previewVerified = MerchantBusinessState(
    businessName = "Panadería Demo",
    isVerified = true,
    description = "Pan y repostería artesanal, hecho cada día.",
    address = "Cra. 27 #18-45",
    city = "Pasto"
)

private val previewNew = MerchantBusinessState(
    businessName = "Panadería Sol",
    isVerified = false,
    description = null,
    address = null,
    city = "Medellín"
)

@Composable
private fun BusinessPreviewContent(themeMode: ThemeMode, state: MerchantBusinessState) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        MerchantBusinessContent(state = state, onSignOut = {})
    }
}

@Preview(name = "Verified light", widthDp = 380, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BusinessVerifiedLightPreview() {
    BusinessPreviewContent(ThemeMode.LIGHT, previewVerified)
}

@Preview(name = "Verified dark", widthDp = 380, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BusinessVerifiedDarkPreview() {
    BusinessPreviewContent(ThemeMode.DARK, previewVerified)
}

@Preview(name = "In review light", widthDp = 380, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BusinessInReviewLightPreview() {
    BusinessPreviewContent(ThemeMode.LIGHT, previewNew)
}

@Preview(name = "In review dark", widthDp = 380, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BusinessInReviewDarkPreview() {
    BusinessPreviewContent(ThemeMode.DARK, previewNew)
}
