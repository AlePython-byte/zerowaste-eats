package com.zerowasteeats.app.presentation.screens.offerdetail

import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.format.formatted
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import com.zerowasteeats.app.presentation.components.common.AppMotion
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.components.offers.OfferPhotoImage
import com.zerowasteeats.app.presentation.format.formatPesos
import com.zerowasteeats.app.presentation.model.OfferUiModel
import com.zerowasteeats.app.presentation.navigation.sharedOfferImage
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.RescateCarbon
import com.zerowasteeats.app.presentation.theme.RescateCoral
import com.zerowasteeats.app.presentation.theme.RescateCream
import kotlinx.coroutines.launch

/** The offer's page, filled in from the catalog: the hero photo, what it is, and where to pick it up. */
@Composable
internal fun OfferDetailContent(
    offer: OfferUiModel,
    reserve: ReservePanelState,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onReserveClick: () -> Unit,
    snackbarHost: @Composable () -> Unit = {}
) {
    val discountText = stringResource(R.string.offer_discount_off, offer.discountPercent)
    val remainingText = pluralStringResource(R.plurals.offer_remaining_count, offer.remainingQuantity, offer.remainingQuantity)
    val heartScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val animationsEnabled = rememberAnimationsEnabled()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        bottomBar = {
            ReservationPanel(
                offer = offer,
                reserve = reserve,
                onDecreaseQuantity = onDecreaseQuantity,
                onIncreaseQuantity = onIncreaseQuantity,
                onReserveClick = onReserveClick
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(scrollState)
        ) {
            // HERO: full-bleed photo with a fade into the carbon background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clipToBounds() // the photo is shifted below, keep it inside the hero
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (offer.photo != null) {
                    OfferPhotoImage(
                        photo = offer.photo,
                        contentDescription = offer.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedOfferImage(offer.id)
                            .graphicsLayer {
                                // Parallax: the content scrolls by `value`, the photo drifts back
                                // down by half of it, so it moves at half speed.
                                if (animationsEnabled) translationY = scrollState.value * 0.5f
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )

            }

            // INFO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = offer.merchantName.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = offer.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!offer.isAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.favorite_not_available),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                offer.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatPesos(offer.discountedPrice),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatPesos(offer.originalPrice),
                        style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$discountText · $remainingText",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.offer_detail_about_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.offer_detail_about_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.offer_detail_pickup_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pickup card: flat surface card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = offer.merchantName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = offer.pickupWindow.formatted(withPickupPrefix = true),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (offer.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = offer.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Space so the floating reservation panel does not cover the content
                Spacer(modifier = Modifier.height(220.dp))
            }
        }

        // Soft backdrop that fades in once the content scrolls past the photo, so text
        // does not collide with the clock or the pinned buttons.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val start = 240.dp.toPx() // hero is 380dp tall; begin fading near its end
                    alpha = ((scrollState.value - start) / 100.dp.toPx()).coerceIn(0f, 1f)
                }
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        1f to Color.Transparent
                    )
                )
                .statusBarsPadding()
                .height(88.dp)
        )

        // Pinned controls: outside the scrolling column so they stay visible over photo and content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(AppSpacing.regular),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            CircleControl(
                text = "←",
                contentDescription = stringResource(R.string.common_back_cd),
                onClick = onBackClick
            )
            CircleControl(
                text = if (offer.isFavorite) "♥" else "♡",
                contentDescription = stringResource(
                    if (offer.isFavorite) R.string.common_favorite_remove_cd else R.string.common_favorite_add_cd
                ),
                tint = if (offer.isFavorite) RescateCoral else RescateCream,
                onClick = {
                    haptics.performHapticFeedback(
                        if (offer.isFavorite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                    )
                    if (animationsEnabled) scope.launch {
                        heartScale.animateTo(
                            1.35f,
                            AppMotion.bounceSpring()
                        )
                        heartScale.animateTo(1f, AppMotion.restSpring())
                    }
                    onFavoriteClick()
                },
                heartScale = heartScale
            )
        }
        }
    }
}

@Composable
private fun CircleControl(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = RescateCream,
    heartScale: Animatable<Float, *>? = null,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(RescateCarbon.copy(alpha = 0.55f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            color = tint,
            modifier = Modifier
                .graphicsLayer {
                    val s = heartScale?.value ?: 1f
                    scaleX = s
                    scaleY = s
                }
                .semantics { this.contentDescription = contentDescription }
        )
    }
}

/** What the reservation panel shows: the units chosen, what they cost, and what can be done. */
internal data class ReservePanelState(
    val quantity: Int,
    val totalPrice: Long,
    val canDecrease: Boolean,
    val canIncrease: Boolean,
    val canReserve: Boolean,
    val isReserving: Boolean
)

internal fun OfferDetailState.toPanel() = ReservePanelState(
    quantity = quantity,
    totalPrice = totalPrice,
    canDecrease = canDecrease,
    canIncrease = canIncrease,
    canReserve = canReserve,
    isReserving = reserve.isReserving
)

@Composable
private fun ReservationPanel(
    offer: OfferUiModel,
    reserve: ReservePanelState,
    onDecreaseQuantity: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onReserveClick: () -> Unit
) {
    val remainingText = pluralStringResource(R.plurals.offer_remaining_count, offer.remainingQuantity, offer.remainingQuantity)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(AppSpacing.regular)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatPesos(offer.discountedPrice),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                QuantityStepper(
                    quantity = reserve.quantity,
                    canDecrease = reserve.canDecrease,
                    canIncrease = reserve.canIncrease,
                    onDecrease = onDecreaseQuantity,
                    onIncrease = onIncreaseQuantity
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppButton(
                text = stringResource(R.string.offer_detail_reserve_total, formatPesos(reserve.totalPrice)),
                onClick = onReserveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = reserve.canReserve || reserve.isReserving,
                isLoading = reserve.isReserving
            )
        }
    }
}

/**
 * Minus, the count, plus. The count slides up when it grows and down when it shrinks, unless
 * the system animations are off. Screen readers hear the label and the count as one thing.
 */
@Composable
private fun QuantityStepper(
    quantity: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val haptics = LocalHapticFeedback.current
    val countDescription = pluralStringResource(R.plurals.offer_detail_quantity_cd, quantity, quantity)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.offer_detail_quantity_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {}
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                text = "−",
                contentDescription = stringResource(R.string.offer_detail_quantity_decrease_cd),
                enabled = canDecrease,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onDecrease()
                }
            )
            AnimatedContent(
                targetState = quantity,
                transitionSpec = {
                    if (!animationsEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val up = targetState > initialState
                        (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it else it } + fadeOut())
                    }
                },
                modifier = Modifier
                    .width(40.dp)
                    .semantics {
                        contentDescription = countDescription
                        liveRegion = LiveRegionMode.Polite
                    },
                contentAlignment = Alignment.Center,
                label = "quantity"
            ) { value ->
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            StepperButton(
                text = "+",
                contentDescription = stringResource(R.string.offer_detail_quantity_increase_cd),
                enabled = canIncrease,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onIncrease()
                }
            )
        }
    }
}

@Composable
private fun StepperButton(
    text: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.15f else 0.06f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}
