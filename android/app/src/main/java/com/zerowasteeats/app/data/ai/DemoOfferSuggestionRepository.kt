package com.zerowasteeats.app.data.ai

import com.zerowasteeats.app.presentation.model.OfferCategory
import java.io.File
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Suggestions for demo mode: chosen from a small local set by the photo's bytes, so the same
 * photo always gets the same one and the feature can be shown with no internet at all.
 * Always marked as an example.
 */
class DemoOfferSuggestionRepository(
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS
) : OfferSuggestionRepository {

    private class Sample(
        val title: String,
        val category: OfferCategory,
        val description: String,
        val discountPercent: Int
    )

    private val samples = listOf(
        Sample(
            "Bolsa sorpresa de panadería",
            OfferCategory.BAKERY,
            "Pan del día y piezas de hojaldre que no alcanzaron a venderse. Ideales para el desayuno o las onces.",
            50
        ),
        Sample(
            "Almuerzo del día para llevar",
            OfferCategory.PREPARED_FOOD,
            "Porción del menú de hoy, preparada esta mañana. Recógela en el horario indicado y caliéntala antes de comer.",
            60
        ),
        Sample(
            "Caja de postres surtidos",
            OfferCategory.DESSERTS,
            "Selección de postres de la vitrina de hoy. Mantenlos refrigerados y consúmelos el mismo día.",
            45
        ),
        Sample(
            "Canasta de frutas y verduras",
            OfferCategory.PRODUCE,
            "Frutas y verduras frescas con detalles de apariencia, perfectas para jugos, sopas o compotas.",
            40
        )
    )

    override suspend fun suggest(photo: File, originalPrice: Long?): SuggestionResult {
        delay(latencyMillis)
        val seed = withContext(Dispatchers.IO) {
            runCatching { photo.readBytes().fold(0) { acc, byte -> acc * 31 + byte } }.getOrDefault(0)
        }
        val sample = samples[Math.floorMod(seed, samples.size)]
        return SuggestionResult.Success(
            OfferSuggestion(
                title = sample.title,
                category = sample.category,
                description = sample.description,
                discountPercent = sample.discountPercent,
                discountedPrice = originalPrice?.let { discountedPrice(it, sample.discountPercent) },
                confidence = 0.5,
                warnings = emptyList(),
                isExample = true
            )
        )
    }

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 900L
        private const val PRICE_STEP = 100

        /** Like the server: the discounted price rounded to the nearest 100 pesos, at least 100. */
        fun discountedPrice(originalPrice: Long, discountPercent: Int): Long {
            val raw = originalPrice * (100 - discountPercent) / 100.0
            return maxOf(PRICE_STEP.toLong(), (raw / PRICE_STEP).roundToLong() * PRICE_STEP)
        }
    }
}
