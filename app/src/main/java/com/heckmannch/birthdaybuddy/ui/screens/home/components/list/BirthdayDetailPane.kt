package com.heckmannch.birthdaybuddy.ui.screens.home.components.list


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.ContactActionRow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Ein Detail-Paneel zur Anzeige aller Informationen eines Kontakts auf Tablets.
 */
@Composable
fun BirthdayDetailPane(
    contact: ContactUiModel,
    newlyAddedIdeaId: String?,
    actions: HomeActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val showDatePicker = remember { mutableStateOf(false) }

    if (showDatePicker.value) {
        BirthdayDatePickerDialog(
            initialDate = contact.birthday,
            onDismissRequest = { showDatePicker.value = false },
            onDateSelected = { date ->
                actions.onUpdateBirthday(contact.contactId, date)
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(SpacingNormal),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                // Großes Avatar-Bild (96.dp statt 40.dp)
                ContactImage(
                    imageUri = contact.imageUri,
                    fullName = contact.fullName,
                    initials = contact.initials,
                    secondImageUri = contact.secondImageUri,
                    secondInitials = contact.secondInitials,
                    secondFullName = contact.secondFullName,
                    size = ContactImageSizeLarge
                )

                // Name des Kontakts
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Geburtstag Datumstext
                Text(
                    text = contact.dateText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status (Alter und Resttage)
                BirthdayStatus(
                    isToday = contact.isToday,
                    nextAge = contact.nextAge,
                    daysUntilNext = contact.daysUntilNext
                ) {
                    showDatePicker.value = true
                }

                // Labels / Gruppen
                if (contact.labels.isNotEmpty()) {
                    Text(
                        text = contact.labels.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaEmphasisMedium),
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = SpacingSmall),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow)
                )

                // Aktionen (Anrufen, SMS, WhatsApp)
                ContactActionRow(
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    phoneNumber = contact.phoneNumber,
                    hasBirthday = contact.daysUntilNext != null,
                    onAddBirthday = { showDatePicker.value = true },
                    actions = actions,
                    isCouple = contact.isCouple
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = SpacingSmall),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow)
                )

                // Titel für Geschenkideen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                ) {
                    Surface(
                        modifier = Modifier.size(SpacingExtraLarge),
                        shape = RoundedCornerShape(SpacingSmall),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerSubtle),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeSmall)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.item_action_gifts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Geschenkideen Liste
                GiftIdeaList(
                    giftIdeas = contact.giftIdeas,
                    newlyAddedId = newlyAddedIdeaId,
                    onAddNewIdea = { actions.onAddGiftIdea(contact.lookupKey) },
                    onCheckedChange = { idea, checked ->
                        actions.onToggleGiftIdea(contact.lookupKey, idea, checked)
                    },
                    onTextChange = { idea, newText ->
                        actions.onUpdateGiftIdeaText(contact.lookupKey, idea.id, newText)
                    },
                    onDelete = { idea ->
                        actions.onDeleteGiftIdea(contact.lookupKey, idea.id)
                    }
                ) {
                    if (it.text.isNotBlank()) {
                        actions.onAddGiftIdea(contact.lookupKey)
                    } else {
                        focusManager.clearFocus()
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(SpacingSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.detail_close_desc)
                )
            }
        }
    }
}
