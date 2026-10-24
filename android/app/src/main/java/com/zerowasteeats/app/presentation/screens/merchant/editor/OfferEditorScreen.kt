package com.zerowasteeats.app.presentation.screens.merchant.editor

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.merchant.MerchantOfferDraft
import com.zerowasteeats.app.data.merchant.OfferPhoto
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppTextField
import com.zerowasteeats.app.presentation.components.common.EmptyState
import com.zerowasteeats.app.presentation.components.common.FormErrorBanner
import com.zerowasteeats.app.presentation.components.common.ModeBadge
import com.zerowasteeats.app.presentation.components.filters.CategoryChip
import com.zerowasteeats.app.presentation.format.PesosInputTransformation
import com.zerowasteeats.app.presentation.model.OfferCategory
import com.zerowasteeats.app.presentation.screens.auth.onFieldBlur
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/** Fields typed with the keyboard: on a failed save these take the focus. */
private val TEXT_FIELDS = setOf(
    OfferField.TITLE,
    OfferField.ORIGINAL_PRICE,
    OfferField.DISCOUNTED_PRICE,
    OfferField.QUANTITY
)

/**
 * Creates an offer when [offerId] is null, otherwise edits that offer. Full screen, without
 * the bottom bar. [onSaved] is called once the offer is saved; [onClose] leaves without saving.
 */
