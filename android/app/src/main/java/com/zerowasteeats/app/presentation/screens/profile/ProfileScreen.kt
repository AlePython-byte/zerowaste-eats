package com.zerowasteeats.app.presentation.screens.profile

import com.zerowasteeats.app.presentation.model.OfferCategory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import androidx.compose.ui.Alignment
import android.content.res.Configuration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.MetricCard
import com.zerowasteeats.app.presentation.components.settings.AppearanceSettingCard
import com.zerowasteeats.app.presentation.components.settings.LanguageSettingCard
import com.zerowasteeats.app.presentation.components.settings.SignOutDialog
import com.zerowasteeats.app.presentation.data.MockProfile
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.CatalogLoadState
import com.zerowasteeats.app.data.reservations.CustomerImpact
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.rememberSkeletonAlpha
import com.zerowasteeats.app.presentation.format.formatPesos
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.navigation.LocalBottomBarInset
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * The customer's profile. Name and email come from the session; the impact comes from the
 * server and is loaded again each time the profile shows. Signing out asks first, then
 * [onSignOut] ends the session.
 */
@Composable
fun ProfileScreen(
    session: Session.Customer,
    onSignOut: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: ProfileImpactViewModel = viewModel(
        factory = ProfileImpactViewModelFactory(container.customerReservations(session))
    )
    val impact by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    ProfileContent(
        displayName = session.displayName,
        email = session.email,
        impact = impact,
        onRetryImpact = viewModel::refresh,
        onSignOut = onSignOut
    )
}

@Composable
private fun ProfileContent(
    displayName: String,
    email: String,
    impact: ProfileImpactState,
    onRetryImpact: () -> Unit,
    onSignOut: () -> Unit
) {
    var isSignOutDialogOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = AppSpacing.regular,
                end = AppSpacing.regular,
                top = AppSpacing.regular,
                // Trailing gap, plus the floating bar the list runs under
                bottom = AppSpacing.large + LocalBottomBarInset.current
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
        ) {
            item {
                ProfileHeader()
            }

            item {
                ProfileHeroCard(displayName = displayName, email = email)
            }

            item {
                ImpactSection(state = impact, onRetry = onRetryImpact)
            }

            item {
                PreferencesSection(categories = MockProfile.favoriteCategories)
            }

            item {
                AppearanceSettingCard()
            }

            item {
                LanguageSettingCard()
            }

            item {
                InfoSection()
            }

            item {
                AppButton(
                    text = stringResource(R.string.common_sign_out),
                    onClick = { isSignOutDialogOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = AppButtonStyle.SECONDARY
                )
            }
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

@Composable
private fun ProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.small)
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = stringResource(R.string.profile_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileHeroCard(
    displayName: String,
    email: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initialsOf(displayName),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.regular))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Orders picked up, food rescued and money saved, from the server. Placeholders while the
 * first load runs; a message with a retry if it fails with nothing to show.
 */
@Composable
private fun ImpactSection(
    state: ProfileImpactState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_impact_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))

        val impact = state.impact
        val error = state.loadError
        when {
            impact != null -> ImpactMetrics(impact)
            state.showError && error != null -> ImpactLoadError(error = error, onRetry = onRetry)
            else -> ImpactSkeleton()
        }
    }
}

@Composable
private fun ImpactMetrics(impact: CustomerImpact) {
    // Responsive arrangement: 2 metrics on first row, 1 full width below
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        MetricCard(
            label = stringResource(R.string.profile_metric_completed_orders),
            value = impact.completedOrders.toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = stringResource(R.string.profile_metric_rescued_food),
            value = stringResource(
                if (impact.foodIsEstimate) R.string.profile_rescued_food_value_approx
                else R.string.profile_rescued_food_value,
                impact.foodRescuedKg
            ),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(AppSpacing.small))

    MetricCard(
        label = stringResource(R.string.profile_metric_savings),
        value = formatPesos(impact.moneySaved),
        modifier = Modifier.fillMaxWidth(),
        isHighlight = true
    )
}

/** The shape of the three figures while they load. Screen readers hear that they load. */
@Composable
private fun ImpactSkeleton() {
    val alpha = rememberSkeletonAlpha()
    val block = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(AppRadius.large)
    val loading = stringResource(R.string.profile_impact_loading)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = loading }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
                .alpha(alpha),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Box(Modifier.weight(1f).height(92.dp).clip(shape).background(block))
            Box(Modifier.weight(1f).height(92.dp).clip(shape).background(block))
        }
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clearAndSetSemantics {}
                .alpha(alpha)
                .clip(shape)
                .background(block)
        )
    }
}

@Composable
private fun ImpactLoadError(error: CatalogError, onRetry: () -> Unit) {
    val message = when (error) {
        CatalogError.NETWORK -> stringResource(R.string.profile_impact_load_failed_network)
        CatalogError.SESSION -> stringResource(R.string.catalog_session_failed)
        CatalogError.UNEXPECTED -> stringResource(R.string.profile_impact_load_failed)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Trying again does not help an ended session: only signing in again does
        if (error != CatalogError.SESSION) {
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            AppButton(
                text = stringResource(R.string.auth_retry),
                onClick = onRetry,
                style = AppButtonStyle.SECONDARY
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesSection(categories: List<OfferCategory>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_preferences_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                ProfilePreferenceChip(text = stringResource(category.labelRes))
            }
        }
    }
}

@Composable
private fun ProfilePreferenceChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = AppSpacing.large, vertical = AppSpacing.small),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoSection(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_about_impact_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = stringResource(R.string.profile_about_impact_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/** Up to two letters for the avatar: the first of the first and last words of [name]. */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return ""
    val first = words.first().first().toString()
    val last = if (words.size > 1) words.last().first().toString() else ""
    return (first + last).uppercase()
}

@Composable
private fun ProfileScreenPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        ProfileContent(
            displayName = "Comprador Demo",
            email = "comprador@demo.co",
            impact = ProfileImpactState(
                impact = CustomerImpact(completedOrders = 3, moneySaved = 41_300, foodRescuedKg = 2.0),
                loadState = CatalogLoadState.Loaded
            ),
            onRetryImpact = {},
            onSignOut = {}
        )
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 1600, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ProfileScreenLightPreview() {
    ProfileScreenPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 1600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenDarkPreview() {
    ProfileScreenPreviewContent(ThemeMode.DARK)
}
