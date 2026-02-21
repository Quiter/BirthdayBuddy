package com.heckmannch.birthdaybuddy.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.GreetingGenerator
import com.heckmannch.birthdaybuddy.utils.formatGermanDate
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun BirthdayItem(
    contact: BirthdayContact,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = isSystemInDarkTheme()
    val isBirthdayToday = contact.remainingDays == 0
    val isSoon = contact.remainingDays in 1..7
    
    // Prüfen ob besonderer Meilenstein (1. Geburtstag oder durch 10 teilbar)
    val isRoundBirthday = contact.age > 0 && (contact.age % 10 == 0 || contact.age == 1)

    val goldColor = Color(0xFFFFD700)
    val secondaryGold = Color(0xFFFBC02D)
    val silverColor = Color(0xFFC0C0C0)
    val secondarySilver = Color(0xFFE0E0E0)

    val currentThemeColor = if (isRoundBirthday) goldColor else silverColor
    val currentSecondaryColor = if (isRoundBirthday) secondaryGold else secondarySilver

    // Konfetti-Konfiguration
    val party = remember(isRoundBirthday) {
        val colors = if (isRoundBirthday) {
            listOf(0xFFFFD700.toInt(), 0xFF4285F4.toInt(), 0xFFF06292.toInt(), 0xFFFFB300.toInt())
        } else {
            listOf(0xFFC0C0C0.toInt(), 0xFFE0E0E0.toInt(), 0xFFFFFFFF.toInt(), 0xFF9E9E9E.toInt())
        }
        Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = colors,
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
    }

    val birthdayModifier = when {
        isBirthdayToday -> Modifier.border(
            BorderStroke(2.dp, Brush.linearGradient(listOf(currentThemeColor, currentSecondaryColor))),
            shape = CardDefaults.elevatedShape
        )
        isSoon -> Modifier.border(
            BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
            shape = CardDefaults.elevatedShape
        )
        else -> Modifier
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(birthdayModifier)
                .clickable {
                    keyboardController?.hide()
                    expanded = !expanded
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = when {
                    isBirthdayToday -> {
                        if (isRoundBirthday) {
                            if (isDark) Color(0xFF332D00) else Color(0xFFFFFDE7)
                        } else {
                            if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
                        }
                    }
                    isSoon -> MaterialTheme.colorScheme.surfaceContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
        ) {
            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = formatGermanDate(contact.birthday),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = contact.photoUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.Person),
                            error = rememberVectorPainter(Icons.Default.Person)
                        )
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = buildAnnotatedString {
                                    append("Wird ")
                                    withStyle(SpanStyle(color = getAgeColorRaw(contact.age, isDark), fontWeight = FontWeight.Bold)) {
                                        val ageText = if (contact.age < 0) "?" else "${contact.age}"
                                        append(ageText)
                                    }
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (isBirthdayToday) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Cake,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = currentThemeColor
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "HEUTE!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isDark) currentThemeColor else (if (isRoundBirthday) Color(0xFF827717) else Color(0xFF616161)),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        append("in ")
                                        withStyle(SpanStyle(color = getDaysColorRaw(contact.remainingDays, isDark), fontWeight = FontWeight.SemiBold)) {
                                            append("${contact.remainingDays}")
                                        }
                                        append(" Tagen")
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                )

                if (expanded) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Labels des Kontakts anzeigen
                    if (contact.labels.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(contact.labels) { label ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val actions = contact.actions
                        val greetingText = GreetingGenerator.generateRandomGreeting(contact.name, contact.age)

                        if (actions.phoneNumber != null) {
                            FilledTonalIconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${actions.phoneNumber}".toUri()))
                            }) {
                                Icon(Icons.Default.Call, "Anrufen")
                            }
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "smsto:${actions.phoneNumber}".toUri()).apply {
                                    putExtra("sms_body", greetingText)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, "SMS")
                            }
                        }

                        if (actions.email != null) {
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "mailto:${actions.email}".toUri()).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Herzlichen Glückwunsch zum Geburtstag!")
                                    putExtra(Intent.EXTRA_TEXT, greetingText)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Email, "E-Mail")
                            }
                        }

                        if (actions.hasWhatsApp && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF25D366), iconRes = R.drawable.ic_whatsapp) {
                                val cleanNumber = actions.phoneNumber.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/$cleanNumber?text=${Uri.encode(greetingText)}".toUri()).apply {
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$cleanNumber?text=${Uri.encode(greetingText)}".toUri()))
                                }
                            }
                        }

                        if (actions.hasSignal && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF3A76F0), iconRes = R.drawable.ic_signal) {
                                val cleanNumber = actions.phoneNumber.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_SENDTO, "smsto:$cleanNumber".toUri()).apply {
                                    setPackage("org.thoughtcrime.securesms")
                                    putExtra("sms_body", greetingText)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://signal.me/#p/$cleanNumber".toUri()))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Signal nicht gefunden", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        if (actions.hasTelegram && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF0088CC), text = "TG") {
                                val cleanNumber = actions.phoneNumber.replace(Regex("[^0-9+]"), "")
                                val intent = Intent(Intent.ACTION_VIEW, "tg://msg?to=$cleanNumber&text=${Uri.encode(greetingText)}".toUri()).apply {
                                    setPackage("org.telegram.messenger")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, "tg://msg?to=$cleanNumber".toUri()))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Telegram nicht gefunden", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        if (actions.phoneNumber == null && actions.email == null && !actions.hasWhatsApp && !actions.hasSignal && !actions.hasTelegram) {
                            Text(
                                "Keine Kontaktdaten",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Konfetti über der Karte anzeigen, wenn heute Geburtstag ist und die Karte aufgeklappt wurde
        if (isBirthdayToday && expanded) {
            KonfettiView(
                modifier = Modifier.matchParentSize(),
                parties = listOf(party)
            )
        }
    }
}

@Composable
private fun MessengerButton(
    color: Color,
    iconRes: Int? = null,
    text: String? = null,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        )
    ) {
        if (iconRes != null) {
            Icon(painterResource(id = iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        } else if (text != null) {
            Text(text, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun getAgeColorRaw(age: Int, isDark: Boolean): Color {
    val youngColor = if (isDark) Color(0xFFFFA4A4) else Color(0xFFFF5252)
    val oldColor = if (isDark) Color(0xFFD32F2F) else Color(0xFF8B0000)
    
    val ageValue = if (age < 0) 50 else age
    val fraction = (ageValue.coerceIn(0, 100) / 100f)
    return lerp(youngColor, oldColor, fraction)
}

private fun getDaysColorRaw(days: Int, isDark: Boolean): Color {
    val nearColor = if (isDark) Color(0xFF80D8FF) else Color(0xFF00BFFF)
    val farColor = if (isDark) Color(0xFF5C6BC0) else Color(0xFF00008B)
    val fraction = (days.coerceIn(1, 365) / 365f)
    return lerp(nearColor, farColor, fraction)
}
