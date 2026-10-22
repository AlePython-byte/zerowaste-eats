package com.zerowasteeats.app.presentation.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.navigation.AppDestination
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigateTo: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.regular),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border),
        shadowElevation = SurfaceTokens.barElevation
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
                
                AppBottomBarItem(
                    destination = destination,
                    isSelected = isSelected,
                    onClick = { onNavigateTo(destination) }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBarItem(
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
            val animationsEnabled = rememberAnimationsEnabled()
            val colorSpec: AnimationSpec<Color> = if (animationsEnabled) tween(200) else snap()
            val dotSpec: AnimationSpec<Float> = if (animationsEnabled) tween(200) else snap()

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = colorSpec,
                label = "bottomBarLabelColor"
            )
            // 0 -> 1 grows and fades the dot in; the dot always occupies its 4dp slot
            val dotProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = dotSpec,
                label = "bottomBarDot"
            )

            Text(
                text = stringResource(destination.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtle selected indicator (small dot)
            Surface(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer {
                        alpha = dotProgress
                        scaleX = dotProgress
                        scaleY = dotProgress
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary
            ) {}
        }
    }
}