@Composable
fun OfferEditorScreen(
    session: Session.Merchant,
    offerId: String?,
    onClose: () -> Unit,
    onSaved: (OfferSaveResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val container =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val viewModel: OfferEditorViewModel = viewModel(
        factory = OfferEditorViewModelFactory(container.merchantRepositories(session).offers, offerId)
    )

    LaunchedEffect(viewModel) { viewModel.saved.collect(onSaved) }

    val existingOffer = viewModel.existingOffer
    if (offerId != null && existingOffer == null) {
        OfferNotFound(onClose = onClose, modifier = modifier)
        return
    }

    val clock = remember { Clock.systemDefaultZone() }
    val zone = clock.zone
    val form = rememberSaveable(saver = OfferEditorFormState.Saver) {
        existingOffer?.let { OfferEditorFormState.fromOffer(it, zone) }
            ?: OfferEditorFormState.newOffer(clock)
    }

    OfferEditorContent(
        form = form,
        isNew = offerId == null,
        isSaving = viewModel.isSaving,
        saveFailed = viewModel.notFound,
        onClose = onClose,
        // The pickup window is for today, for now
        onSave = { viewModel.save(form.toDraft(LocalDate.now(clock), zone, existingOffer)) },
        modifier = modifier,
        nowMinutes = { OfferEditorFormState.minutesOf(LocalTime.now(clock)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferEditorContent(
    form: OfferEditorFormState,
    isNew: Boolean,
    isSaving: Boolean,
    saveFailed: Boolean,
    onClose: () -> Unit,
    /** Called with a form that already passed validation. */
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    /** The time of day now, in minutes, so a pickup that already ended today is caught. */
    nowMinutes: () -> Int = { OfferEditorFormState.minutesOf(LocalTime.now()) }
) {
    var isDiscardDialogOpen by rememberSaveable { mutableStateOf(false) }
    var openTimePicker by rememberSaveable { mutableStateOf<OfferField?>(null) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequesters = remember { TEXT_FIELDS.associateWith { FocusRequester() } }
    val bringIntoView = remember { OfferField.entries.associateWith { BringIntoViewRequester() } }

    fun requestClose() {
        if (form.hasUnsavedChanges) isDiscardDialogOpen = true else onClose()
    }

    // Without changes Back simply leaves; with changes it asks first
    BackHandler(enabled = form.hasUnsavedChanges) { isDiscardDialogOpen = true }

    fun submit() {
        val invalid = form.validate(nowMinutes())
        if (invalid == null) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onSave()
            return
        }
        // A typed field takes the focus; a choice (category, photo, time) has nothing to type
        // into, so the keyboard closes and the screen scrolls to it
        val focus = focusRequesters[invalid]
        if (focus != null) focus.requestFocus() else focusManager.clearFocus()
        scope.launch { bringIntoView.getValue(invalid).bringIntoView() }
    }

    fun fieldModifier(field: OfferField): Modifier = Modifier
        .bringIntoViewRequester(bringIntoView.getValue(field))
        .then(focusRequesters[field]?.let { Modifier.focusRequester(it) } ?: Modifier)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            EditorTopBar(isNew = isNew, onBack = ::requestClose)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = AppSpacing.regular, vertical = AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraLarge)
            ) {
                AppTextField(
                    value = form.values.title,
                    onValueChange = form::onTitleChange,
                    label = stringResource(R.string.offer_field_title),
                    modifier = fieldModifier(OfferField.TITLE)
                        .onFieldBlur { form.onBlur(OfferField.TITLE) },
                    error = form.errorOf(OfferField.TITLE)?.message(),
                    imeAction = ImeAction.Next
                )

                CategorySection(form = form, modifier = fieldModifier(OfferField.CATEGORY))

                PhotoSection(form = form, modifier = fieldModifier(OfferField.PHOTO))

                PriceSection(form = form, fieldModifier = ::fieldModifier)

                QuantitySection(
                    form = form,
                    modifier = Modifier.bringIntoViewRequester(bringIntoView.getValue(OfferField.QUANTITY)),
                    fieldModifier = focusRequesters[OfferField.QUANTITY]
                        ?.let { Modifier.focusRequester(it) } ?: Modifier,
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )

                PickupSection(
                    form = form,
                    startBringIntoView = bringIntoView.getValue(OfferField.PICKUP_START),
                    endBringIntoView = bringIntoView.getValue(OfferField.PICKUP_END),
                    onPick = { field ->
                        focusManager.clearFocus()
                        openTimePicker = field
                    }
                )
            }

            // Outside the scrolling column, so it stays reachable above the keyboard
            Column(
                modifier = Modifier.padding(AppSpacing.regular),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                FormErrorBanner(
                    message = if (saveFailed) stringResource(R.string.offer_editor_not_found_message) else null
                )
                AppButton(
                    text = stringResource(
                        if (isNew) R.string.merchant_publish_offer else R.string.offer_editor_save_changes
                    ),
                    onClick = ::submit,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isSaving
                )
            }
        }
    }

    openTimePicker?.let { field ->
        // A new picker per field, so it opens on that field's own time
        key(field) {
            PickupTimeDialog(
                field = field,
                form = form,
                onDismiss = { openTimePicker = null }
            )
        }
    }

    if (isDiscardDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDiscardDialogOpen = false },
            title = { Text(text = stringResource(R.string.offer_editor_discard_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDiscardDialogOpen = false
                        onClose()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.offer_editor_discard),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isDiscardDialogOpen = false }) {
                    Text(
                        text = stringResource(R.string.offer_editor_keep_editing),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            containerColor = SurfaceTokens.surface
        )
    }
}

@Composable
private fun EditorTopBar(isNew: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.small, vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.common_back_cd),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column {
            ModeBadge()
            Text(
                text = stringResource(
                    if (isNew) R.string.merchant_new_offer else R.string.offer_editor_title_edit
                ),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/** A labelled choice with its error under it, for the fields that are not typed. */
@Composable
private fun ChoiceSection(
    @StringRes labelRes: Int,
    error: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(bottom = AppSpacing.small),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
        FieldError(error)
    }
}

@Composable
private fun FieldError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            modifier = Modifier
                .padding(top = AppSpacing.extraSmall)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(form: OfferEditorFormState, modifier: Modifier) {
    ChoiceSection(
        labelRes = R.string.offer_field_category,
        error = form.errorOf(OfferField.CATEGORY)?.let { stringResource(R.string.offer_error_choose) },
        modifier = modifier
    ) {
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            OfferCategory.entries.filter { it != OfferCategory.ALL }.forEach { category ->
                val isSelected = form.values.category == category
                CategoryChip(
                    text = stringResource(category.labelRes),
                    isSelected = isSelected,
                    onClick = { form.onCategorySelected(category) },
                    modifier = Modifier.semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    }
                )
            }
        }
    }
}

// TEMPORARY: picking one of the app's photos stands in for uploading a real one.
@Composable
private fun PhotoSection(form: OfferEditorFormState, modifier: Modifier) {
    ChoiceSection(
        labelRes = R.string.offer_field_photo,
        error = form.errorOf(OfferField.PHOTO)?.let { stringResource(R.string.offer_error_choose) },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            OfferPhoto.entries.forEach { photo ->
                PhotoOption(
                    photo = photo,
                    isSelected = form.values.photo == photo,
                    onClick = { form.onPhotoSelected(photo) }
                )
            }
        }
    }
}

