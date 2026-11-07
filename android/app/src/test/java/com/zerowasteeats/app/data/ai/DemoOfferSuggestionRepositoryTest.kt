package com.zerowasteeats.app.data.ai

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoOfferSuggestionRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val repository = DemoOfferSuggestionRepository(latencyMillis = 0)

    private fun photo(name: String, bytes: ByteArray): File =
        folder.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun worksWithoutAnyServer_andIsAlwaysAnExample() = runBlocking {
        val result = repository.suggest(photo("a.jpg", byteArrayOf(1, 2, 3)), 15000)

        val suggestion = (result as SuggestionResult.Success).suggestion
        assertTrue(suggestion.isExample)
        assertNotNull(suggestion.category)
        assertTrue(suggestion.discountPercent in 30..70)
        assertEquals(0, suggestion.discountedPrice!! % 100)
        assertTrue(suggestion.discountedPrice!! < 15000)
    }

    @Test
    fun theSamePhoto_alwaysGetsTheSameSuggestion() = runBlocking {
        val bytes = ByteArray(500) { (it * 7).toByte() }

        assertEquals(
            repository.suggest(photo("a.jpg", bytes), 9000),
            repository.suggest(photo("b.jpg", bytes), 9000)
        )
    }

    @Test
    fun withoutAPrice_thereIsOnlyThePercent() = runBlocking {
        val suggestion =
            (repository.suggest(photo("a.jpg", byteArrayOf(9)), null) as SuggestionResult.Success).suggestion

        assertNull(suggestion.discountedPrice)
    }

    @Test
    fun aMissingFile_stillAnswers() = runBlocking {
        assertTrue(repository.suggest(File(folder.root, "gone.jpg"), null) is SuggestionResult.Success)
    }

    @Test
    fun theDiscountedPrice_isRoundedLikeTheServer() {
        assertEquals(6000, DemoOfferSuggestionRepository.discountedPrice(12000, 50))
        assertEquals(7400, DemoOfferSuggestionRepository.discountedPrice(12350, 40))
        assertEquals(100, DemoOfferSuggestionRepository.discountedPrice(150, 70))
    }
}
