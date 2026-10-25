package com.zerowasteeats.app.presentation.screens.welcome

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.brand.ZeroWasteLogo
import com.zerowasteeats.app.presentation.components.common.ENTRANCE_MILLIS
import com.zerowasteeats.app.presentation.components.common.HALO_BREATH_MILLIS
import com.zerowasteeats.app.presentation.components.common.LOGO_TOTAL_MILLIS
import com.zerowasteeats.app.presentation.components.common.STAGGER_MILLIS
import com.zerowasteeats.app.presentation.components.common.entranceRise
import com.zerowasteeats.app.presentation.components.common.rememberAnimationsEnabled
import com.zerowasteeats.app.presentation.components.settings.CompactLanguageSelector
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.RescateButter
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ThemePreferences
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import com.zerowasteeats.app.presentation.theme.isAppInDarkTheme
import kotlinx.coroutines.delay

/** First screen for someone signed out: the pitch, plus the way into sign up or sign in. */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // The column is at least as tall as the screen, so the spacer pushes the buttons to
        // the bottom, and it scrolls when large text or a small screen needs more room
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.extraLarge, vertical = AppSpacing.regular)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            ThemePreferences.setMode(
                                context,
                                if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            )
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                            ),
                            contentDescription = stringResource(
                                if (isDark) R.string.welcome_switch_to_light_cd
                                else R.string.welcome_switch_to_dark_cd
                            ),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    CompactLanguageSelector()
                }

                // The logo, the name and the headline sit in the middle of the room left between
                // the top row and the buttons, so no big gap opens above or below them
                Spacer(modifier = Modifier.weight(1f).heightIn(min = AppSpacing.large))
                BrandHero(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    background = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

                // The texts come in after the logo has played, one after the other; once seen
                // (and after a rotation) they are simply there
                var introSeen by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay((LOGO_TOTAL_MILLIS + 3 * STAGGER_MILLIS + ENTRANCE_MILLIS).toLong())
                    introSeen = true
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier
                            .fillMaxWidth()
                            .entranceRise(
                                rise = 8.dp,
                                delayMillis = LOGO_TOTAL_MILLIS,
                                enabled = !introSeen
                            ),
                        // Brand accent: 5.9:1 on cream in light mode, butter on carbon in dark
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Text(
                        text = stringResource(R.string.welcome_headline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .entranceRise(
                                rise = 8.dp,
                                delayMillis = LOGO_TOTAL_MILLIS + 2 * STAGGER_MILLIS,
                                enabled = !introSeen
                            ),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f).heightIn(min = AppSpacing.section))

                Column(
                    modifier = Modifier.fillMaxWidth().entranceRise(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    AppButton(
                        text = stringResource(R.string.welcome_create_account),
                        onClick = onCreateAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppButton(
                        text = stringResource(R.string.welcome_sign_in),
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        style = AppButtonStyle.SECONDARY
                    )
                }
            }
        }
    }
}

/**
 * The animated logo with a soft butter halo behind it that breathes very slowly. The halo is a
 * flat translucent circle, so the leaf cut-out is the background with the halo over it. Still
 * when system animations are off.
 */
@Composable
private fun BrandHero(background: Color, modifier: Modifier = Modifier) {
    val breathe = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val haloScale = if (breathe) {
        val transition = rememberInfiniteTransition(label = "haloBreath")
        val value by transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(HALO_BREATH_MILLIS / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "haloScale"
        )
        value
    } else {
        1f
    }
    val halo = RescateButter.copy(alpha = HALO_ALPHA)

    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .widthIn(max = 300.dp)
            .aspectRatio(1f)
            // Decorative: the name under it says what the app is
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2 * haloScale
            drawCircle(color = halo, radius = radius)
        }
        ZeroWasteLogo(
            animated = true,
            // The drawing leaves margin inside its square; at 0.95 the symbol is about 60% of the
            // halo wide and 68% tall, and its farthest corner is still inside the circle
            modifier = Modifier.fillMaxSize(0.95f),
            cutoutColor = halo.compositeOver(background)
        )
    }
}

private const val HALO_ALPHA = 0.09f

@Composable
private fun WelcomeScreenPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        WelcomeScreen(onCreateAccount = {}, onSignIn = {})
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun WelcomeScreenLightPreview() {
    WelcomeScreenPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenDarkPreview() {
    WelcomeScreenPreviewContent(ThemeMode.DARK)
}
