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

    // Butter as a fill with carbon text (selected segment, chips)
    primaryContainer = RescatePrimaryContainerLight,
    onPrimaryContainer = RescateOnPrimaryContainerLight,
    secondaryContainer = RescateButter,
    onSecondaryContainer = RescateCarbon,
    tertiaryContainer = RescateTertiaryContainerLight,
    onTertiaryContainer = RescateOnPrimaryContainerLight,

    surface = RescateSurfaceLight,
    onSurface = RescateCarbon,
    surfaceVariant = RescateSurfaceVariantLight,
    onSurfaceVariant = RescateTextMutedLight,
    surfaceTint = RescateButter,
    surfaceBright = RescateSurfaceLight,
    surfaceDim = RescateSurfaceDimLight,
    surfaceContainerLowest = RescateSurfaceLight,
    surfaceContainerLow = RescateContainerLowLight,
    surfaceContainer = RescateContainerLight,
    surfaceContainerHigh = RescateContainerHighLight,
    surfaceContainerHighest = RescateContainerHighestLight,

    outline = RescateOutlineLight,
    outlineVariant = RescateOutlineVariantLight,
    scrim = RescateCarbonOverlay,

    inverseSurface = RescateCarbon,
    inverseOnSurface = RescateCream,
    inversePrimary = RescateButter,

    error = RescateCoralOnLight,
    onError = RescateCream,
    errorContainer = RescateErrorContainerLight,
    onErrorContainer = RescateOnErrorContainerLight
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

    primaryContainer = RescatePrimaryContainerDark,
    onPrimaryContainer = RescateOnPrimaryContainerDark,
    secondaryContainer = RescateButter,
    onSecondaryContainer = RescateCarbon,
    tertiaryContainer = RescatePrimaryContainerDark,
    onTertiaryContainer = RescateOnPrimaryContainerDark,

    surface = RescateCarbonElevated,
    onSurface = RescateCream,
    surfaceVariant = RescateSurfaceVariantDark,
    onSurfaceVariant = RescateCreamMuted,
    surfaceTint = RescateButter,
    surfaceBright = RescateSurfaceBrightDark,
    surfaceDim = RescateCarbon,
    surfaceContainerLowest = RescateContainerLowestDark,
    surfaceContainerLow = RescateContainerLowDark,
    surfaceContainer = RescateCarbonElevated,
    surfaceContainerHigh = RescateContainerHighDark,
    surfaceContainerHighest = RescateContainerHighestDark,

    outline = RescateOutlineDark,
    outlineVariant = RescateOutlineVariantDark,
    scrim = RescateCarbonOverlay,

    inverseSurface = RescateCream,
    inverseOnSurface = RescateCarbon,
    inversePrimary = RescateButterOnLight,

    error = RescateCoral,
    onError = RescateCarbon,
    errorContainer = RescateErrorContainerDark,
    onErrorContainer = RescateOnErrorContainerDark
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

        // Dark icons on the light theme and light icons on the dark one, in both bars,
        // whatever the system theme is
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZeroWasteTypography,
        shapes = ZeroWasteShapes
    ) {
        CompositionLocalProvider(LocalThemeMode provides themeMode, content = content)
    }
}
