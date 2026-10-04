package com.zerowasteeats.app.presentation.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.filters.CategoryChip
import com.zerowasteeats.app.presentation.components.glass.GlassButton
import com.zerowasteeats.app.presentation.components.glass.GlassSearchBar
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.theme.AppSpacing

enum class ExploreSortOption(val label: String) {
    DISTANCE("Cercanía"),
    PRICE("Precio"),
    DISCOUNT("Descuento")
}

@Composable
fun ExploreScreen(
    onOfferClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Todo") }
    var sortOption by rememberSaveable { mutableStateOf(ExploreSortOption.DISTANCE) }

    val categories = listOf("Todo", "Panadería", "Comida preparada", "Postres")
    val sortOptions = ExploreSortOption.entries

    val mockOffers = MockOffers.getList()

    val filteredOffers = mockOffers.filter { offer ->
        val matchesSearch = offer.title.contains(searchQuery, ignoreCase = true) ||
                offer.merchantName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Todo" || offer.category == selectedCategory
        matchesSearch && matchesCategory
    }.sortedWith(Comparator { a, b ->
        when (sortOption) {
            ExploreSortOption.DISTANCE -> {
                val distA = a.distance.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f
                val distB = b.distance.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f
                distA.compareTo(distB)
            }
            ExploreSortOption.PRICE -> {
                val priceA = a.discountedPrice.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                val priceB = b.discountedPrice.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                priceA.compareTo(priceB)
            }
            ExploreSortOption.DISCOUNT -> {
                val discA = a.discountPercentage.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                val discB = b.discountPercentage.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                discB.compareTo(discA) // Descending
            }
        }
    })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppSpacing.regular,
                    end = AppSpacing.regular,
                    top = AppSpacing.regular,
                    bottom = AppSpacing.huge * 3 + 80.dp // Padding for bottom nav and floating map button
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Header
                item {
                    ExploreHeader()
                }

                // Search Bar
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    GlassSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Buscar ofertas o establecimientos",
                        clearContentDescription = "Limpiar búsqueda",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Filters
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(vertical = AppSpacing.small)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                text = category,
                                isSelected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }

                // Sorting
                item {
                    Text(
                        text = "Ordenar por",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = AppSpacing.small, bottom = AppSpacing.extraSmall)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(bottom = AppSpacing.small)
                    ) {
                        items(sortOptions) { option ->
                            CategoryChip(
                                text = option.label,
                                isSelected = sortOption == option,
                                onClick = { sortOption = option }
                            )
                        }
                    }
                }

                // Results Header
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.medium))
                    ResultsHeader(count = filteredOffers.size)
                }

                // Offers or Empty State
                if (filteredOffers.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(
                        items = filteredOffers,
                        key = { it.id }
                    ) { offer ->
                        OfferCard(
                            offer = offer,
                            onClick = { onOfferClick(offer.id) },
                            onFavoriteClick = { onFavoriteClick(offer.id) },
                            favoriteContentDescription = if (offer.isFavorite) "Quitar de favoritos" else "Marcar como favorito"
                        )
                    }
                }
            }

            // Floating Map Entry Point
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppSpacing.huge + 80.dp) // Avoid colliding with GlassBottomBar
            ) {
                GlassButton(
                    text = "Ver mapa",
                    onClick = onMapClick
                )
            }
        }
    }
}

@Composable
private fun ExploreHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Explorar",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = "Encuentra ofertas que se adapten a ti",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResultsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Ofertas encontradas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        
        val countText = if (count == 1) "1 oferta" else "$count ofertas"
        Text(
            text = countText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No encontramos resultados",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = "Prueba con otra búsqueda o cambia los filtros.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
