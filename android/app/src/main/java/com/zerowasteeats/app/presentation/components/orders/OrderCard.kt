package com.zerowasteeats.app.presentation.components.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.components.glass.GlassCard
import com.zerowasteeats.app.presentation.model.OrderStatus
import com.zerowasteeats.app.presentation.model.OrderUiModel
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun OrderCard(
    order: OrderUiModel,
    modifier: Modifier = Modifier
) {
    val isActive = order.status == OrderStatus.RESERVED || order.status == OrderStatus.READY_FOR_PICKUP

    GlassCard(modifier = modifier.fillMaxWidth()) {
        // Header: Merchant & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = order.merchantName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(end = AppSpacing.small)
            )
            OrderStatusBadge(status = order.status)
        }

        Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

        // Title
        Text(
            text = order.offerTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        // Pickup Info
        Text(
            text = order.pickupTime,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Footer: Reservation Code & Pricing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Cantidad: ${order.quantity}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isActive) {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Text(
                        text = "Código: ${order.reservationCode}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = order.totalPrice,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val containerColor = when (status) {
        OrderStatus.RESERVED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.primary
        OrderStatus.COMPLETED -> if (isDarkTheme) GlassTokens.darkSurface else GlassTokens.lightSurface
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (status) {
        OrderStatus.RESERVED -> MaterialTheme.colorScheme.primary
        OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.onPrimary
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
    }

    val borderColor = if (status == OrderStatus.COMPLETED) {
        if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(AppRadius.small),
        color = containerColor,
        border = if (status == OrderStatus.COMPLETED) BorderStroke(GlassTokens.borderWidth, borderColor) else null
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall)
        )
    }
}
