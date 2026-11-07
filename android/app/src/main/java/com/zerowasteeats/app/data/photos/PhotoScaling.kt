package com.zerowasteeats.app.data.photos

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The arithmetic of preparing an offer photo, kept apart from Android so it can be tested:
 * how far to sample while decoding, the final size, how EXIF orientation turns the picture,
 * and how to stay under the server's size limit.
 */
object PhotoScaling {
    /** The longest side of a prepared photo, in pixels. */
    const val MAX_SIDE = 1280

    /** The first JPEG quality tried: about 250 KB for a typical food photo at 1280 px. */
    const val JPEG_QUALITY = 80

    /** The backend refuses photos over 2 MB. */
    const val SERVER_LIMIT_BYTES = 2 * 1024 * 1024

    /** A little under [SERVER_LIMIT_BYTES], so multipart overhead never tips it over. */
    const val MAX_BYTES = SERVER_LIMIT_BYTES - 64 * 1024

    /** Tried in order until the photo fits in [MAX_BYTES]. */
    val QUALITIES = listOf(JPEG_QUALITY, 70, 60, 50)

    /** When even the lowest quality is too big, the photo shrinks, but never below this. */
    const val MIN_SIDE = 480

    private const val SHRINK_STEP = 0.75

    data class Size(val width: Int, val height: Int) {
        val longSide: Int get() = max(width, height)
    }

    /** How a picture must turn to stand upright, as EXIF orientation describes it. */
    data class Orientation(
        /** Clockwise. */
        val rotationDegrees: Int,
        /** Mirrored horizontally after the rotation. */
        val mirrored: Boolean
    ) {
        /** A quarter turn swaps width and height. */
        val swapsSides: Boolean get() = rotationDegrees % 180 != 0

        companion object {
            val UPRIGHT = Orientation(0, mirrored = false)
        }
    }

    /**
     * The size that fits [maxSide] on the longest side, keeping the proportions. A photo that
     * already fits is left as it is: nothing is enlarged.
     */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Size {
        require(width > 0 && height > 0) { "A photo has a size" }
        val longSide = max(width, height)
        if (longSide <= maxSide) return Size(width, height)
        val scale = maxSide.toDouble() / longSide
        return Size(
            max(1, (width * scale).roundToInt()),
            max(1, (height * scale).roundToInt())
        )
    }

    /**
     * The decoder's sample size (a power of two): the largest that still leaves the longest
     * side at [maxSide] or more, so the final scaling only ever shrinks and the decoder never
     * holds the full-resolution picture in memory.
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        val longSide = max(width, height)
        var sample = 1
        while (longSide / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    /** The size once [orientation] is applied. */
    fun orientedSize(width: Int, height: Int, orientation: Orientation): Size =
        if (orientation.swapsSides) Size(height, width) else Size(width, height)

    /**
     * The turn for an EXIF orientation value (1 to 8, as ExifInterface defines them). Missing
     * or unknown values mean upright.
     */
    fun orientationFromExif(value: Int): Orientation = when (value) {
        2 -> Orientation(0, mirrored = true) // FLIP_HORIZONTAL
        3 -> Orientation(180, mirrored = false) // ROTATE_180
        4 -> Orientation(180, mirrored = true) // FLIP_VERTICAL
        5 -> Orientation(90, mirrored = true) // TRANSPOSE
        6 -> Orientation(90, mirrored = false) // ROTATE_90
        7 -> Orientation(270, mirrored = true) // TRANSVERSE
        8 -> Orientation(270, mirrored = false) // ROTATE_270
        else -> Orientation.UPRIGHT
    }

    class Encoded(val bytes: ByteArray, val size: Size, val quality: Int)

    /**
     * Encodes at [size], lowering the quality and then the size until the result fits in
     * [maxBytes]. [encode] does the real work (JPEG at the given size and quality). Returns
     * null only if the photo cannot fit even at [MIN_SIDE] and the lowest quality.
     */
    fun encodeWithinLimit(
        size: Size,
        maxBytes: Int = MAX_BYTES,
        encode: (Size, Int) -> ByteArray
    ): Encoded? {
        var current = size
        while (true) {
            for (quality in QUALITIES) {
                val bytes = encode(current, quality)
                if (bytes.size <= maxBytes) return Encoded(bytes, current, quality)
            }
            if (current.longSide <= MIN_SIDE) return null
            val smaller = max(MIN_SIDE, (current.longSide * SHRINK_STEP).roundToInt())
            current = targetSize(current.width, current.height, smaller)
        }
    }
}