@Composable
private fun PhotoOption(photo: OfferPhoto, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.medium)
    val description = stringResource(photoNameRes(photo))
    Surface(
        modifier = Modifier
            .size(84.dp)
            .clip(shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        shape = shape,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box {
            Image(
                painter = painterResource(photo.drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                // Plain butter is too faint on cream to mark the choice alone
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacing.extraSmall)
                        .size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.padding(AppSpacing.extraSmall),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceSection(form: OfferEditorFormState, fieldModifier: (OfferField) -> Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.regular)) {
        AppTextField(
            value = form.values.originalPrice,
            onValueChange = form::onOriginalPriceChange,
            label = stringResource(R.string.offer_field_original_price),
            modifier = fieldModifier(OfferField.ORIGINAL_PRICE)
                .onFieldBlur { form.onBlur(OfferField.ORIGINAL_PRICE) },
            error = form.errorOf(OfferField.ORIGINAL_PRICE)?.message(),
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
            visualTransformation = PesosInputTransformation
        )
        Column {
            AppTextField(
                value = form.values.discountedPrice,
                onValueChange = form::onDiscountedPriceChange,
                label = stringResource(R.string.offer_field_discounted_price),
                modifier = fieldModifier(OfferField.DISCOUNTED_PRICE)
                    .onFieldBlur { form.onBlur(OfferField.DISCOUNTED_PRICE) },
                error = form.errorOf(OfferField.DISCOUNTED_PRICE)?.message(),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                visualTransformation = PesosInputTransformation
            )
            form.discountPercent?.let { percent ->
                Text(
                    text = stringResource(R.string.offer_discount_off, percent),
                    modifier = Modifier
                        .padding(top = AppSpacing.small)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuantitySection(
    form: OfferEditorFormState,
    modifier: Modifier,
    fieldModifier: Modifier,
    onDone: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            OutlinedIconButton(
                onClick = form::decrementQuantity,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_remove),
                    contentDescription = stringResource(R.string.offer_quantity_decrease_cd)
                )
            }
            AppTextField(
                value = form.values.quantity,
                onValueChange = form::onQuantityChange,
                label = stringResource(R.string.offer_field_quantity),
                modifier = Modifier
                    .weight(1f)
                    .then(fieldModifier)
                    .onFieldBlur { form.onBlur(OfferField.QUANTITY) },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onDone() })
            )
            OutlinedIconButton(
                onClick = form::incrementQuantity,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.offer_quantity_increase_cd)
                )
            }
        }
        FieldError(form.errorOf(OfferField.QUANTITY)?.message())
    }
}

@Composable
private fun PickupSection(
    form: OfferEditorFormState,
    /** Scrolls to the start time when it is the first field with an error. */
    startBringIntoView: BringIntoViewRequester,
    /** Scrolls to the end time when it is the first field with an error. */
    endBringIntoView: BringIntoViewRequester,
    onPick: (OfferField) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.offer_field_pickup),
            modifier = Modifier.padding(bottom = AppSpacing.small),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            TimeField(
                labelRes = R.string.offer_field_pickup_start,
                time = form.values.pickupStartMinutes?.let {
                    formatter.format(OfferEditorFormState.timeOf(it))
                },
                error = form.errorOf(OfferField.PICKUP_START)?.message(),
                onClick = { onPick(OfferField.PICKUP_START) },
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(startBringIntoView)
            )
            TimeField(
                labelRes = R.string.offer_field_pickup_end,
                time = form.values.pickupEndMinutes?.let {
                    formatter.format(OfferEditorFormState.timeOf(it))
                },
                error = form.errorOf(OfferField.PICKUP_END)?.message(),
                onClick = { onPick(OfferField.PICKUP_END) },
                modifier = Modifier
                    .weight(1f)
                    .bringIntoViewRequester(endBringIntoView)
            )
        }
    }
}

@Composable
private fun TimeField(
    @StringRes labelRes: Int,
    time: String?,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppRadius.large)
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick),
            shape = shape,
            color = SurfaceTokens.surface,
            border = BorderStroke(
                1.dp,
                if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = AppSpacing.regular, vertical = AppSpacing.small)) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = time ?: stringResource(R.string.offer_pickup_choose_time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (time != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (time != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        FieldError(error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupTimeDialog(
    field: OfferField,
    form: OfferEditorFormState,
    onDismiss: () -> Unit
) {
    val values = form.values
    val current = if (field == OfferField.PICKUP_START) values.pickupStartMinutes else values.pickupEndMinutes
    // With no time yet, start in the evening, and the end an hour after the start
    val initial = current ?: when (field) {
        OfferField.PICKUP_END -> ((values.pickupStartMinutes ?: DEFAULT_START_MINUTES) + 60)
            .coerceAtMost(OfferEditorFormState.LAST_MINUTE_OF_DAY)
        else -> DEFAULT_START_MINUTES
    }
    val state = rememberTimePickerState(
        initialHour = initial / 60,
        initialMinute = initial % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (field == OfferField.PICKUP_START) R.string.offer_field_pickup_start
                    else R.string.offer_field_pickup_end
                )
            )
        },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = state.hour * 60 + state.minute
                    if (field == OfferField.PICKUP_START) form.onPickupStartSelected(minutes)
                    else form.onPickupEndSelected(minutes)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.common_ok), color = MaterialTheme.colorScheme.tertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel), color = MaterialTheme.colorScheme.tertiary)
            }
        },
        containerColor = SurfaceTokens.surface
    )
}

