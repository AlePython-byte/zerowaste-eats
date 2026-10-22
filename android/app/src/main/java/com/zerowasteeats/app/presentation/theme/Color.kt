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

// Neutral and container roles, so Material components never fall back to the
// default lilac palette. Pairs checked >= 4.5:1 for text, >= 3:1 for outlines.
val RescateSurfaceVariantLight = Color(0xFFEDE5D8)
val RescateOutlineLight = Color(0xFF857A6D)
val RescateOutlineVariantLight = Color(0xFFD9CEBE)
val RescateContainerLowLight = Color(0xFFFBF6EC)
val RescateContainerLight = Color(0xFFF4EDE0)
val RescateContainerHighLight = Color(0xFFEFE7D8)
val RescateContainerHighestLight = Color(0xFFE9E0CF)
val RescateSurfaceDimLight = Color(0xFFE6DDCC)
val RescatePrimaryContainerLight = Color(0xFFFBE3AE)
val RescateOnPrimaryContainerLight = Color(0xFF3D2800)
val RescateTertiaryContainerLight = Color(0xFFF3DFB8)
val RescateErrorContainerLight = Color(0xFFF9D9D2)
val RescateOnErrorContainerLight = Color(0xFF5C1608)

val RescateSurfaceVariantDark = Color(0xFF2A241E)
val RescateOutlineDark = Color(0xFF8F8579)
val RescateOutlineVariantDark = Color(0xFF3A332B)
val RescateContainerLowestDark = Color(0xFF0F0D0B)
val RescateContainerLowDark = Color(0xFF1A1713)
val RescateContainerHighDark = Color(0xFF28231D)
val RescateContainerHighestDark = Color(0xFF332D26)
val RescateSurfaceBrightDark = Color(0xFF3A342D)
val RescatePrimaryContainerDark = Color(0xFF4A3410)
val RescateOnPrimaryContainerDark = Color(0xFFFBE3AE)
val RescateErrorContainerDark = Color(0xFF5C1F14)
val RescateOnErrorContainerDark = Color(0xFFFFDAD3)
