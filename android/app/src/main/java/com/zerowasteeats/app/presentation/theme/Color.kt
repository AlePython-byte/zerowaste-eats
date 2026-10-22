package com.zerowasteeats.app.presentation.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Rescate identity (prototype)
// Dark-first: carbon background, warm cream text, butter accent, coral urgency.
// ---------------------------------------------------------------------------
val RescateCarbon = Color(0xFF14120F)
val RescateCarbonElevated = Color(0xFF1E1A16)
val RescateCarbonOverlay = Color(0xFF0B0A08)
val RescateCream = Color(0xFFF7F1E8)
val RescateCreamMuted = Color(0xFFB9AFA2)
val RescateButter = Color(0xFFF2B84B)
val RescateCoral = Color(0xFFE8553D)

// Rescate light mode (default). Contrast ratios (WCAG):
//   Carbon on Cream 16.7, Carbon on Surface 18.3, Carbon on Butter 10.5,
//   TextMuted on Cream 5.3, CoralOnLight on Cream 4.7 (plain coral is 3.2).
val RescateCreamBackground = Color(0xFFF7F1E8)
val RescateSurfaceLight = Color(0xFFFFFCF7)
val RescateTextMutedLight = Color(0xFF6B6258)
val RescateCoralOnLight = Color(0xFFC23B26)
// Amber for prices/accent text on light surfaces (5.9 on Cream; plain Butter is 1.6).
val RescateButterOnLight = Color(0xFF805400)

// ---------------------------------------------------------------------------
// Legacy Editorial Glass tokens (kept so untouched screens still compile.
// Remove once every screen is migrated to Rescate.)
// ---------------------------------------------------------------------------
val EcoGreen = Color(0xFF247A52)
val EcoGreenDark = Color(0xFF185A3B)
val EcoLime = Color(0xFFA8D672)

val BackgroundLight = Color(0xFFF4F7F3)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFE9EFEB)

val GlassLight = Color(0xCCFFFFFF)
val GlassLightStrong = Color(0xE6FFFFFF)
val GlassBorderLight = Color(0x66FFFFFF)

val TextPrimaryLight = Color(0xFF151A17)
val TextSecondaryLight = Color(0xFF68716B)
val TextOnPrimary = Color(0xFFFFFFFF)

// Status (success stays green on purpose: "Reservado", "Recogido")
val SuccessGreen = Color(0xFF2E7D4F)
val WarningAmber = Color(0xFFF2A93B)
val ErrorRed = Color(0xFFBA3A3A)

val BackgroundDark = Color(0xFF101512)
val SurfaceDark = Color(0xFF171D19)
val GlassDark = Color(0x991D2520)
val GlassBorderDark = Color(0x33FFFFFF)

val TextPrimaryDark = Color(0xFFF2F5F3)
val TextSecondaryDark = Color(0xFFADB7B0)
