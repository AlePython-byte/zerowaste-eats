package com.zerowasteeats.app.presentation.components.filters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.SurfaceTokens

/** Opens the filter sheet. Shows a badge with [activeCount] while there are active filters. */
@Composable
fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (activeCount > 0) {
        pluralStringResource(R.plurals.filter_button_active_cd, activeCount, activeCount)
    } else {
        stringResource(R.string.filter_button_cd)
    }
    val animationsEnabled = rememberAnimationsEnabled()
    // Keeps the last count on screen while the badge animates out
    var shownCount by remember { mutableIntStateOf(activeCount) }
    LaunchedEffect(activeCount) {
        if (activeCount > 0) shownCount = activeCount
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(AppRadius.large),
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Box(
            modifier = Modifier.clearAndSetSemantics {},
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                contentDescription = null, // described by the button
                tint = MaterialTheme.colorScheme.onSurface
            )
            AnimatedVisibility(
                visible = activeCount > 0,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
                enter = if (animationsEnabled) scaleIn() + fadeIn() else EnterTransition.None,
                exit = if (animationsEnabled) scaleOut() + fadeOut() else ExitTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shownCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