@Composable
private fun OfferNotFound(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EditorTopBar(isNew = false, onBack = onClose)
            Spacer(modifier = Modifier.height(AppSpacing.section))
            EmptyState(
                title = stringResource(R.string.navigation_offer_not_found),
                message = stringResource(R.string.offer_editor_not_found_message)
            )
        }
    }
}

@Composable
private fun OfferFieldError.message(): String = when (this) {
    OfferFieldError.REQUIRED -> stringResource(R.string.validation_required)
    OfferFieldError.TITLE_TOO_SHORT -> pluralStringResource(
        R.plurals.validation_name_too_short,
        OfferEditorFormState.MIN_TITLE_LENGTH,
        OfferEditorFormState.MIN_TITLE_LENGTH
    )
    OfferFieldError.TITLE_TOO_LONG -> pluralStringResource(
        R.plurals.validation_name_too_long,
        OfferEditorFormState.MAX_TITLE_LENGTH,
        OfferEditorFormState.MAX_TITLE_LENGTH
    )
    OfferFieldError.PRICE_NOT_POSITIVE -> stringResource(R.string.offer_error_price_not_positive)
    OfferFieldError.DISCOUNT_NOT_LOWER -> stringResource(R.string.offer_error_discount_not_lower)
    OfferFieldError.QUANTITY_OUT_OF_RANGE -> stringResource(
        R.string.offer_error_quantity_range,
        OfferEditorFormState.MIN_QUANTITY,
        OfferEditorFormState.MAX_QUANTITY
    )
    OfferFieldError.END_NOT_AFTER_START -> stringResource(R.string.offer_error_end_not_after_start)
    OfferFieldError.END_IN_PAST -> stringResource(R.string.offer_error_end_in_past)
}

private fun photoNameRes(photo: OfferPhoto): Int = when (photo) {
    OfferPhoto.CROISSANT -> R.string.offer_photo_croissant
    OfferPhoto.SALMON_BOWL -> R.string.offer_photo_salmon_bowl
    OfferPhoto.DESSERTS -> R.string.offer_photo_desserts
    OfferPhoto.BURGER -> R.string.offer_photo_burger
    OfferPhoto.VEGETABLES -> R.string.offer_photo_vegetables
}

private const val DEFAULT_START_MINUTES = 18 * 60

@Composable
private fun EditorPreviewContent(themeMode: ThemeMode, withErrors: Boolean) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        val form = remember {
            OfferEditorFormState.newOffer().apply {
                if (withErrors) {
                    onTitleChange("Pa")
                    onCategorySelected(OfferCategory.BAKERY)
                    onOriginalPriceChange("15000")
                    onDiscountedPriceChange("16000")
                    onPickupStartSelected(19 * 60)
                    onPickupEndSelected(18 * 60)
                    validate()
                } else {
                    onTitleChange("Croissants del día")
                    onCategorySelected(OfferCategory.BAKERY)
                    onPhotoSelected(OfferPhoto.CROISSANT)
                    onOriginalPriceChange("15000")
                    onDiscountedPriceChange("8900")
                    onPickupStartSelected(18 * 60)
                    onPickupEndSelected(19 * 60 + 30)
                }
            }
        }
        OfferEditorContent(
            form = form,
            isNew = true,
            isSaving = false,
            saveFailed = false,
            onClose = {},
            onSave = {},
            // Fixed, so the previews do not depend on the time they are rendered
            nowMinutes = { 9 * 60 }
        )
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun EditorLightPreview() {
    EditorPreviewContent(ThemeMode.LIGHT, withErrors = false)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditorDarkPreview() {
    EditorPreviewContent(ThemeMode.DARK, withErrors = false)
}

@Preview(name = "Errors light", widthDp = 380, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun EditorErrorsLightPreview() {
    EditorPreviewContent(ThemeMode.LIGHT, withErrors = true)
}

@Preview(name = "Errors dark", widthDp = 380, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditorErrorsDarkPreview() {
    EditorPreviewContent(ThemeMode.DARK, withErrors = true)
}
