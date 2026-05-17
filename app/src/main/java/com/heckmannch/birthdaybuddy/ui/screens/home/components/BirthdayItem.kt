package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayItem(
    contact: ContactUiModel,
    isExpanded: Boolean,
    newlyAddedIdeaId: String?,
    onExpand: () -> Unit,
    onAddGiftIdea: (String) -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    val borderStroke = remember(contact.isToday, contact.nextAge) {
        if (contact.isToday) {
            val age = contact.nextAge
            when {
                // Alle durch 10 teilbaren (10, 20, 30...) sind Gold
                (age != null && age % 10 == 0) -> BorderStroke(2.dp, BirthdayGold)
                // Kinder von 0 bis 9 sind Bunt
                (age != null) && (age in 0..9) -> BorderStroke(2.dp, Brush.linearGradient(KidColors))
                // Alle anderen (inkl. ohne Jahr) sind Silber
                else -> BorderStroke(2.dp, BirthdaySilver)
            }
        } else null
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
        Column(modifier = Modifier.animateContentSize()) {
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
                        daysUntilNext = contact.daysUntilNext
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
                    onOpenContact = onOpenContact
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                GiftIdeaList(
                    giftIdeas = contact.giftIdeas,
                    newlyAddedId = newlyAddedIdeaId,
                    onAddNewIdea = { onAddGiftIdea(contact.lookupKey) },
                    onFocusRequested = { }, // Wird nun durch newlyAddedIdeaId gesteuert
                    onCheckedChange = { idea, checked ->
                        val newIdeas = contact.giftIdeas.toMutableList()
                        val idx = newIdeas.indexOfFirst { it.id == idea.id }
                        if (idx != -1) {
                            newIdeas.removeAt(idx)
                            val newItem = idea.copy(isChecked = checked)
                            if (checked) {
                                newIdeas.add(newItem)
                            } else {
                                val firstCheckedIndex = newIdeas.indexOfFirst { it.isChecked }
                                if (firstCheckedIndex != -1) newIdeas.add(firstCheckedIndex, newItem)
                                else newIdeas.add(0, newItem)
                            }
                            onUpdateGiftIdeas(contact.lookupKey, GiftIdea.toString(newIdeas))
                        }
                    },
                    onTextChange = { idea, newText ->
                        val newIdeas = contact.giftIdeas.map {
                            if (it.id == idea.id) it.copy(text = newText) else it
                        }
                        onUpdateGiftIdeas(contact.lookupKey, GiftIdea.toString(newIdeas))
                    },
                    onDelete = { idea ->
                        val newIdeas = contact.giftIdeas.filter { it.id != idea.id }
                        onUpdateGiftIdeas(contact.lookupKey, GiftIdea.toString(newIdeas))
                    },
                    onDone = { idea ->
                        if (idea.text.isNotBlank()) onAddGiftIdea(contact.lookupKey)
                        else focusManager.clearFocus()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BirthdayItemPreview() {
    val sampleContact = ContactUiModel(
        id = "1",
        contactId = "1",
        lookupKey = "k1",
        fullName = "Max Mustermann",
        dateText = "12. Mai",
        monthName = "Mai",
        imageUri = null,
        phoneNumber = "+49 123 456789",
        initials = "M",
        nextAge = 30,
        daysUntilNext = 5,
        isToday = false,
        labels = listOf("Freunde"),
        giftIdeas = emptyList()
    )
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            BirthdayItem(
                contact = sampleContact,
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                onAddGiftIdea = {},
                onUpdateGiftIdeas = { _, _ -> },
                onOpenContact = { _, _ -> }
            )
            
            BirthdayItem(
                contact = sampleContact.copy(
                    fullName = "Ausgeklappt",
                    phoneNumber = "+49 123 456789",
                    giftIdeas = listOf(GiftIdea(text = "Socken"), GiftIdea(text = "Wein", isChecked = true))
                ),
                isExpanded = true,
                newlyAddedIdeaId = null,
                onExpand = {},
                onAddGiftIdea = {},
                onUpdateGiftIdeas = { _, _ -> },
                onOpenContact = { _, _ -> }
            )
        }
    }
}
