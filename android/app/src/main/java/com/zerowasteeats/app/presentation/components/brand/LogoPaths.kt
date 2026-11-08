package com.zerowasteeats.app.presentation.components.brand

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes

/**
 * Geometry of the logo, in a [VIEWPORT] x [VIEWPORT] box: a butter paper bag whose coral
 * handles draw a heart, with a leaf cut out of the bag. The shapes are symmetric about x = 54.
 *
 * This is the source of truth. The vector drawables copy these strings (resources cannot be
 * built from Kotlin), and LogoPathsTest fails as soon as a copy drifts.
 */
object LogoPaths {
    const val VIEWPORT = 108f

    /** The handle is a stroke, not a filled shape: round caps and joins, this width. */
    const val HANDLE_STROKE_WIDTH = 6.5f

    /** Trapezoid with rounded corners, wider at the bottom. */
    const val BAG =
        "M34,44.1 H74 Q79.5,44.1 80.56,49.5 L87.56,85.24 Q89,92.6 81.5,92.6 H26.5 " +
            "Q19,92.6 20.44,85.24 L27.44,49.5 Q28.5,44.1 34,44.1 Z"

    /** Two round holes where the handle enters the bag. */
    const val EYELETS =
        "M36,50.6 C36,47.839 38.239,45.6 41,45.6 C43.761,45.6 46,47.839 46,50.6 " +
            "C46,53.361 43.761,55.6 41,55.6 C38.239,55.6 36,53.361 36,50.6 Z " +
            "M62,50.6 C62,47.839 64.239,45.6 67,45.6 C69.761,45.6 72,47.839 72,50.6 " +
            "C72,53.361 69.761,55.6 67,55.6 C64.239,55.6 62,53.361 62,50.6 Z"

    /** Leaf with its stem; the vein is a notch in the outline, so it is one closed shape. */
    const val LEAF =
        "M39.4,85 C36.5,78.1 37,70.6 42,66.1 C47,61.6 55,60.9 61.5,60.6 " +
            "C64.5,60.5 66.5,60 68.6,59.4 C69.5,59.2 69.8,60 69.8,60.8 " +
            "C69.8,67.6 68,75.6 62.5,80.6 C58.5,84.2 53,85.4 48,85.9 " +
            "C45.5,86.1 43.7,86.6 42.9,87.9 C42.4,88.7 41.2,88.5 41.5,87.5 " +
            "C43.5,79.1 50.5,72.1 61.1,68.2 C51,70.6 43,76.1 39.4,85 Z"

    /** Centre line of the handle: up from one eyelet, round the heart, down to the other. */
    const val HANDLE =
        "M41,50.6 V46.6 C41,41.6 31.5,36.6 31.5,29.6 C31.5,23.1 36.5,18.6 42.5,18.6 " +
            "C47.5,18.6 51,22.1 54,25.1 C57,22.1 60.5,18.6 65.5,18.6 " +
            "C71.5,18.6 76.5,23.1 76.5,29.6 C76.5,36.6 67,41.6 67,46.6 V50.6"

    /** The bag with the eyelets and the leaf as holes. Fill it with the even-odd rule. */
    const val BAG_WITH_CUTOUTS = "$BAG $EYELETS $LEAF"

    val bag: List<PathNode> by lazy { addPathNodes(BAG) }
    val eyelets: List<PathNode> by lazy { addPathNodes(EYELETS) }
    val leaf: List<PathNode> by lazy { addPathNodes(LEAF) }
    val handle: List<PathNode> by lazy { addPathNodes(HANDLE) }
    val bagWithCutouts: List<PathNode> by lazy { addPathNodes(BAG_WITH_CUTOUTS) }
}
