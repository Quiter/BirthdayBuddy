package com.heckmannch.birthdaybuddy.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
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
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayColors
import com.heckmannch.birthdaybuddy.utils.GreetingGenerator
import com.heckmannch.birthdaybuddy.utils.toGermanDate
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun BirthdayItem(
    modifier: Modifier = Modifier,
    contact: BirthdayContact,
    onUpdateGiftIdea: (String, String) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    var showGiftDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Ressourcen-Strings vorab laden für die Verwendung in Lambdas (behebt Lint-Error)
    val shareSubject = stringResource(R.string.share_subject)
    val actionCallLabel = stringResource(R.string.action_call)
    val actionSmsLabel = stringResource(R.string.action_sms)
    val actionEmailLabel = stringResource(R.string.action_email)
    
    val isDark = MaterialTheme.colorScheme.surface.toArgb().let { 
        val luminance = 0.2126 * ((it shr 16) and 0xFF) + 0.7152 * ((it shr 8) and 0xFF) + 0.0722 * (it and 0xFF)
        luminance < 128
    }

    val isBirthdayToday = contact.remainingDays == 0
    val isKidBirthday = contact.age in 0..9
    val isRoundBirthday = contact.age > 0 && contact.age % 10 == 0

    val borderBrush = when {
        isKidBirthday -> Brush.linearGradient(BirthdayColors.KidColors)
        isRoundBirthday -> Brush.linearGradient(listOf(BirthdayColors.Gold, BirthdayColors.GoldSecondary))
        else -> Brush.linearGradient(listOf(BirthdayColors.Silver, BirthdayColors.SilverSecondary))
    }

    val todayIconColor = when {
        isKidBirthday -> BirthdayColors.KidPrimary
        isRoundBirthday -> BirthdayColors.Gold
        else -> BirthdayColors.Silver
    }

    val party = remember(isKidBirthday, isRoundBirthday) {
        val colors = when {
            isKidBirthday -> BirthdayColors.KidColors.map { it.toArgb() }
            isRoundBirthday -> listOf(BirthdayColors.Gold.toArgb(), BirthdayColors.GoldSecondary.toArgb())
            else -> listOf(BirthdayColors.Silver.toArgb(), BirthdayColors.SilverSecondary.toArgb())
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
                            isKidBirthday -> if (isDark) BirthdayColors.KidContainerDark else BirthdayColors.KidContainerLight
                            isRoundBirthday -> if (isDark) BirthdayColors.GoldContainerDark else BirthdayColors.GoldContainerLight
                            else -> if (isDark) BirthdayColors.SilverContainerDark else BirthdayColors.SilverContainerLight
                        }
                    }
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
                            text = contact.birthday.toGermanDate(),
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
                                    withStyle(SpanStyle(color = getAgeColor(contact.age, isDark), fontWeight = FontWeight.Bold)) {
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
                                        tint = todayIconColor
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.birthday_item_today),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when {
                                            isKidBirthday -> if (isDark) BirthdayColors.KidPrimary else BirthdayColors.KidTextDark
                                            isRoundBirthday -> if (isDark) BirthdayColors.Gold else BirthdayColors.GoldTextDark
                                            else -> if (isDark) BirthdayColors.Silver else BirthdayColors.SilverTextDark
                                        },
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        append(stringResource(R.string.birthday_item_in))
                                        withStyle(SpanStyle(color = getDaysColor(contact.remainingDays, isDark), fontWeight = FontWeight.SemiBold)) {
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

                    if (contact.labels.isNotEmpty()) {
                        val sortedLabels = remember(contact.labels) { contact.labels.sorted() }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(sortedLabels) { label ->
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

                    // GESCHENKIDEE SEKTION
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = contact.giftIdea.ifEmpty { stringResource(R.string.gift_idea_empty) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (contact.giftIdea.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { showGiftDialog = true }) {
                                Icon(
                                    if (contact.giftIdea.isEmpty()) Icons.Default.AddCircleOutline else Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.gift_idea_edit),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { showGiftDialog = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val actions = contact.actions
                        val greetingText = if (isBirthdayToday) GreetingGenerator.generateRandomGreeting(context, contact.name, contact.age) else ""

                        if (actions.phoneNumber != null) {
                            FilledTonalIconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${actions.phoneNumber}".toUri()))
                            }) {
                                Icon(Icons.Default.Call, actionCallLabel)
                            }
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "smsto:${actions.phoneNumber}".toUri()).apply {
                                    if (isBirthdayToday) putExtra("sms_body", greetingText)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, actionSmsLabel)
                            }
                        }

                        if (actions.email != null) {
                            FilledTonalIconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, "mailto:${actions.email}".toUri()).apply {
                                    if (isBirthdayToday) {
                                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                        putExtra(Intent.EXTRA_TEXT, greetingText)
                                    }
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Email, actionEmailLabel)
                            }
                        }

                        if (actions.hasWhatsApp && actions.phoneNumber != null) {
                            MessengerButton(color = BirthdayColors.WhatsApp, iconRes = R.drawable.ic_whatsapp) {
                                launchMessenger(context, actions.phoneNumber, greetingText, isBirthdayToday, "com.whatsapp", "https://wa.me/")
                            }
                        }

                        if (actions.hasSignal && actions.phoneNumber != null) {
                            MessengerButton(color = BirthdayColors.Signal, iconRes = R.drawable.ic_signal) {
                                launchMessenger(context, actions.phoneNumber, greetingText, isBirthdayToday, "org.thoughtcrime.securesms", "https://signal.me/#p/")
                            }
                        }

                        if (actions.hasTelegram && actions.phoneNumber != null) {
                            MessengerButton(color = BirthdayColors.Telegram, text = "TG") {
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

    if (showGiftDialog) {
        GiftIdeaDialog(
            initialText = contact.giftIdea,
            onDismiss = { showGiftDialog = false },
            onConfirm = { newIdea ->
                onUpdateGiftIdea(contact.id, newIdea)
                showGiftDialog = false
            }
        )
    }
}

@Composable
fun GiftIdeaDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gift_idea_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.gift_idea_dialog_hint)) },
                minLines = 3
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

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

private fun getAgeColor(age: Int, isDark: Boolean): Color {
    val near = BirthdayColors.ageNear(isDark)
    val far = BirthdayColors.ageFar(isDark)
    val ageValue = if (age < 0) 50 else age
    val fraction = (ageValue.coerceIn(0, 100) / 100f)
    return lerp(near, far, fraction)
}

private fun getDaysColor(days: Int, isDark: Boolean): Color {
    val near = BirthdayColors.daysNear(isDark)
    val far = BirthdayColors.daysFar(isDark)
    val fraction = (days.coerceIn(1, 365) / 365f)
    return lerp(near, far, fraction)
}
