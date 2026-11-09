package com.zerowasteeats.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import com.zerowasteeats.app.ZeroWasteEatsApplication
import com.zerowasteeats.app.data.session.SessionNotice
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zerowasteeats.app.presentation.screens.forgotpassword.ForgotPasswordScreen
import com.zerowasteeats.app.presentation.screens.signin.SignInScreen
import com.zerowasteeats.app.presentation.screens.signup.SignUpScreen
import com.zerowasteeats.app.presentation.screens.welcome.WelcomeScreen

enum class AuthDestination(val route: String) {
    WELCOME("welcome"),
    SIGN_IN("signin"),
    SIGN_UP("signup"),
    FORGOT_PASSWORD("forgot_password")
}

/**
 * The signed-out space: its own nav controller and graph, with no bottom bar. Like
 * [CustomerNavigation] everything is remembered here, so dropping it discards the back stack.
 */
@Composable
fun AuthNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val sessionManager =
        (LocalContext.current.applicationContext as ZeroWasteEatsApplication)
            .container.sessionManager
    val notice by sessionManager.notice
    // No tabs here: every screen slides on the shared axis, and an instant swap when the
    // system animations are off
    val transitions = rememberNavTransitions(tabRoutes = emptySet())

    NavHost(
        navController = navController,
        startDestination = AuthDestination.WELCOME.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = transitions.enter,
        exitTransition = transitions.exit,
        popEnterTransition = transitions.popEnter,
        popExitTransition = transitions.popExit,
        predictivePopEnterTransition = transitions.predictivePopEnter,
        predictivePopExitTransition = transitions.predictivePopExit
    ) {
        composable(AuthDestination.WELCOME.route) {
            WelcomeScreen(
                onCreateAccount = {
                    sessionManager.dismissNotice()
                    navController.navigate(AuthDestination.SIGN_UP.route)
                },
                onSignIn = {
                    sessionManager.dismissNotice()
                    navController.navigate(AuthDestination.SIGN_IN.route)
                },
                notice = when (notice) {
                    SessionNotice.SESSION_EXPIRED -> stringResource(R.string.session_expired)
                    null -> null
                }
            )
        }
        composable(AuthDestination.SIGN_IN.route) {
            SignInScreen(
                onForgotPassword = {
                    navController.navigate(AuthDestination.FORGOT_PASSWORD.route)
                },
                onSignUp = { navController.switchTo(AuthDestination.SIGN_UP) }
            )
        }
        composable(AuthDestination.SIGN_UP.route) {
            SignUpScreen(onSignIn = { navController.switchTo(AuthDestination.SIGN_IN) })
        }
        composable(AuthDestination.FORGOT_PASSWORD.route) {
            // Only reachable from sign in, so going back lands there
            ForgotPasswordScreen(onBackToSignIn = { navController.popBackStack() })
        }
    }
}

/**
 * Sign in and sign up link to each other. Swapping one for the other keeps Back going to the
 * welcome screen, instead of piling up a chain of the two.
 */
private fun NavHostController.switchTo(destination: AuthDestination) {
    navigate(destination.route) {
        popUpTo(AuthDestination.WELCOME.route)
        launchSingleTop = true
    }
}
