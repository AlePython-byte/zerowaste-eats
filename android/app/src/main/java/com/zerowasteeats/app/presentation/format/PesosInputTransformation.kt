package com.zerowasteeats.app.presentation.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shows the digits of a price field as [formatPesos] would ("8900" -> "$8.900") while the
 * person types. The field's value stays plain digits; only the display changes.
 */
object PesosInputTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty() || !digits.all { it.isDigit() }) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val shown = formatPesos(digits.toLong())

        val mapping = object : OffsetMapping {
            // The cursor after the k-th digit sits after the k-th digit of the shown text
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 1 // After the "$"
                var digitsSeen = 0
                shown.forEachIndexed { index, char ->
                    if (char.isDigit()) {
                        digitsSeen++
                        if (digitsSeen == offset) return index + 1
                    }
                }
                return shown.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                shown.take(offset).count { it.isDigit() }
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
