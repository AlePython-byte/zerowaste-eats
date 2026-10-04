package com.zerowasteeats.app.presentation.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.navigation.AppDestination
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onNavigateTo: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.9f)
    } else {
        GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.95f)
    }

    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.regular),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = backgroundColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor),
        shadowElevation = GlassTokens.elevation / 1.5f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.small, horizontal = AppSpacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val destinations = AppDestination.entries
            
            destinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                
                GlassBottomBarItem(
                    destination = destination,
                    isSelected = isSelected,
                    onClick = { onNavigateTo(destination) }
                )
            }
        }
    }
}

@Composable
private fun GlassBottomBarItem(
    destination: AppDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.large))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp, minWidth = 64.dp)
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtle selected indicator (small dot)
            Surface(
                modifier = Modifier.size(4.dp),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            ) {}
        }
    }
}
