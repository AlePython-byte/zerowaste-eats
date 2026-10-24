package com.zerowasteeats.app.presentation.screens.welcome

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.common.AppButton
import com.zerowasteeats.app.presentation.components.common.AppButtonStyle
import com.zerowasteeats.app.presentation.components.common.entranceRise
import com.zerowasteeats.app.presentation.components.settings.CompactLanguageSelector
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ThemePreferences
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme
import com.zerowasteeats.app.presentation.theme.isAppInDarkTheme

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
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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

                Column(modifier = Modifier.fillMaxWidth().entranceRise()) {
                    Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                    Text(
                        text = stringResource(R.string.welcome_headline),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                    Image(
                        painter = painterResource(R.drawable.offer_croissant),
                        // Decorative: the headline already says what the app is about
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(AppRadius.extraLarge))
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
