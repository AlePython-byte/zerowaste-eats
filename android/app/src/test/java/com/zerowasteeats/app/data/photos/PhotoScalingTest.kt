package com.zerowasteeats.app.data.photos

import com.zerowasteeats.app.data.photos.PhotoScaling.Orientation
import com.zerowasteeats.app.data.photos.PhotoScaling.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoScalingTest {

    // --- Final size ---

    @Test
    fun aLandscapePhoto_fitsTheLongSideTo1280_keepingItsProportions() {
        assertEquals(Size(1280, 960), PhotoScaling.targetSize(4032, 3024))
    }

    @Test
    fun aPortraitPhoto_fitsItsHeight() {
        assertEquals(Size(960, 1280), PhotoScaling.targetSize(3024, 4032))
    }

    @Test
    fun aSquarePhoto_staysSquare() {
        assertEquals(Size(1280, 1280), PhotoScaling.targetSize(3000, 3000))
    }

    @Test
    fun aSmallPhoto_isNeverEnlarged() {
        assertEquals(Size(800, 600), PhotoScaling.targetSize(800, 600))
        assertEquals(Size(1280, 720), PhotoScaling.targetSize(1280, 720))
    }

    @Test
    fun aVeryThinPhoto_keepsAtLeastOnePixel() {
        assertEquals(Size(1280, 1), PhotoScaling.targetSize(20000, 2))
    }

    @Test
    fun anEmptySize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { PhotoScaling.targetSize(0, 100) }
    }

    // --- Sampling while decoding ---

    @Test
    fun sampling_isThePowerOfTwoThatKeepsTheLongSideAboveTheTarget() {
        // 4032 / 2 = 2016 ≥ 1280, but 4032 / 4 = 1008 < 1280
        assertEquals(2, PhotoScaling.sampleSize(4032, 3024))
        // A 48 MP photo: 8000 / 4 = 2000, / 8 = 1000
        assertEquals(4, PhotoScaling.sampleSize(8000, 6000))
        assertEquals(8, PhotoScaling.sampleSize(12000, 9000))
    }

    @Test
    fun sampling_neverTakesThePhotoBelowTheTarget() {
        listOf(1281 to 1, 2559 to 1, 2560 to 2, 5119 to 2, 5120 to 4, 16000 to 8).forEach { (side, sample) ->
            assertEquals("side $side", sample, PhotoScaling.sampleSize(side, side / 2))
            assertTrue(side / sample >= PhotoScaling.MAX_SIDE)
        }
    }

    @Test
    fun aPhotoAlreadySmall_isNotSampled() {
        assertEquals(1, PhotoScaling.sampleSize(1000, 700))
    }

    @Test
    fun sampling_canAimAtAnySize_forShowingThumbnails() {
        assertEquals(4, PhotoScaling.sampleSize(1280, 960, maxSide = 216))
    }

    // --- EXIF orientation ---

    @Test
    fun exifValues_turnThePhotoUpright() {
        assertEquals(Orientation.UPRIGHT, PhotoScaling.orientationFromExif(1))
        assertEquals(Orientation(90, false), PhotoScaling.orientationFromExif(6))
        assertEquals(Orientation(180, false), PhotoScaling.orientationFromExif(3))
        assertEquals(Orientation(270, false), PhotoScaling.orientationFromExif(8))
        assertEquals(Orientation(0, true), PhotoScaling.orientationFromExif(2))
        assertEquals(Orientation(180, true), PhotoScaling.orientationFromExif(4))
        assertEquals(Orientation(90, true), PhotoScaling.orientationFromExif(5))
        assertEquals(Orientation(270, true), PhotoScaling.orientationFromExif(7))
    }

    @Test
    fun missingOrUnknownExif_meansUpright() {
        listOf(0, 9, -1).forEach {
            assertEquals(Orientation.UPRIGHT, PhotoScaling.orientationFromExif(it))
        }
    }

    @Test
    fun aQuarterTurn_swapsWidthAndHeight_beforeFittingTheSize() {
        // A phone photo stored landscape with "rotate 90": upright it is portrait
        val upright = PhotoScaling.orientedSize(4032, 3024, PhotoScaling.orientationFromExif(6))

        assertEquals(Size(3024, 4032), upright)
        assertEquals(Size(960, 1280), PhotoScaling.targetSize(upright.width, upright.height))
        assertFalse(Orientation(180, false).swapsSides)
        assertTrue(Orientation(270, true).swapsSides)
    }

    // --- Staying under the server's limit ---

    /** Bytes roughly like a JPEG: proportional to pixels and quality. */
    private fun fakeJpeg(bytesPerPixelAtQuality100: Double) = { size: Size, quality: Int ->
        ByteArray((size.width.toLong() * size.height * bytesPerPixelAtQuality100 * quality / 100).toInt())
    }

    @Test
    fun aTypicalPhoto_isEncodedOnceAtQuality80_wellUnder2MB() {
        val tried = mutableListOf<Int>()
        // About 0.25 B per pixel at quality 80, like a food photo: ~300 KB at 1280×960
        val result = PhotoScaling.encodeWithinLimit(Size(1280, 960)) { size, quality ->
            tried += quality
            fakeJpeg(0.3125)(size, quality)
        }!!

        assertEquals(listOf(80), tried)
        assertEquals(80, result.quality)
        assertEquals(Size(1280, 960), result.size)
        assertTrue(result.bytes.size < 400_000)
    }

    @Test
    fun aHeavyPhoto_lowersTheQualityFirst() {
        val result = PhotoScaling.encodeWithinLimit(Size(1280, 1280), encode = fakeJpeg(2.0))!!

        assertEquals(Size(1280, 1280), result.size)
        assertTrue(result.quality < PhotoScaling.JPEG_QUALITY)
        assertTrue(result.bytes.size <= PhotoScaling.MAX_BYTES)
    }

    @Test
    fun anExtremePhoto_shrinksOnlyAfterTheLowestQuality_andStillFits() {
        val attempts = mutableListOf<Pair<Int, Int>>()
        val result = PhotoScaling.encodeWithinLimit(Size(1280, 1280)) { size, quality ->
            attempts += size.longSide to quality
            fakeJpeg(6.0)(size, quality)
        }!!

        // Every quality at full size first, from the highest down
        assertEquals(
            PhotoScaling.QUALITIES.map { 1280 to it },
            attempts.take(PhotoScaling.QUALITIES.size)
        )
        assertTrue(result.size.longSide < 1280)
        assertTrue(result.size.longSide >= PhotoScaling.MIN_SIDE)
        assertTrue(result.bytes.size <= PhotoScaling.MAX_BYTES)
    }

    @Test
    fun theLimit_staysBelowTheServers2MB() {
        assertTrue(PhotoScaling.MAX_BYTES < PhotoScaling.SERVER_LIMIT_BYTES)
        assertEquals(2 * 1024 * 1024, PhotoScaling.SERVER_LIMIT_BYTES)
    }

    @Test
    fun aPhotoThatCannotFit_givesUp_insteadOfLoopingForever() {
        var calls = 0
        val result = PhotoScaling.encodeWithinLimit(Size(1280, 960)) { _, _ ->
            calls++
            ByteArray(PhotoScaling.MAX_BYTES + 1)
        }

        assertNull(result)
        assertTrue(calls < 50)
    }
}
