package com.zerowasteeats.app.presentation.screens.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.theme.AppSpacing

@Composable
fun GlassPlaygroundScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4F7F3),
                        Color(0xFFE5F1E9),
                        Color(0xFFD5E7DB)
                    )
                )
            )
    ) {
        DecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AppSpacing.extraLarge,
                    vertical = AppSpacing.screen
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ZeroWaste Eats",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(AppSpacing.small)
            )

            Text(
                text = "Rescata comida. Ahorra más. Desperdicia menos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.65f
                )
            )

            Spacer(
                modifier = Modifier.height(AppSpacing.extraLarge)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Oferta destacada",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(AppSpacing.medium)
                )

                Text(
                    text = "Croissants del día",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(
                    modifier = Modifier.height(AppSpacing.extraSmall)
                )

                Text(
                    text = "Panadería artesanal · Recoge antes de las 7:30 p. m.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.65f
                    )
                )

                Spacer(
                    modifier = Modifier.height(AppSpacing.large)
                )

                Text(
                    text = "$8.900",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Antes $15.000 · 40% menos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.65f
                    )
                )
            }
        }
    }
}

@Composable
private fun DecorativeBackground() {
    Box(
        modifier = Modifier
            .padding(start = 220.dp, top = 120.dp)
            .clip(CircleShape)
            .background(
                Color(0xFFA8D672).copy(alpha = 0.45f)
            )
            .fillMaxWidth(0.55f)
            .height(210.dp)
    )

    Box(
        modifier = Modifier
            .padding(start = 12.dp, top = 520.dp)
            .clip(
                RoundedCornerShape(100.dp)
            )
            .background(
                Color(0xFF247A52).copy(alpha = 0.18f)
            )
            .fillMaxWidth(0.55f)
            .height(170.dp)
    )
}