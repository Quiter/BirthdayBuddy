package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

private val PixelBlue = Color(0xFF1A73E8)

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ContactActionRow(
    contactId: String,
    lookupKey: String,
    phoneNumber: String?,
    hasBirthday: Boolean,
    onAddBirthday: () -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val installedMessengers = remember(context) {
        MessengerApp.getInstalledMessengers(context)
            .sortedBy { it.name }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Kontakt-Details öffnen (Immer an 1. Stelle)
        ActionItem(
            icon = Icons.Default.Person,
            label = stringResource(R.string.item_action_contact),
            brandColor = PixelBlue,
            onClick = { actions.onOpenContact(contactId, lookupKey) }
        )

        if (phoneNumber != null) {
            // 2. Anrufen (An 2. Stelle)
            ActionItem(
                icon = Icons.Default.Call,
                label = stringResource(R.string.item_action_call),
                brandColor = PixelBlue,
                onClick = { actions.onDial(phoneNumber) }
            )

            // 3. SMS (An 3. Stelle)
            ActionItem(
                icon = Icons.AutoMirrored.Filled.Message,
                label = stringResource(R.string.item_action_sms),
                brandColor = PixelBlue,
                onClick = { actions.onSendSms(phoneNumber) }
            )
        }

        // 4. Geburtstag hinzufügen/bearbeiten
        if (!hasBirthday) {
            ActionItem(
                icon = Icons.Default.Add,
                label = stringResource(R.string.item_action_edit_birthday),
                brandColor = PixelBlue,
                onClick = onAddBirthday
            )
        }

        if (phoneNumber != null) {
            // 5. Alle installierten Messenger-Aktionen alphabetisch geordnet
            installedMessengers.forEach { app ->
                ActionItem(
                    painter = painterResource(app.iconResId),
                    label = stringResource(app.labelResId),
                    brandColor = app.brandColor,
                    onClick = { actions.onOpenMessengerApp(app, phoneNumber) }
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    brandColor: Color,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = brandColor.copy(alpha = 0.15f),
            contentColor = brandColor
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactActionRowPreview() {
    val actions = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {}
    )
    BirthdayBuddyTheme {
        ContactActionRow(
            contactId = "1",
            lookupKey = "k1",
            phoneNumber = "123",
            hasBirthday = false,
            onAddBirthday = {},
            actions = actions
        )
    }
}
