package com.zerowasteeats.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zerowasteeats.app.presentation.navigation.ZeroWasteEatsApp
import com.zerowasteeats.app.presentation.theme.ThemePreferences
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // First launch: use Spanish instead of the device language.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }

        // Before super.onCreate: swaps the launch theme for the app's. No keep-on-screen
        // condition, so the splash goes away as soon as the first frame is ready.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ZeroWasteEatsTheme(themeMode = ThemePreferences.mode) {
                ZeroWasteEatsApp()
            }
        }
    }
}