package com.zerowasteeats.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zerowasteeats.app.R

// Headings and prices: static cuts, one file per weight.
val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold)
)

// Body and labels: a single variable font, one entry per weight in use.
private fun dmSans(weight: FontWeight) = Font(
    resId = R.font.dm_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val DmSans = FontFamily(
    dmSans(FontWeight.Normal),
    dmSans(FontWeight.Medium),
    dmSans(FontWeight.SemiBold),
    dmSans(FontWeight.Bold)
)

private fun heading(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = BricolageGrotesque,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

private fun text(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = DmSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

// Sizes follow the previous scale; roles that were never customised keep the
// Material 3 default size and line height.
val ZeroWasteTypography = Typography(
    displayLarge = heading(FontWeight.ExtraBold, 32, 38),
    displayMedium = heading(FontWeight.ExtraBold, 45, 52),
    displaySmall = heading(FontWeight.ExtraBold, 36, 44),

    headlineLarge = heading(FontWeight.Bold, 26, 32),
    headlineMedium = heading(FontWeight.Bold, 28, 36),
    headlineSmall = heading(FontWeight.Bold, 24, 32),

    titleLarge = heading(FontWeight.SemiBold, 20, 26),
    titleMedium = heading(FontWeight.SemiBold, 16, 24),
    titleSmall = heading(FontWeight.SemiBold, 14, 20),

    bodyLarge = text(FontWeight.Normal, 16, 24),
    bodyMedium = text(FontWeight.Normal, 14, 20),
    bodySmall = text(FontWeight.Normal, 12, 16),

    labelLarge = text(FontWeight.Medium, 13, 18),
    labelMedium = text(FontWeight.Medium, 12, 16),
    labelSmall = text(FontWeight.SemiBold, 12, 16)
)
