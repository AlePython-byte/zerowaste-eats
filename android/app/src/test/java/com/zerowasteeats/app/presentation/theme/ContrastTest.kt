package com.zerowasteeats.app.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG contrast of the text and icon pairs the app relies on, in light and dark mode. */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertReadable(name: String, foreground: Color, background: Color, minimum: Double = 4.5) {
        val actual = ratio(foreground, background)
        assertTrue("$name is $actual, needs $minimum", actual >= minimum)
    }

    /** What a scrim of [alpha] looks like over a photo that is all [photo]. */
    private fun scrimOver(photo: Color, alpha: Float) = RescateCarbon.copy(alpha = alpha).compositeOver(photo)

    @Test
    fun `secondary text reads on the light surfaces`() {
        assertReadable("muted on cream", RescateTextMutedLight, RescateCreamBackground)
        assertReadable("muted on surface", RescateTextMutedLight, RescateSurfaceLight)
        assertReadable("muted on surface variant", RescateTextMutedLight, RescateSurfaceVariantLight)
    }

    @Test
    fun `secondary text reads on the dark surfaces`() {
        assertReadable("muted on carbon", RescateCreamMuted, RescateCarbon)
        assertReadable("muted on elevated carbon", RescateCreamMuted, RescateCarbonElevated)
        assertReadable("muted on surface variant", RescateCreamMuted, RescateSurfaceVariantDark)
    }

    @Test
    fun `chips over a photo read even over a white photo`() {
        assertReadable("cream chip", RescateCream, scrimOver(Color.White, PhotoScrim.CHIP_ALPHA))
        assertReadable("discount badge", RescateCream, RescateDiscountBadge)
    }

    @Test
    fun `round buttons over a photo read over white and over black photos`() {
        listOf(Color.White, Color.Black).forEach { photo ->
            val backing = scrimOver(photo, PhotoScrim.CONTROL_ALPHA)
            assertReadable("cream icon over $photo", RescateCream, backing)
            assertReadable("favorite heart over $photo", RescateCoralOnPhoto, backing)
        }
    }
}
