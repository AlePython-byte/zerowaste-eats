package com.zerowasteeats.app.presentation.screens.merchant.editor

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.zerowasteeats.app.R
import com.zerowasteeats.app.data.ai.SuggestionError
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.components.common.FormErrorBanner
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.screens.auth.AuthLinkButton
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

private const val REVEAL_MILLIS = 200

/** Asking for a suggestion from the merchant's photo, and what came of it. */
class SuggestionPanel(
    val state: SuggestionUiState = SuggestionUiState.Idle,
    /** Also the retry: the same photo and the current original price. */
    val onSuggest: () -> Unit = {}
)

/**
 * The "suggest with AI" button and, under it, the outcome: a quiet note while it works, what
 * was filled (with the server's warnings) or why it failed, with a retry. The form itself is
 * never touched by a failure.
 */
@Composable
fun SuggestionSection(panel: SuggestionPanel, modifier: Modifier = Modifier) {
    val state = panel.state
    val isLoading = state == SuggestionUiState.Loading
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        AppButton(
            text = stringResource(R.string.ai_suggest_button),
            onClick = panel.onSuggest,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.SECONDARY,
            isLoading = isLoading
        )
        Text(
            text = stringResource(
                if (isLoading) R.string.ai_suggest_loading else R.string.ai_suggest_hint
            ),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Reveal(visible = state is SuggestionUiState.Applied) {
            (state as? SuggestionUiState.Applied)?.let { AppliedCard(it) }
        }
        Reveal(visible = state is SuggestionUiState.Failed) {
            (state as? SuggestionUiState.Failed)?.let { failed ->
                Column {
                    FormErrorBanner(message = failed.error.message())
                    // A refused photo needs another photo, not the same request again
                    if (failed.error != SuggestionError.INVALID_PHOTO) {
                        AuthLinkButton(
                            text = stringResource(R.string.auth_retry),
                            onClick = panel.onSuggest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    val animate = rememberAnimationsEnabled()
    AnimatedVisibility(
        visible = visible,
        enter = if (animate) {
            expandVertically(tween(REVEAL_MILLIS)) + fadeIn(tween(REVEAL_MILLIS))
        } else {
            EnterTransition.None
        },
        exit = if (animate) {
            shrinkVertically(tween(REVEAL_MILLIS)) + fadeOut(tween(REVEAL_MILLIS))
        } else {
            ExitTransition.None
        }
    ) { content() }
}

@Composable
private fun AppliedCard(state: SuggestionUiState.Applied) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Text(
                text = stringResource(R.string.ai_suggestion_applied_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (state.isExample) {
                Pill(text = stringResource(R.string.ai_suggestion_example))
            }
        }
        Text(
            text = stringResource(R.string.ai_suggestion_applied),
            modifier = Modifier
                .padding(top = AppSpacing.extraSmall)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!state.priceFilled) {
            Text(
                text = stringResource(R.string.ai_suggestion_discount_hint, state.discountPercent),
                modifier = Modifier.padding(top = AppSpacing.small),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        state.warnings.forEach { warning ->
            Text(
                text = "• $warning",
                modifier = Modifier.padding(top = AppSpacing.small),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** The "suggested by AI" mark next to a field the suggestion filled; gone once edited. */
@Composable
fun SuggestedByAiChip(visible: Boolean, modifier: Modifier = Modifier) {
    val animate = rememberAnimationsEnabled()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animate) fadeIn(tween(REVEAL_MILLIS)) else EnterTransition.None,
        exit = if (animate) fadeOut(tween(REVEAL_MILLIS)) else ExitTransition.None
    ) {
        Pill(text = stringResource(R.string.ai_suggested_chip))
    }
}

@Composable
private fun Pill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.extraSmall),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SuggestionError.message(): String = stringResource(
    when (this) {
        SuggestionError.INVALID_PHOTO -> R.string.ai_error_invalid_photo
        SuggestionError.LIMIT_REACHED -> R.string.ai_error_limit
        SuggestionError.UNAVAILABLE -> R.string.ai_error_unavailable
        SuggestionError.NETWORK -> R.string.auth_error_network
        SuggestionError.UNEXPECTED -> R.string.ai_error_unexpected
    }
)

@Composable
private fun SuggestionPreview(themeMode: ThemeMode, state: SuggestionUiState) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SuggestionSection(
                panel = SuggestionPanel(state = state),
                modifier = Modifier.padding(AppSpacing.regular)
            )
        }
    }
}

@Preview(name = "Applied light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SuggestionAppliedLightPreview() {
    SuggestionPreview(
        ThemeMode.LIGHT,
        SuggestionUiState.Applied(
            isExample = true,
            warnings = listOf("La foto no parece mostrar comida."),
            discountPercent = 50,
            priceFilled = false
        )
    )
}

@Preview(name = "Failed dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuggestionFailedDarkPreview() {
    SuggestionPreview(ThemeMode.DARK, SuggestionUiState.Failed(SuggestionError.UNAVAILABLE))
}
