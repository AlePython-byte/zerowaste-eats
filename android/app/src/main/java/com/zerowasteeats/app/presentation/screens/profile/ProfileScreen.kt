package com.zerowasteeats.app.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.data.MockProfile
import com.zerowasteeats.app.presentation.model.ProfileUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun ProfileScreen() {
    val profile = MockProfile.getProfile()

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
                bottom = AppSpacing.huge + 80.dp // Padding for GlassBottomBar
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
        ) {
            item {
                ProfileHeader()
            }

            item {
                ProfileHeroCard(profile = profile)
            }

            item {
                ImpactSection(profile = profile)
            }

            item {
                PreferencesSection(categories = profile.favoriteCategories)
            }

            item {
                InfoSection()
            }
        }
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
            text = "Perfil",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = "Tu cuenta e impacto en ZeroWaste Eats.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProfileHeroCard(profile: ProfileUiModel, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
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
                        text = profile.initials,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.regular))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.roleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.city,
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
            text = "Tu impacto",
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
                label = "Pedidos rescatados",
                value = profile.completedOrders.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Comida rescatada",
                value = "${profile.rescuedFoodKg.toString().replace('.', ',')} kg",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.small))
        
        MetricCard(
            label = "Ahorro estimado",
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
    val isDarkTheme = isSystemInDarkTheme()
    
    val containerColor = if (isDarkTheme) GlassTokens.darkSurface else GlassTokens.lightSurface
    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.large),
        color = containerColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor),
        shadowElevation = GlassTokens.elevation / 2
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.regular),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = if (isHighlight) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
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
private fun PreferencesSection(categories: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Preferencias",
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
                ProfilePreferenceChip(text = category)
            }
        }
    }
}

@Composable
private fun ProfilePreferenceChip(text: String, modifier: Modifier = Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val containerColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = 0.5f)
    } else {
        GlassTokens.lightSurface.copy(alpha = 0.5f)
    }
    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = containerColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor)
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
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Sobre tu impacto",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = "Cada pedido rescatado ayuda a aprovechar alimentos que aún están en buen estado y reduce el desperdicio.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}
