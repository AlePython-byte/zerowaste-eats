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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.data.settings.DataMode
import com.zerowasteeats.app.presentation.components.common.DemoModeBadge
import com.zerowasteeats.app.presentation.components.common.ServerWakingBanner
import com.zerowasteeats.app.data.remote.ServerWakeUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.presentation.components.common.AppMotion
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled

/**
 * Root of the app's UI: shows the space that matches the current session, and switches by
 * itself when the session changes (sign in, sign up, sign out). Nothing navigates between
 * spaces by hand.
 */
@Composable
fun ZeroWasteEatsApp() {
    val container = (LocalContext.current.applicationContext as ZeroWasteEatsApplication).container
    val sessionManager = container.sessionManager
    val session by sessionManager.session
    val dataMode by container.dataMode.mode
    val serverWaitingSince by container.serverWaitingSince.collectAsState()

    // A sleeping deployed server starts while the person signs in, not after
    LaunchedEffect(dataMode) { container.wakeServer() }
    val animationsEnabled = rememberAnimationsEnabled()

    // Behind every space, so the fade between two never shows anything but the theme
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = appSpaceFor(session),
                transitionSpec = {
                    if (animationsEnabled) {
                        fadeIn(tween(AppMotion.MEDIUM_MILLIS)) togetherWith fadeOut(tween(AppMotion.MEDIUM_MILLIS))
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

            // Over every signed-in screen while the data is the demo's (sign in already says so)
            if (showsDemoBadge(session, dataMode)) {
                DemoModeBadge(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 4.dp)
                )
            }

            // Live mode only: demo data never waits for a server
            ServerWakingBanner(
                waitingSince = serverWaitingSince,
                slowAfterMillis = ServerWakeUp.SLOW_AFTER_MILLIS,
                nowMillis = System::currentTimeMillis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            )
        }
    }
}

/** Signed in on demo data: the badge says so, so it is never mistaken for the real thing. */
internal fun showsDemoBadge(session: Session, mode: DataMode): Boolean =
    mode == DataMode.DEMO && (session is Session.Customer || session is Session.Merchant)
