package com.zerowasteeats.app.presentation.components.glass

import android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.GlassTokens

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    onClearClick: () -> Unit = { onQueryChange("") }
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Slightly more translucent for a refined look
    val backgroundColor = if (isDarkTheme) {
        GlassTokens.darkSurface.copy(alpha = GlassTokens.darkSurface.alpha * 0.85f) 
    } else {
        GlassTokens.lightSurface.copy(alpha = GlassTokens.lightSurface.alpha * 0.90f)
    }
    
    val borderColor = if (isDarkTheme) GlassTokens.darkBorder else GlassTokens.lightBorder

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(AppRadius.large),
        color = backgroundColor,
        border = BorderStroke(GlassTokens.borderWidth, borderColor),
        shadowElevation = GlassTokens.elevation / 1.5f // Softened shadow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.regular)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(AppSpacing.medium))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = AppSpacing.regular),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .semantics {
                            contentDescription = clearContentDescription
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = null, // semantics are on the IconButton
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
