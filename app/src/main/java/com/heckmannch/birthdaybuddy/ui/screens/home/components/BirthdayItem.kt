package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayItem(
    contact: ContactUiModel,
    isExpanded: Boolean,
    newlyAddedIdeaId: String?,
    onExpand: () -> Unit,
    onAddGiftIdea: (String) -> Unit,
    onToggleGiftIdea: (String, GiftIdea, Boolean) -> Unit,
    onUpdateGiftIdeaText: (String, String, String) -> Unit,
    onDeleteGiftIdea: (String, String) -> Unit,
    onUpdateBirthday: (String, java.time.LocalDate) -> Unit,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    var giftIdeasExpanded by remember(isExpanded) { mutableStateOf(value = false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onUpdateBirthday(contact.contactId, date)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.gift_dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.gift_dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Automatisches Aufklappen, wenn eine neue Idee hinzugefügt wurde
    LaunchedEffect(newlyAddedIdeaId) {
        if (newlyAddedIdeaId != null) {
            giftIdeasExpanded = true
        }
    }

    val borderStroke = remember(contact.isToday, contact.nextAge) {
        if (contact.isToday) {
            val age = contact.nextAge
            when {
                // Alle durch 10 teilbaren (10, 20, 30...) sind Gold
                (age != null && (age % 10 == 0)) -> BorderStroke(2.dp, BirthdayGold)
                // Kinder von 0 bis 9 sind Bunt
                (age != null) && (age in 0..9) -> BorderStroke(2.dp, Brush.linearGradient(KidColors))
                // Alle anderen (inkl. ohne Jahr) sind Silber
                else -> BorderStroke(2.dp, BirthdaySilver)
            }
        } else null
    }

    val confettiColors = remember(contact.isToday, contact.nextAge) {
        if (!contact.isToday) return@remember emptyList<Color>()
        val age = contact.nextAge
        when {
            (age != null && age % 10 == 0) -> listOf(BirthdayGold)
            (age != null) && (age in 0..9) -> KidColors
            else -> listOf(BirthdaySilver, Color.White)
        }
    }

    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded && contact.isToday) {
            showConfetti = true
            delay(3000)
            showConfetti = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = borderStroke
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                ListItem(
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExpand()
                    },
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
                            if (contact.hasGiftIdeas) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFFB300)
                                )
                            }
                        }
                    },
                    supportingContent = {
                        Text(
                            text = contact.dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        ContactImage(
                            imageUri = contact.imageUri,
                            fullName = contact.fullName,
                            initials = contact.initials
                        )
                    },
                    trailingContent = {
                        BirthdayStatus(
                            isToday = contact.isToday,
                            nextAge = contact.nextAge,
                            daysUntilNext = contact.daysUntilNext,
                            onAddClick = { showDatePicker = true }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (isExpanded) {
                    if (contact.labels.isNotEmpty()) {
                        Text(
                            text = contact.labels.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ContactActionRow(
                        contactId = contact.contactId,
                        lookupKey = contact.lookupKey,
                        phoneNumber = contact.phoneNumber,
                        hasWhatsApp = contact.hasWhatsApp,
                        hasSignal = contact.hasSignal,
                        onOpenContact = onOpenContact
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Toggle-Bereich für Geschenkideen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { giftIdeasExpanded = !giftIdeasExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.item_action_gifts),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (giftIdeasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (giftIdeasExpanded) {
                        GiftIdeaList(
                            giftIdeas = contact.giftIdeas,
                            newlyAddedId = newlyAddedIdeaId,
                            onAddNewIdea = { onAddGiftIdea(contact.lookupKey) },
                            onCheckedChange = { idea, checked -> 
                                onToggleGiftIdea(contact.lookupKey, idea, checked) 
                            },
                            onTextChange = { idea, newText -> 
                                onUpdateGiftIdeaText(contact.lookupKey, idea.id, newText) 
                            },
                            onDelete = { idea -> 
                                onDeleteGiftIdea(contact.lookupKey, idea.id)
                            },
                            onDone = { idea ->
                                if (idea.text.isNotBlank()) onAddGiftIdea(contact.lookupKey)
                                else focusManager.clearFocus()
                            }
                        )
                    }
                }
            }

            if (showConfetti) {
                ConfettiEffect(
                    colors = confettiColors,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirthdayItemPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            BirthdayItem(
                contact = SampleData.contact1,
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                onAddGiftIdea = {},
                onToggleGiftIdea = { _, _, _ -> },
                onUpdateGiftIdeaText = { _, _, _ -> },
                onDeleteGiftIdea = { _, _ -> },
                onUpdateBirthday = { _, _ -> },
                onOpenContact = { _, _ -> }
            )
            
            BirthdayItem(
                contact = SampleData.contact3.copy(fullName = "Ausgeklappt (5. Geb.)"),
                isExpanded = true,
                newlyAddedIdeaId = null,
                onExpand = {},
                onAddGiftIdea = {},
                onToggleGiftIdea = { _, _, _ -> },
                onUpdateGiftIdeaText = { _, _, _ -> },
                onDeleteGiftIdea = { _, _ -> },
                onUpdateBirthday = { _, _ -> },
                onOpenContact = { _, _ -> }
            )
        }
    }
}
