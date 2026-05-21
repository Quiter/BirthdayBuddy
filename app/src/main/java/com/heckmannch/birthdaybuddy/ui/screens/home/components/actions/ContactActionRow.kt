package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@Composable
fun ContactActionRow(
    contactId: String,
    lookupKey: String,
    phoneNumber: String?,
    hasWhatsApp: Boolean,
    hasSignal: Boolean,
    hasBirthday: Boolean,
    onAddBirthday: () -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kontakt-Details öffnen
        ActionItem(
            icon = Icons.Default.Person,
            label = stringResource(R.string.item_action_contact),
            onClick = { actions.onOpenContact(contactId, lookupKey) }
        )

        if (!hasBirthday) {
            ActionItem(
                icon = Icons.Default.Add,
                label = stringResource(R.string.item_action_edit_birthday),
                onClick = onAddBirthday
            )
        }

        if (phoneNumber != null) {
            // Anrufen
            ActionItem(
                icon = Icons.Default.Call,
                label = stringResource(R.string.item_action_call),
                onClick = { actions.onDial(phoneNumber) }
            )

            // SMS
            ActionItem(
                icon = Icons.AutoMirrored.Filled.Message,
                label = stringResource(R.string.item_action_sms),
                onClick = { actions.onSendSms(phoneNumber) }
            )

            if (hasWhatsApp) {
                ActionItem(
                    painter = painterResource(R.drawable.ic_whatsapp),
                    label = stringResource(R.string.item_action_whatsapp),
                    onClick = { actions.onWhatsApp(phoneNumber) }
                )
            }

            if (hasSignal) {
                ActionItem(
                    painter = painterResource(R.drawable.ic_signal),
                    label = stringResource(R.string.item_action_signal),
                    onClick = { actions.onSignal(phoneNumber) }
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
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
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
        onWhatsApp = {},
        onSignal = {},
        onRefresh = {}
    )
    BirthdayBuddyTheme {
        ContactActionRow(
            contactId = "1",
            lookupKey = "k1",
            phoneNumber = "123",
            hasWhatsApp = true,
            hasSignal = true,
            hasBirthday = false,
            onAddBirthday = {},
            actions = actions
        )
    }
}
