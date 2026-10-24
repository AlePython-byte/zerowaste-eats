package com.zerowasteeats.app.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
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
    val animationsEnabled = rememberAnimationsEnabled()

    // A short slide toward the side the screen comes from, or an instant swap when the
    // system animations are off
    val enter = if (animationsEnabled) {
        slideInHorizontally(tween(ENTRANCE_MILLIS)) { width -> width / 4 } +
            fadeIn(tween(ENTRANCE_MILLIS))
    } else {
        EnterTransition.None
    }
    val exit = if (animationsEnabled) {
        slideOutHorizontally(tween(ENTRANCE_MILLIS)) { width -> -width / 4 } +
            fadeOut(tween(ENTRANCE_MILLIS))
    } else {
        ExitTransition.None
    }
    val popEnter = if (animationsEnabled) {
        slideInHorizontally(tween(ENTRANCE_MILLIS)) { width -> -width / 4 } +
            fadeIn(tween(ENTRANCE_MILLIS))
    } else {
        EnterTransition.None
    }
    val popExit = if (animationsEnabled) {
        slideOutHorizontally(tween(ENTRANCE_MILLIS)) { width -> width / 4 } +
            fadeOut(tween(ENTRANCE_MILLIS))
    } else {
        ExitTransition.None
    }

    NavHost(
        navController = navController,
        startDestination = AuthDestination.WELCOME.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { popEnter },
        popExitTransition = { popExit }
    ) {
        composable(AuthDestination.WELCOME.route) {
            WelcomeScreen(
                onCreateAccount = { navController.navigate(AuthDestination.SIGN_UP.route) },
                onSignIn = { navController.navigate(AuthDestination.SIGN_IN.route) }
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
