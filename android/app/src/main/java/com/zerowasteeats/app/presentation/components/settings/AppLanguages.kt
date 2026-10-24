package com.zerowasteeats.app.presentation.components.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.zerowasteeats.app.R

/** A language the app is translated into. [tag] is its BCP 47 language tag. */
internal enum class AppLanguage(
    val tag: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val codeRes: Int
) {
    SPANISH("es", R.string.language_name_es, R.string.language_code_es),
    ENGLISH("en", R.string.language_name_en, R.string.language_code_en)
}

/** Tag of the language in use. Read from the configuration so it updates without a recreation. */
@Composable
internal fun currentLanguageTag(): String =
    LocalConfiguration.current.locales[0]?.language ?: AppLanguage.SPANISH.tag

internal fun changeLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
