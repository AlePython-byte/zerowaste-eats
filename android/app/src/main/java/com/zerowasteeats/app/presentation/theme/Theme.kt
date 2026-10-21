package com.zerowasteeats.app.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The user picks the mode; the system setting is intentionally ignored.
enum class ThemeMode { LIGHT, DARK }

private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.LIGHT }

// Replaces isSystemInDarkTheme() so components follow the user's choice.
@Composable
fun isAppInDarkTheme(): Boolean = LocalThemeMode.current == ThemeMode.DARK

private val RescateLightColorScheme = lightColorScheme(
    primary = RescateButter,
    onPrimary = RescateCarbon,

    secondary = RescateCoralOnLight,
    onSecondary = RescateCream,

    // tertiary = accent for text (prices, pickup time)
    tertiary = RescateButterOnLight,
    onTertiary = RescateCream,

    background = RescateCreamBackground,
    onBackground = RescateCarbon,

    surface = RescateSurfaceLight,
    onSurface = RescateCarbon,
    surfaceVariant = RescateSurfaceLight,
    onSurfaceVariant = RescateTextMutedLight,

    error = RescateCoralOnLight,
    onError = RescateCream
)

private val RescateDarkColorScheme = darkColorScheme(
    primary = RescateButter,
    onPrimary = RescateCarbon,

    secondary = RescateCoral,
    onSecondary = RescateCarbon,

    tertiary = RescateButter,
    onTertiary = RescateCarbon,

    background = RescateCarbon,
    onBackground = RescateCream,

    surface = RescateCarbonElevated,
    onSurface = RescateCream,
    surfaceVariant = RescateCarbonElevated,
    onSurfaceVariant = RescateCreamMuted,

    error = RescateCoral,
    onError = RescateCarbon
)

@Composable
fun ZeroWasteEatsTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = themeMode == ThemeMode.DARK
    val colorScheme = if (isDark) RescateDarkColorScheme else RescateLightColorScheme

    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window

        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()

        WindowCompat
            .getInsetsController(window, view)
            .isAppearanceLightStatusBars = !isDark
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZeroWasteTypography,
        shapes = ZeroWasteShapes
    ) {
        CompositionLocalProvider(LocalThemeMode provides themeMode, content = content)
    }
}
