package com.zerowasteeats.app.presentation.components.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SkeletonShimmerTest {

    @Test
    fun `the band starts fully off the left side`() {
        assertEquals(-60f, shimmerBandStart(progress = 0f, width = 100f, bandWidth = 60f), 0.001f)
    }

    @Test
    fun `the band ends fully off the right side`() {
        assertEquals(100f, shimmerBandStart(progress = 1f, width = 100f, bandWidth = 60f), 0.001f)
    }

    @Test
    fun `the band moves at an even pace in between`() {
        assertEquals(20f, shimmerBandStart(progress = 0.5f, width = 100f, bandWidth = 60f), 0.001f)
    }

    @Test
    fun `progress outside the sweep is held at its ends`() {
        assertEquals(-60f, shimmerBandStart(progress = -1f, width = 100f, bandWidth = 60f), 0.001f)
        assertEquals(100f, shimmerBandStart(progress = 2f, width = 100f, bandWidth = 60f), 0.001f)
    }
}
