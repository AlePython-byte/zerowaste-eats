package com.zerowasteeats.app.presentation.components.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathNode
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateDiscountBadge
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Keeps the vector drawables in step with [LogoPaths], their source of truth. */
class LogoPathsTest {

    private val android = "http://schemas.android.com/apk/res/android"

    // Gradle runs unit tests from the module folder
    private fun document(path: String): Element {
        val file = File("src/main/res/$path")
        assertTrue("missing ${file.absolutePath}", file.exists())
        return DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file).documentElement
    }

    /** Every named `<path>` of a vector drawable, by name. */
    private fun namedPaths(drawable: String): Map<String, Element> {
        val paths = document("drawable/$drawable.xml").getElementsByTagName("path")
        return (0 until paths.length)
            .map { paths.item(it) as Element }
            .associateBy { it.getAttributeNS(android, "name") }
    }

    private fun color(name: String): Color {
        val colors = document("values/colors.xml").getElementsByTagName("color")
        val value = (0 until colors.length)
            .map { colors.item(it) as Element }
            .single { it.getAttribute("name") == name }
            .textContent.trim().removePrefix("#")
        return Color(value.toLong(16).toInt())
    }

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    private fun PathNode.points(): List<Pair<Float, Float>> = when (this) {
        is PathNode.MoveTo -> listOf(x to y)
        is PathNode.LineTo -> listOf(x to y)
        is PathNode.QuadTo -> listOf(x1 to y1, x2 to y2)
        is PathNode.CurveTo -> listOf(x1 to y1, x2 to y2, x3 to y3)
        is PathNode.HorizontalTo -> listOf(x to LogoPaths.VIEWPORT / 2)
        is PathNode.VerticalTo -> listOf(LogoPaths.VIEWPORT / 2 to y)
        is PathNode.Close -> emptyList()
        else -> error("LogoPaths uses absolute commands only, found $this")
    }

    @Test
    fun icLogo_copiesLogoPathsExactly() {
        val paths = namedPaths("ic_logo")

        assertEquals(LogoPaths.BAG_WITH_CUTOUTS, paths.getValue("bag_body").getAttributeNS(android, "pathData"))
        assertEquals(LogoPaths.HANDLE, paths.getValue("handle_stroke").getAttributeNS(android, "pathData"))
        assertEquals(LogoPaths.LEAF, paths.getValue("leaf_outline").getAttributeNS(android, "pathData"))
    }

    @Test
    fun icLogo_cutsTheHolesWithEvenOdd_andStrokesTheHandle() {
        val paths = namedPaths("ic_logo")

        assertEquals("evenOdd", paths.getValue("bag_body").getAttributeNS(android, "fillType"))
        assertEquals(
            LogoPaths.HANDLE_STROKE_WIDTH,
            paths.getValue("handle_stroke").getAttributeNS(android, "strokeWidth").toFloat()
        )
    }

    @Test
    fun brandColors_matchTheTheme() {
        assertEquals(RescateButter, color("brand_butter"))
        assertEquals(RescateDiscountBadge, color("brand_coral"))
    }

    @Test
    fun everyPath_parsesAndStaysInsideTheViewport() {
        val all = listOf(LogoPaths.bag, LogoPaths.eyelets, LogoPaths.leaf, LogoPaths.handle)
        all.forEach { nodes ->
            assertTrue(nodes.isNotEmpty())
            nodes.flatMap { it.points() }.forEach { (x, y) ->
                assertTrue("($x, $y) outside", x in 0f..LogoPaths.VIEWPORT && y in 0f..LogoPaths.VIEWPORT)
            }
        }
        assertEquals(
            LogoPaths.bag.size + LogoPaths.eyelets.size + LogoPaths.leaf.size,
            LogoPaths.bagWithCutouts.size
        )
    }

    @Test
    fun coralHandle_onCarbon_meetsTheGraphicContrastMinimum() {
        // WCAG 1.4.11: graphics need 3:1. The dark mode needs no lighter coral.
        assertTrue(contrast(RescateDiscountBadge, RescateCarbon) >= 3f)
    }
}
