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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zerowasteeats.app.presentation.components.settings.AppearanceSettingCard
import com.zerowasteeats.app.presentation.components.settings.LanguageSettingCard
import com.zerowasteeats.app.presentation.data.MockProfile
import com.zerowasteeats.app.presentation.model.ProfileUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * The customer's profile. Name and email come from the session; the impact numbers are still
 * sample data. Signing out asks first, then [onSignOut] ends the session.
 */
@Composable
fun ProfileScreen(
    displayName: String,
    email: String,
    onSignOut: () -> Unit
) {
    val profile = MockProfile.getProfile()
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
                bottom = AppSpacing.large // Trailing gap; the bottom bar inset comes from the app scaffold
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
                ImpactSection(profile = profile)
            }

            item {
                PreferencesSection(categories = profile.favoriteCategories)
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
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.profile_sign_out_title)) },
        text = { Text(text = stringResource(R.string.profile_sign_out_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.common_sign_out),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        containerColor = SurfaceTokens.surface
    )
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

@Composable
private fun ImpactSection(profile: ProfileUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_impact_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Responsive arrangement: 2 metrics on first row, 1 full width below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            MetricCard(
                label = stringResource(R.string.profile_metric_rescued_orders),
                value = profile.completedOrders.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = stringResource(R.string.profile_metric_rescued_food),
                value = stringResource(R.string.profile_rescued_food_value, profile.rescuedFoodKg),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.small))
        
        MetricCard(
            label = stringResource(R.string.profile_metric_estimated_savings),
            value = profile.estimatedSavings,
            modifier = Modifier.fillMaxWidth(),
            isHighlight = true
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.large),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.regular),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = if (isHighlight) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        ProfileScreen(displayName = "Comprador Demo", email = "comprador@demo.co", onSignOut = {})
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
