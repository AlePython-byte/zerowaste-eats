package com.zerowasteeats.app.presentation.components.offers

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartBurstTest {

    @Test
    fun `the burst starts at the heart and ends at the full reach`() {
        assertEquals(0f, burstRadius(0f, maxRadius = 20f), 0.001f)
        assertEquals(20f, burstRadius(1f, maxRadius = 20f), 0.001f)
    }

    @Test
    fun `the sparks are solid at first and gone at the end`() {
        assertEquals(1f, burstAlpha(0f), 0.001f)
        assertEquals(1f, burstAlpha(0.4f), 0.001f)
        assertEquals(0f, burstAlpha(1f), 0.001f)
    }

    @Test
    fun `the first spark points up and the third points right`() {
        val center = Offset(50f, 50f)

        val up = burstSparkCenter(0, center, radius = 10f)
        val right = burstSparkCenter(2, center, radius = 10f)

        assertEquals(50f, up.x, 0.001f)
        assertEquals(40f, up.y, 0.001f)
        assertEquals(60f, right.x, 0.001f)
        assertEquals(50f, right.y, 0.001f)
    }
}
