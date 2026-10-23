package com.zerowasteeats.app.presentation.components.settings

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppCard
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

@Composable
fun LanguageSettingCard(modifier: Modifier = Modifier) {
    val languages = listOf(
        "es" to R.string.language_name_es,
        "en" to R.string.language_name_en
    )
    // Read from the configuration so the selector updates without an activity recreation.
    val current = LocalConfiguration.current.locales[0]?.language ?: "es"

    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_language_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.profile_language_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            languages.forEachIndexed { index, (tag, nameRes) ->
                SegmentedButton(
                    selected = current == tag,
                    onClick = {
                        if (current != tag) {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(tag)
                            )
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, languages.size)
                ) {
                    Text(text = stringResource(nameRes))
                }
            }
        }
    }
}

@Composable
private fun LanguageSettingCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LanguageSettingCard()
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun LanguageSettingCardLightPreview() {
    LanguageSettingCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LanguageSettingCardDarkPreview() {
    LanguageSettingCardPreviewContent(ThemeMode.DARK)
}
