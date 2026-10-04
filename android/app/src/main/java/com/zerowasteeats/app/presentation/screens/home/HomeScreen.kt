package com.zerowasteeats.app.presentation.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassSearchBar
import com.zerowasteeats.app.presentation.components.offers.OfferCard
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

@Composable
fun HomeScreen(
    onMapClick: () -> Unit = {},
    onOfferClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Todo") }

    val categories = listOf("Todo", "Panadería", "Comida preparada", "Postres")
    val mockOffers = getMockOffers()

    val filteredOffers = mockOffers.filter { offer ->
        val matchesSearch = offer.title.contains(searchQuery, ignoreCase = true) ||
                offer.merchantName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Todo" || offer.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle tonal depth in the background header area
            val isDark = isSystemInDarkTheme()
            val gradientColor = if (isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                gradientColor,
                                Color.Transparent
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppSpacing.regular,
                    end = AppSpacing.regular,
                    top = AppSpacing.regular, // Slightly tighter top spacing
                    bottom = AppSpacing.huge + 64.dp // Padding for future FAB/Nav
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium) // Improved info density
            ) {
                item {
                    HomeHeader()
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    GlassSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Buscar comida cerca de ti",
                        clearContentDescription = "Limpiar búsqueda",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    SectionHeader(onMapClick = onMapClick)
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                        contentPadding = PaddingValues(bottom = AppSpacing.small)
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
        }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Buenos días",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
        Text(
            text = "Encuentra algo bueno cerca de ti",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cerca de ti",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        TextButton(
            onClick = onMapClick,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
        ) {
            Text(
                text = "Ver mapa",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Highly refined, restrained chips
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDarkTheme) GlassTokens.darkSurface.copy(alpha = 0.5f) 
        else GlassTokens.lightSurface.copy(alpha = 0.5f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        Color.Transparent
    } else {
        if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(AppRadius.extraLarge))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = containerColor,
        border = if (!isSelected) BorderStroke(GlassTokens.borderWidth, borderColor) else null,
        shadowElevation = 0.dp // Flatter chips to let cards pop
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.large)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
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
            text = "No encontramos ofertas con esos filtros.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = "Intenta buscar otra cosa o cambia de categoría.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

private fun getMockOffers(): List<OfferUiModel> = listOf(
    OfferUiModel(
        id = "1",
        title = "Croissants del día",
        merchantName = "Panadería Artesanal",
        imageUrl = "",
        originalPrice = "$15.000",
        discountedPrice = "$8.900",
        discountPercentage = "40% menos",
        distance = "0.5 km",
        remainingQuantityText = "Quedan 2",
        pickupTime = "Recoge hoy, 6:00 p. m. - 7:30 p. m.",
        isFavorite = false,
        category = "Panadería"
    ),
    OfferUiModel(
        id = "2",
        title = "Bowl saludable de salmón",
        merchantName = "Green Vida",
        imageUrl = "",
        originalPrice = "$32.000",
        discountedPrice = "$19.000",
        discountPercentage = "40% menos",
        distance = "1.2 km",
        remainingQuantityText = "Queda 1",
        pickupTime = "Recoge hoy, 8:00 p. m. - 9:00 p. m.",
        isFavorite = true,
        category = "Comida preparada"
    ),
    OfferUiModel(
        id = "3",
        title = "Caja de postres surtidos",
        merchantName = "Dulce Rincón",
        imageUrl = "",
        originalPrice = "$20.000",
        discountedPrice = "$9.900",
        discountPercentage = "50% menos",
        distance = "2.5 km",
        remainingQuantityText = "Quedan 3",
        pickupTime = "Recoge mañana, 10:00 a. m. - 12:00 p. m.",
        isFavorite = false,
        category = "Postres"
    )
)

@Preview
@Composable
private fun HomeScreenPreview() {
    ZeroWasteEatsTheme {
        HomeScreen()
    }
}
