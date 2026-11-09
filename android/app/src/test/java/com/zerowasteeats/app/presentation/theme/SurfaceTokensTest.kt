package com.zerowasteeats.app.presentation.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceTokensTest {

    @Test
    fun `cards have a soft shadow in light mode and none in dark mode`() {
        assertEquals(2.dp, surfaceElevation(isDark = false))
        assertEquals(0.dp, surfaceElevation(isDark = true))
    }

    @Test
    fun `the border is faint in both modes and a little stronger in dark mode`() {
        assertTrue(surfaceBorderAlpha(isDark = true) > surfaceBorderAlpha(isDark = false))
        assertTrue(surfaceBorderAlpha(isDark = true) <= 0.2f)
    }
}
