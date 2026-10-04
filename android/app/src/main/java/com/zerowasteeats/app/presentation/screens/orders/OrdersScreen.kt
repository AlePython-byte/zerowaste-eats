package com.zerowasteeats.app.presentation.screens.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.orders.OrderCard
import com.zerowasteeats.app.presentation.data.MockOrders
import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

enum class OrdersTab(val label: String) {
    ACTIVE("Activos"),
    HISTORY("Historial")
}

@Composable
fun OrdersScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(OrdersTab.ACTIVE) }

    val allOrders = MockOrders.getList()
    val displayedOrders = allOrders.filter { order ->
        when (selectedTab) {
            OrdersTab.ACTIVE -> order.status == OrderStatus.RESERVED || order.status == OrderStatus.READY_FOR_PICKUP
            OrdersTab.HISTORY -> order.status == OrderStatus.COMPLETED || order.status == OrderStatus.CANCELLED
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.regular,
                        end = AppSpacing.regular,
                        top = AppSpacing.regular,
                        bottom = AppSpacing.small
                    )
            ) {
                Text(
                    text = "Pedidos",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                Text(
                    text = "Gestiona tus reservas y recogidas.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                // Segmented Selector
                OrdersSegmentedSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            // List Content
            if (displayedOrders.isEmpty()) {
                EmptyOrdersState(selectedTab = selectedTab)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.regular,
                        end = AppSpacing.regular,
                        top = AppSpacing.small,
                        bottom = AppSpacing.huge + 80.dp // To prevent hidden content under GlassBottomBar
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    items(displayedOrders, key = { it.id }) { order ->
                        OrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersSegmentedSelector(
    selectedTab: OrdersTab,
    onTabSelected: (OrdersTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val backgroundColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.5f)
    } else {
        GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.5f)
    }
    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(AppRadius.extraLarge),
        color = backgroundColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrdersTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppRadius.extraLarge))
                        .clickable { onTabSelected(tab) }
                        .padding(AppSpacing.extraSmall)
                ) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(AppRadius.large),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {}
                    }
                    
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(selectedTab: OrdersTab, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = AppSpacing.huge + 80.dp), // offset for bottom bar
        contentAlignment = Alignment.Center
    ) {
        val text = when (selectedTab) {
            OrdersTab.ACTIVE -> "No tienes pedidos activos."
            OrdersTab.HISTORY -> "Aún no tienes pedidos anteriores."
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
