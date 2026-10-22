package com.zerowasteeats.app.presentation.components.filters

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.model.OfferFilters
import com.zerowasteeats.app.presentation.model.OfferSort
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import kotlinx.coroutines.launch

/**
 * Bottom sheet to edit [draft]. [onApply] runs after the sheet has finished closing;
 * dragging it down, tapping outside or going back calls [onDismiss] and leaves the draft out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFilterSheet(
    draft: OfferFilters,
    onDraftChange: (OfferFilters) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Hiding the sheet to apply must not be mistaken for a dismissal that discards the draft
    val isApplying = remember { booleanArrayOf(false) }

    ModalBottomSheet(
        onDismissRequest = { if (!isApplying[0]) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) } // the content pads for the nav bar
    ) {
        FilterSheetContent(
            draft = draft,
            onDraftChange = onDraftChange,
            onApply = {
                isApplying[0] = true
                scope.launch { sheetState.hide() }.invokeOnCompletion { onApply() }
            },
            onClear = onClear
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheetContent(
    draft: OfferFilters,
    onDraftChange: (OfferFilters) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.regular),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)
    ) {
        Text(
            text = stringResource(R.string.filter_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        SectionTitle(stringResource(R.string.filter_sheet_category))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            OfferCategory.entries.forEach { category ->
                val isSelected = draft.category == category
                CategoryChip(
                    text = stringResource(category.labelRes),
                    isSelected = isSelected,
                    onClick = { onDraftChange(draft.copy(category = category)) },
                    modifier = Modifier.semantics { selected = isSelected }
                )
            }
        }

        SectionTitle(stringResource(R.string.explore_sort_by))
        Column {
            OfferSort.entries.forEach { sort ->
                val isSelected = draft.sort == sort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onDraftChange(draft.copy(sort = sort)) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null, // the whole row is the target
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.tertiary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(sort.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = AppSpacing.medium)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            AppButton(
                text = stringResource(R.string.filter_sheet_clear),
                onClick = onClear,
                style = AppButtonStyle.SECONDARY,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = stringResource(R.string.filter_sheet_apply),
                onClick = onApply,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun FilterSheetPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FilterSheetContent(
                draft = OfferFilters(OfferCategory.BAKERY, OfferSort.PRICE),
                onDraftChange = {},
                onApply = {},
                onClear = {}
            )
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun FilterSheetLightPreview() {
    FilterSheetPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterSheetDarkPreview() {
    FilterSheetPreviewContent(ThemeMode.DARK)
}
