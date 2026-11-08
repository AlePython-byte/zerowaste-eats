package com.zerowasteeats.app.presentation.components.brand

import com.zerowasteeats.app.presentation.components.common.LOGO_HANDLE_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HANDLE_START_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HEART_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_HEART_START_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_LEAF_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_LEAF_START_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_TOTAL_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoTimelineTest {

    @Test
    fun phase_isZeroBeforeItStarts_andOneAfterItEnds() {
        assertEquals(0f, LogoFrame.phase(0f, 300, 400), 0f)
        assertEquals(0f, LogoFrame.phase(300f, 300, 400), 0f)
        assertEquals(0.5f, LogoFrame.phase(500f, 300, 400), 0.0001f)
        assertEquals(1f, LogoFrame.phase(700f, 300, 400), 0f)
        assertEquals(1f, LogoFrame.phase(5000f, 300, 400), 0f)
    }

    @Test
    fun atTheStart_nothingButTheBagIsShown() {
        assertEquals(LogoFrame.Start, LogoFrame.at(0f))
    }

    @Test
    fun atTheEnd_thePictureIsTheStaticOne() {
        assertEquals(LogoFrame.Complete, LogoFrame.at(LOGO_TOTAL_MILLIS.toFloat()))
        assertEquals(LogoFrame.Complete, LogoFrame.at(LOGO_TOTAL_MILLIS + 1000f))
    }

    @Test
    fun theSequenceTakesAboutOnePointTwoSeconds() {
        assertTrue(LOGO_TOTAL_MILLIS in 1100..1300)
    }

    @Test
    fun steps_followOneAnother_withoutWaitingForTheOneBeforeToEnd() {
        assertTrue(LOGO_LEAF_START_MILLIS > LOGO_HANDLE_START_MILLIS)
        assertTrue(LOGO_HEART_START_MILLIS > LOGO_LEAF_START_MILLIS)
        // The handle is still drawing when the leaf starts: one movement, not four
        assertTrue(LOGO_LEAF_START_MILLIS < LOGO_HANDLE_START_MILLIS + LOGO_HANDLE_MILLIS)
        // The beat comes last
        assertEquals(LOGO_TOTAL_MILLIS, LOGO_HEART_START_MILLIS + LOGO_HEART_MILLIS)
        assertEquals(LOGO_LEAF_START_MILLIS + LOGO_LEAF_MILLIS, LOGO_HEART_START_MILLIS)
    }

    @Test
    fun handleAndLeaf_neverGoBackwards() {
        val frames = (0..LOGO_TOTAL_MILLIS step 10).map { LogoFrame.at(it.toFloat()) }

        frames.zipWithNext().forEach { (a, b) ->
            assertTrue(b.handleProgress >= a.handleProgress)
            assertTrue(b.leafProgress >= a.leafProgress)
        }
    }

    @Test
    fun heart_beatsOnce_peakingAtTheMiddleOfItsStep() {
        val peak = LogoFrame.at(LOGO_HEART_START_MILLIS + LOGO_HEART_MILLIS / 2f).heartScale
        val frames = (0..LOGO_TOTAL_MILLIS step 5).map { LogoFrame.at(it.toFloat()).heartScale }

        assertEquals(1f + LogoFrame.HEART_BEAT_AMPLITUDE, peak, 0.001f)
        assertEquals(1f, LogoFrame.at(LOGO_HEART_START_MILLIS.toFloat()).heartScale, 0.0001f)
        // Up then down: exactly one local maximum
        val rising = frames.zipWithNext().count { (a, b) -> b > a }
        val falling = frames.zipWithNext().count { (a, b) -> b < a }
        assertTrue(rising > 0 && falling > 0)
        val firstFall = frames.zipWithNext().indexOfFirst { (a, b) -> b < a }
        assertTrue(frames.drop(firstFall).zipWithNext().none { (a, b) -> b > a })
    }

    @Test
    fun leaf_growsOnlyAfterItsStep_notDuringTheHandle() {
        assertEquals(0f, LogoFrame.at(LOGO_LEAF_START_MILLIS - 1f).leafProgress, 0f)
        assertTrue(LogoFrame.at(LOGO_LEAF_START_MILLIS + 50f).leafProgress > 0f)
    }
}
