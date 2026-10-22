package com.zerowasteeats.app.presentation.components.filters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.OfferSort
import com.zerowasteeats.app.presentation.theme.AppSpacing

/**
 * One removable chip per active filter (category and sort), shown under the search bar.
 * The row slides in when the first filter is applied and out when the last one is removed.
 */
@Composable
fun ActiveFilterChips(
    filters: OfferFilters,
    onRemoveCategory: () -> Unit,
    onRemoveSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val hasCategory = filters.category != OfferCategory.ALL
    val hasSort = filters.sort != OfferSort.DISTANCE

    AnimatedVisibility(
        visible = filters.activeCount > 0,
        modifier = modifier,
        enter = if (animationsEnabled) expandVertically() + fadeIn() else EnterTransition.None,
        exit = if (animationsEnabled) shrinkVertically() + fadeOut() else ExitTransition.None
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            contentPadding = PaddingValues(vertical = AppSpacing.extraSmall)
        ) {
            if (hasCategory) {
                item(key = "category") {
                    RemovableFilterChip(
                        label = stringResource(filters.category.labelRes),
                        onRemove = onRemoveCategory
                    )
                }
            }
            if (hasSort) {
                item(key = "sort") {
                    RemovableFilterChip(
                        label = stringResource(R.string.filter_chip_sort, stringResource(filters.sort.labelRes)),
                        onRemove = onRemoveSort
                    )
                }
            }
        }
    }
}

@Composable
private fun RemovableFilterChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val removeDescription = stringResource(R.string.filter_chip_remove_cd, label)

    // Surface(onClick) already keeps a 48dp touch area around the 36dp chip
    Surface(
        onClick = onRemove,
        modifier = modifier.semantics { contentDescription = removeDescription },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(start = AppSpacing.medium, end = AppSpacing.small)
                .padding(vertical = AppSpacing.small)
                .clearAndSetSemantics {}, // described by the chip
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(AppSpacing.small))
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
