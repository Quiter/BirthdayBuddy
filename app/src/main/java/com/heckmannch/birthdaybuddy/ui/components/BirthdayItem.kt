package com.heckmannch.birthdaybuddy.ui.components

import android.content.Context
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
import androidx.compose.ui.res.stringResource
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
    
    // Meilensteine differenzieren
    val isKidBirthday = contact.age in 1..9
    val isRoundBirthday = contact.age > 0 && contact.age % 10 == 0

    // Farben für UI-Highlights
    val goldColor = Color(0xFFFFD700)
    val secondaryGold = Color(0xFFFBC02D)
    val silverColor = Color(0xFFC0C0C0)
    val secondarySilver = Color(0xFFE0E0E0)
    val kidColors = listOf(Color(0xFF4285F4), Color(0xFFF06292), Color(0xFFFFB300), Color(0xFF4CAF50))

    val borderBrush = when {
        isKidBirthday -> Brush.linearGradient(kidColors)
        isRoundBirthday -> Brush.linearGradient(listOf(goldColor, secondaryGold))
        else -> Brush.linearGradient(listOf(silverColor, secondarySilver))
    }

    val todayLabelColor = when {
        isKidBirthday -> Color(0xFF4285F4)
        isRoundBirthday -> goldColor
        else -> silverColor
    }

    // Konfetti-Konfiguration (nur berechnen, wenn nötig)
    val party = remember(isKidBirthday, isRoundBirthday) {
        val colors = when {
            isKidBirthday -> kidColors.map { it.hashCode() }
            isRoundBirthday -> listOf(0xFFFFD700.toInt(), 0xFFFFE082.toInt(), 0xFFB8860B.toInt())
            else -> listOf(0xFFC0C0C0.toInt(), 0xFFE0E0E0.toInt(), 0xFFFFFFFF.toInt(), 0xFF9E9E9E.toInt())
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

    val birthdayModifier = if (isBirthdayToday) {
        Modifier.border(BorderStroke(2.dp, borderBrush), shape = CardDefaults.elevatedShape)
    } else if (isSoon) {
        Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer), shape = CardDefaults.elevatedShape)
    } else Modifier

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
                        when {
                            isKidBirthday -> if (isDark) Color(0xFF0D1B2A) else Color(0xFFE3F2FD)
                            isRoundBirthday -> if (isDark) Color(0xFF332D00) else Color(0xFFFFFDE7)
                            else -> if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
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
                                    append(stringResource(R.string.birthday_item_will_be))
                                    withStyle(SpanStyle(color = getAgeColorRaw(contact.age, isDark), fontWeight = FontWeight.Bold)) {
                                        append(if (contact.age < 0) "?" else "${contact.age}")
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
                                        tint = todayLabelColor
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.birthday_item_today),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isDark) todayLabelColor else (if (isKidBirthday) Color(0xFF1976D2) else if (isRoundBirthday) Color(0xFF827717) else Color(0xFF616161)),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        append(stringResource(R.string.birthday_item_in))
                                        withStyle(SpanStyle(color = getDaysColorRaw(contact.remainingDays, isDark), fontWeight = FontWeight.SemiBold)) {
                                            append("${contact.remainingDays}")
                                        }
                                        append(stringResource(R.string.birthday_item_days))
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

                    // Labels
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

                    // Aktionen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val actions = contact.actions
                        val greetingText = if (isBirthdayToday) GreetingGenerator.generateRandomGreeting(contact.name, contact.age) else ""

                        if (actions.phoneNumber != null) {
                            FilledTonalIconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${actions.phoneNumber}".toUri()))
                            }) {
                                Icon(Icons.Default.Call, stringResource(R.string.action_call))
                            }
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "smsto:${actions.phoneNumber}".toUri()).apply {
                                    if (isBirthdayToday) putExtra("sms_body", greetingText)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.action_sms))
                            }
                        }

                        if (actions.email != null) {
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "mailto:${actions.email}".toUri()).apply {
                                    if (isBirthdayToday) {
                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                                        putExtra(Intent.EXTRA_TEXT, greetingText)
                                    }
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Email, stringResource(R.string.action_email))
                            }
                        }

                        // Messenger-Buttons mit Hilfsfunktion
                        if (actions.hasWhatsApp && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF25D366), iconRes = R.drawable.ic_whatsapp) {
                                launchMessenger(context, actions.phoneNumber, greetingText, isBirthdayToday, "com.whatsapp", "https://wa.me/")
                            }
                        }

                        if (actions.hasSignal && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF3A76F0), iconRes = R.drawable.ic_signal) {
                                launchMessenger(context, actions.phoneNumber, greetingText, isBirthdayToday, "org.thoughtcrime.securesms", "https://signal.me/#p/")
                            }
                        }

                        if (actions.hasTelegram && actions.phoneNumber != null) {
                            MessengerButton(color = Color(0xFF0088CC), text = "TG") {
                                launchMessenger(context, actions.phoneNumber, greetingText, isBirthdayToday, "org.telegram.messenger", "tg://msg?to=")
                            }
                        }

                        if (actions.phoneNumber == null && actions.email == null && !actions.hasWhatsApp && !actions.hasSignal && !actions.hasTelegram) {
                            Text(
                                stringResource(R.string.error_no_contact_data),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isBirthdayToday && expanded) {
            KonfettiView(modifier = Modifier.matchParentSize(), parties = listOf(party))
        }
    }
}

/**
 * Hilfsfunktion zum Starten von Messenger-Apps
 */
private fun launchMessenger(context: Context, number: String, text: String, isToday: Boolean, packageName: String, urlPrefix: String) {
    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
    val url = if (isToday) "$urlPrefix$cleanNumber?text=${Uri.encode(text)}" else "$urlPrefix$cleanNumber"
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply { setPackage(packageName) }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: Exception) {
            val appName = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            Toast.makeText(context, context.getString(R.string.error_app_not_found, appName), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun MessengerButton(color: Color, iconRes: Int? = null, text: String? = null, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = color.copy(alpha = 0.15f), contentColor = color)
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
