package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.SurfaceTokens

/** Two-option segmented control (list or grid). */
@Composable
fun ViewModeSelector(
    selected: ExploreViewMode,
    onSelect: (ExploreViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val groupDescription = stringResource(R.string.explore_view_mode_cd)

    Surface(
        modifier = modifier.semantics { contentDescription = groupDescription },
        shape = RoundedCornerShape(50),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Row(
            modifier = Modifier
                .padding(2.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExploreViewMode.entries.forEach { mode ->
                ViewModeSegment(
                    text = stringResource(
                        when (mode) {
                            ExploreViewMode.LIST -> R.string.explore_view_mode_list
                            ExploreViewMode.GRID -> R.string.explore_view_mode_grid
                        }
                    ),
                    isSelected = mode == selected,
                    onClick = {
                        if (mode != selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(mode)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewModeSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val colorSpec = if (animationsEnabled) tween<Color>(150) else snap()
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = colorSpec,
        label = "viewModeSegmentBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = colorSpec,
        label = "viewModeSegmentContent"
    )

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}
