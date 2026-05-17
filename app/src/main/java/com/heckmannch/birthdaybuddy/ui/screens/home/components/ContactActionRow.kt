package com.heckmannch.birthdaybuddy.ui.screens.home.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@Composable
fun ContactActionRow(
    contactId: String,
    lookupKey: String,
    phoneNumber: String?,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .padding(start = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kontakt öffnen (Blau)
        ActionIcon(Icons.Default.Person, Color(0xFF2196F3)) {
            onOpenContact(contactId, lookupKey)
        }

        if (!phoneNumber.isNullOrBlank()) {
            ActionIcon(Icons.Default.Call, Color(0xFF4CAF50)) {
                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri()))
            }
            ActionIcon(Icons.AutoMirrored.Filled.Message, Color(0xFF2196F3)) {
                context.startActivity(Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumber".toUri()))
            }
            // WhatsApp
            ActionIcon(Icons.Default.Add, Color(0xFF25D366)) {
                val cleanNumber = phoneNumber.replace("\\s+".toRegex(), "").replace("+", "")
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://api.whatsapp.com/send?phone=$cleanNumber".toUri()
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
