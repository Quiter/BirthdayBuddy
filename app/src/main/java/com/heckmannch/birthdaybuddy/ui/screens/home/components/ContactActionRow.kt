package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.util.ContactActions

@Composable
fun ContactActionRow(
    contactId: String,
    lookupKey: String,
    phoneNumber: String?,
    hasWhatsApp: Boolean,
    hasSignal: Boolean,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val actions = remember(context) { ContactActions(context) }

    Row(
        modifier = modifier
            .padding(start = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kontakt öffnen (Blau)
        ActionIcon(Icons.Default.Person, Color(0xFF2196F3)) {
            onOpenContact(contactId, lookupKey)
        }

        if (!phoneNumber.isNullOrBlank()) {
            // Anrufen
            ActionIcon(Icons.Default.Call, Color(0xFF4CAF50)) {
                actions.dialNumber(phoneNumber)
            }
            // SMS
            ActionIcon(Icons.AutoMirrored.Filled.Message, Color(0xFF2196F3)) {
                actions.sendSms(phoneNumber)
            }

            // WhatsApp (Nur wenn verfügbar)
            if (hasWhatsApp) {
                ActionIcon(
                    icon = painterResource(R.drawable.ic_whatsapp),
                    color = Color(0xFF25D366),
                    onClick = { actions.openWhatsApp(phoneNumber) }
                )
            }

            // Signal (Nur wenn verfügbar)
            if (hasSignal) {
                ActionIcon(
                    icon = painterResource(R.drawable.ic_signal),
                    color = Color(0xFF3A76F0),
                    onClick = { actions.openSignal(phoneNumber) }
                )
            }
        }
    }
}

@Composable
private fun ActionIcon(
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ActionIcon(color = color, onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.painter.Painter,
    color: Color,
    onClick: () -> Unit
) {
    ActionIcon(color = color, onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
