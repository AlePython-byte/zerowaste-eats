package com.zerowasteeats.app.presentation.format

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class PesosInputTransformationTest {

    private fun transform(digits: String) = PesosInputTransformation.filter(AnnotatedString(digits))

    @Test
    fun digits_areShownAsPesos() {
        assertEquals("$8.900", transform("8900").text.text)
        assertEquals("$1.250.000", transform("1250000").text.text)
        assertEquals("$5", transform("5").text.text)
    }

    @Test
    fun anEmptyField_staysEmpty() {
        assertEquals("", transform("").text.text)
    }

    @Test
    fun cursorPositions_mapBothWays() {
        // "8900" -> "$8.900"
        val mapping = transform("8900").offsetMapping

        assertEquals(1, mapping.originalToTransformed(0)) // after "$"
        assertEquals(2, mapping.originalToTransformed(1)) // "$8|"
        assertEquals(4, mapping.originalToTransformed(2)) // "$8.9|"
        assertEquals(6, mapping.originalToTransformed(4)) // at the end

        assertEquals(0, mapping.transformedToOriginal(1))
        assertEquals(1, mapping.transformedToOriginal(3)) // "$8.|" is after the first digit
        assertEquals(4, mapping.transformedToOriginal(6))
    }
}
