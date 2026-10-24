package com.zerowasteeats.app.presentation.components.auth

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zerowasteeats.app.R
import com.zerowasteeats.app.presentation.theme.AppRadius
import com.zerowasteeats.app.presentation.theme.AppSpacing
import com.zerowasteeats.app.presentation.theme.SurfaceTokens
import com.zerowasteeats.app.presentation.theme.ThemeMode
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

/**
 * One choice of a radio group, drawn as a large card: icon, title and a line of text. The
 * chosen card gets a butter border and a check. Put the cards in a column with
 * [Modifier.selectableGroup] so a screen reader announces them as options of one group.
 */
@Composable
fun RoleOptionCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppRadius.large)
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(SurfaceTokens.borderWidth, SurfaceTokens.border)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = SurfaceTokens.surface,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.regular),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.regular)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                // Plain butter is too faint on cream to mark the choice alone
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun RoleOptionCardPreviewContent(themeMode: ThemeMode) {
    ZeroWasteEatsTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .padding(AppSpacing.regular)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                RoleOptionCard(
                    title = "Quiero rescatar comida",
                    description = "Encuentra ofertas cerca de ti a buen precio.",
                    icon = R.drawable.ic_role_customer,
                    selected = true,
                    onClick = {}
                )
                RoleOptionCard(
                    title = "Tengo un negocio",
                    description = "Publica tus excedentes y llega a más clientes.",
                    icon = R.drawable.ic_role_merchant,
                    selected = false,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun RoleOptionCardLightPreview() {
    RoleOptionCardPreviewContent(ThemeMode.LIGHT)
}

@Preview(name = "Dark", widthDp = 380, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoleOptionCardDarkPreview() {
    RoleOptionCardPreviewContent(ThemeMode.DARK)
}
