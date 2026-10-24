package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** A number with its label. [isHighlight] makes the number larger, for the main figure. */
@Composable
fun MetricCard(
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

@Composable
private fun MetricCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(AppSpacing.regular)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    MetricCard(label = "Pedidos rescatados", value = "7", modifier = Modifier.weight(1f))
                    MetricCard(label = "Comida rescatada", value = "4,2 kg", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(AppSpacing.small))
                MetricCard(
                    label = "Ahorro estimado",
                    value = "$86.400",
                    modifier = Modifier.fillMaxWidth(),
                    isHighlight = true
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MetricCardLightPreview() {
    MetricCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MetricCardDarkPreview() {
    MetricCardPreviewContent(ThemeMode.DARK)
}
