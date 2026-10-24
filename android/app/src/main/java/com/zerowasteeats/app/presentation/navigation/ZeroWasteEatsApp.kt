package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled

/**
 * Root of the app's UI: shows the space that matches the current session, and switches by
 * itself when the session changes (sign in, sign up, sign out). Nothing navigates between
 * spaces by hand.
 */
@Composable
fun ZeroWasteEatsApp() {
    val sessionManager =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication)
            .container.sessionManager
    val session by sessionManager.session
    val animationsEnabled = rememberAnimationsEnabled()

    // Behind every space, so the fade between two never shows anything but the theme
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = appSpaceFor(session),
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(ENTRANCE_MILLIS)) togetherWith fadeOut(tween(ENTRANCE_MILLIS))
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            // A new key drops the previous content with all its state and back stack
            contentKey = { it.key },
            label = "appSpace"
        ) { space ->
            when (space) {
                AppSpace.Loading -> Box(modifier = Modifier.fillMaxSize())
                AppSpace.Auth -> AuthNavigation()
                is AppSpace.Customer -> CustomerNavigation(
                    session = space.session,
                    onSignOut = sessionManager::signOut
                )
                is AppSpace.Merchant -> MerchantNavigation(
                    session = space.session,
                    onSignOut = sessionManager::signOut
                )
            }
        }
    }
}
