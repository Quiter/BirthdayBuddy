package com.heckmannch.birthdaybuddy.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.formatGermanDate

@Composable
fun BirthdayItem(contact: BirthdayContact) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // NEU: Tastatur-Controller auch für die Karte holen
    val keyboardController = LocalSoftwareKeyboardController.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize() // Animiert das Ausklappen butterweich
            .clickable {
                keyboardController?.hide() // Tastatur einklappen, wenn Karte angetippt wird
                expanded = !expanded
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Datum: ${formatGermanDate(contact.birthday)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = buildAnnotatedString {
                            append("Wird ")
                            withStyle(style = SpanStyle(color = getAgeColor(contact.age))) { append("${contact.age}") }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("in ")
                            withStyle(style = SpanStyle(color = getDaysColor(contact.remainingDays), fontWeight = FontWeight.Bold)) {
                                append("${contact.remainingDays}")
                            }
                            append(" Tagen")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Der aufklappbare Bereich für die Aktionen
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actions = contact.actions

                    // Telefon und SMS
                    if (actions.phoneNumber != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${actions.phoneNumber}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Anrufen", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // E-Mail
                    if (actions.email != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${actions.email}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Email, contentDescription = "E-Mail", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Messenger
                    if (actions.hasWhatsApp && actions.phoneNumber != null) {
                        MessengerIcon(text = "WA", color = Color(0xFF25D366)) {
                            val cleanNumber = actions.phoneNumber.replace(Regex("[^0-9+]"), "")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNumber"))
                            context.startActivity(intent)
                        }
                    }

                    if (actions.hasSignal && actions.phoneNumber != null) {
                        MessengerIcon(text = "SIG", color = Color(0xFF3A76F0)) {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}")).apply {
                                setPackage("org.thoughtcrime.securesms")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    }

                    if (actions.hasTelegram && actions.phoneNumber != null) {
                        MessengerIcon(text = "TG", color = Color(0xFF0088CC)) {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}")).apply {
                                setPackage("org.telegram.messenger")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    }

                    // Wenn wir gar keine Daten von dem Kontakt haben
                    if (actions.phoneNumber == null && actions.email == null && !actions.hasWhatsApp && !actions.hasSignal && !actions.hasTelegram) {
                        Text(
                            text = "Keine Kontaktdaten hinterlegt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// Hilfsfunktion für die kleinen Messenger-Logos
@Composable
private fun MessengerIcon(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
    }
}

// Ausgelagerte Farb-Helfer (jetzt wieder korrekt außerhalb der Hauptfunktion!)
@Composable
private fun getAgeColor(age: Int): Color {
    val isDark = isSystemInDarkTheme()
    val youngColor = if (isDark) Color(0xFFFFA4A4) else Color(0xFFFF5252)
    val oldColor = if (isDark) Color(0xFFD32F2F) else Color(0xFF8B0000)
    val clampedAge = age.coerceIn(0, 100)
    val fraction = clampedAge / 100f
    return lerp(youngColor, oldColor, fraction)
}

@Composable
private fun getDaysColor(days: Int): Color {
    if (days == 0) return Color(0xFFFFC107)
    val isDark = isSystemInDarkTheme()
    val nearColor = if (isDark) Color(0xFF80D8FF) else Color(0xFF00BFFF)
    val farColor = if (isDark) Color(0xFF5C6BC0) else Color(0xFF00008B)
    val clampedDays = days.coerceIn(1, 365)
    val fraction = clampedDays / 365f
    return lerp(nearColor, farColor, fraction)
}