package com.zerowasteeats.app.presentation.model

import androidx.annotation.StringRes
import com.zerowasteeats.app.R

enum class OfferCategory(@StringRes val labelRes: Int) {
    // Filter-only value; no offer has this category.
    ALL(R.string.category_all),
    BAKERY(R.string.category_bakery),
    PREPARED_FOOD(R.string.category_prepared_food),
    DESSERTS(R.string.category_desserts)
}
