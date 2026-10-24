package com.zerowasteeats.app.presentation.components.settings

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * ES/EN switch that fits in a corner. Same behavior as [LanguageSettingCard]; each option is
 * a 48 dp radio button announced with the full language name.
 */
@Composable
fun CompactLanguageSelector(modifier: Modifier = Modifier) {
    val current = currentLanguageTag()

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceTokens.surface,
        border = BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    ) {
        Row(
            modifier = Modifier
                .padding(AppSpacing.extraSmall)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLanguage.entries.forEach { language ->
                val selected = current == language.tag
                val name = stringResource(language.nameRes)
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier
                        )
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { if (!selected) changeLanguage(language.tag) }
                        )
                        .semantics { contentDescription = name }
                        .padding(horizontal = AppSpacing.regular),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(language.codeRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactLanguageSelectorPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            CompactLanguageSelector(modifier = Modifier.padding(AppSpacing.regular))
        }
    }
}

@Preview(name = "Light", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun CompactLanguageSelectorLightPreview() {
    CompactLanguageSelectorPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompactLanguageSelectorDarkPreview() {
    CompactLanguageSelectorPreviewContent(ThemeMode.DARK)
}
