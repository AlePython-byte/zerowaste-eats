package com.zerowasteeats.app.presentation.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// SharedPreferences is enough for a single enum flag; the in-memory state
// lets Compose recompose the theme as soon as the user flips the switch.
object ThemePreferences {
    private const val FILE = "theme_prefs"
    private const val KEY_MODE = "theme_mode"

    var mode by mutableStateOf(ThemeMode.LIGHT)
        private set

    fun load(context: Context) {
        val saved = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_MODE, null)
        mode = ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.LIGHT
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, newMode.name)
            .apply()
    }
}
