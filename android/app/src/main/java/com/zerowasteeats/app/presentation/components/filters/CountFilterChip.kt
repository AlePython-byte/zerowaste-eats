package com.zerowasteeats.app.presentation.components.filters

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * Filter chip like [CategoryChip] with a small count next to the label. One option of a radio
 * group: put the chips in a row with [Modifier.selectableGroup].
 */
@Composable
fun CountFilterChip(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppRadius.extraLarge)
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else SurfaceTokens.surface,
        border = if (isSelected) null else BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.regular),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
            // Small count pill; on the butter chip it darkens so the number stays readable
            Surface(
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CountFilterChipPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .padding(AppSpacing.regular)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                CountFilterChip(text = "Activas", count = 2, isSelected = true, onClick = {})
                CountFilterChip(text = "Pausadas", count = 1, isSelected = false, onClick = {})
                CountFilterChip(text = "Agotadas", count = 0, isSelected = false, onClick = {})
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CountFilterChipLightPreview() {
    CountFilterChipPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CountFilterChipDarkPreview() {
    CountFilterChipPreviewContent(ThemeMode.DARK)
}
