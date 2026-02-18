package com.heckmannch.birthdaybuddy.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.formatGermanDate

@Composable
fun BirthdayItem(
    contact: BirthdayContact,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable {
                keyboardController?.hide()
                expanded = !expanded
            },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp, pressedElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = "Profilbild von ${contact.name}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(Icons.Default.Person),
                    error = rememberVectorPainter(Icons.Default.Person),
                    fallback = rememberVectorPainter(Icons.Default.Person)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatGermanDate(contact.birthday),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = buildAnnotatedString {
                            append("Wird ")
                            withStyle(style = SpanStyle(color = getAgeColor(contact.age), fontWeight = FontWeight.Bold)) {
                                append("${contact.age}")
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("in ")
                            withStyle(style = SpanStyle(color = getDaysColor(contact.remainingDays), fontWeight = FontWeight.SemiBold)) {
                                append("${contact.remainingDays}")
                            }
                            append(" Tagen")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actions = contact.actions

                    if (actions.phoneNumber != null) {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${actions.phoneNumber}")))
                        }) {
                            Icon(Icons.Default.Call, "Anrufen", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}")))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "SMS", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (actions.email != null) {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${actions.email}")))
                        }) {
                            Icon(Icons.Default.Email, "E-Mail", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (actions.hasWhatsApp && actions.phoneNumber != null) {
                        MessengerIcon(text = "WA", color = Color(0xFF25D366)) {
                            val cleanNumber = actions.phoneNumber.replace(Regex("[^0-9+]"), "")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNumber")))
                        }
                    }

                    if (actions.hasSignal && actions.phoneNumber != null) {
                        MessengerIcon(text = "SIG", color = Color(0xFF3A76F0)) {
                            try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}")).setPackage("org.thoughtcrime.securesms")) } catch (e: Exception) { /* Ignored */ }
                        }
                    }

                    if (actions.hasTelegram && actions.phoneNumber != null) {
                        MessengerIcon(text = "TG", color = Color(0xFF0088CC)) {
                            try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${actions.phoneNumber}")).setPackage("org.telegram.messenger")) } catch (e: Exception) { /* Ignored */ }
                        }
                    }

                    if (actions.phoneNumber == null && actions.email == null && !actions.hasWhatsApp && !actions.hasSignal && !actions.hasTelegram) {
                        Text(
                            "Keine Kontaktdaten hinterlegt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessengerIcon(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
    }
}

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
