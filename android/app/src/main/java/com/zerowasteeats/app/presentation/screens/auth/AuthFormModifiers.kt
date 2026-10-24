package com.zerowasteeats.app.presentation.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Calls [onBlur] when the field had focus and loses it. Having no focus at first does not
 * count, so a field that is merely shown (or redrawn after a rotation) is never "left".
 */
@Composable
fun Modifier.onFieldBlur(onBlur: () -> Unit): Modifier {
    var hadFocus by remember { mutableStateOf(false) }
    val currentOnBlur by rememberUpdatedState(onBlur)
    return onFocusChanged { state ->
        if (hadFocus && !state.isFocused) currentOnBlur()
        hadFocus = state.isFocused
    }
}
