package com.zerowasteeats.app.presentation.components.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/** One option of [SegmentedCountControl]: its label and the count next to it. */
data class SegmentOption(val label: String, val count: Int)

/**
 * Two or more tabs in one pill, like the orders selector, each with a small count. Screen
 * readers announce each option as a tab and which one is selected.
 */
@Composable
fun SegmentedCountControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppRadius.extraLarge))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(AppSpacing.extraSmall),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(AppRadius.large),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {}
                    }
                    SegmentLabel(option = option, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun SegmentLabel(option: SegmentOption, isSelected: Boolean) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
        Surface(
            shape = CircleShape,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = option.count.toString(),
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp)
                    .padding(horizontal = AppSpacing.small, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SegmentedCountControlPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SegmentedCountControl(
                options = listOf(SegmentOption("Pendientes", 2), SegmentOption("Completadas", 1)),
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.padding(AppSpacing.regular)
            )
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SegmentedCountControlLightPreview() {
    SegmentedCountControlPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SegmentedCountControlDarkPreview() {
    SegmentedCountControlPreviewContent(ThemeMode.DARK)
}
