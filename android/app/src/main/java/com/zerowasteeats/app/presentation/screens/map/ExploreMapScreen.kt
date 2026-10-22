package com.zerowasteeats.app.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.components.map.MapCircleButton
import com.zerowasteeats.app.presentation.components.map.OffersMap
import com.zerowasteeats.app.presentation.data.MockOffers
import com.zerowasteeats.app.presentation.theme.AppSpacing
import kotlinx.coroutines.launch

@Composable
fun ExploreMapScreen(
    onBackClick: () -> Unit,
    onOfferClick: (String) -> Unit
) {
    var selectedOfferId by rememberSaveable { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val offers = remember { MockOffers.getList() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            OffersMap(
                offers = offers,
                selectedOfferId = selectedOfferId,
                onSelectOffer = { selectedOfferId = it },
                onOfferClick = onOfferClick,
                onLocationMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(AppSpacing.regular)
            ) {
                MapCircleButton(
                    contentDescription = stringResource(R.string.common_back_cd),
                    onClick = onBackClick
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
