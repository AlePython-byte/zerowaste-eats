package com.zerowasteeats.app.presentation.screens.feed

import com.zerowasteeats.app.data.catalog.CatalogError
import com.zerowasteeats.app.data.catalog.FavoriteResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteProblemTest {

    @Test
    fun aSavedFavorite_needsNoMessage() {
        assertNull(FavoriteResult.Saved.problemFor("a", true))
    }

    @Test
    fun anOfferNoLongerOnSale_isSaidAsSuch() {
        assertEquals(FavoriteProblem.NotAvailable, FavoriteResult.NotAvailable.problemFor("a", true))
    }

    @Test
    fun aFailure_remembersWhatToTryAgain() {
        val problem = FavoriteResult.Failed(CatalogError.NETWORK).problemFor("a", false)

        assertEquals(FavoriteProblem.Failed(CatalogError.NETWORK, "a", false), problem)
    }
}
