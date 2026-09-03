package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.ContactActionRow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.birthdayGoldColor
import com.heckmannch.birthdaybuddy.util.hasYear
import java.time.LocalDate

/**
 * Ein Detail-Paneel zur Anzeige aller Informationen eines Kontakts auf Tablets.
 *
 * @param contact Das [ContactUiModel] mit den anzuzeigenden Kontaktdaten (Name, Bild, Geburtstag, Labels, Geschenkideen etc.).
 * @param newlyAddedIdeaId Die ID einer neu hinzugefügten Geschenkidee zur automatischen Fokussierung, oder `null`.
 * @param actions Die gebündelten [HomeActions]-Callbacks für Nutzeraktionen (z. B. Geschenke bearbeiten, Geburtstag anpassen, Anrufen).
 * @param onClose Callback-Funktion, die aufgerufen wird, wenn das Detail-Paneel geschlossen werden soll.
 * @param modifier Der auf das umgebende Card-Layout anzuwendende [Modifier].
 * @param contentPadding Scaffold- oder Container-Padding zur Begrenzung des Paneels unterhalb der Top-Bar und oberhalb der Navigationsleiste.
 */
@Composable
fun BirthdayDetailPane(
    contact: ContactUiModel,
    newlyAddedIdeaId: String?,
    actions: HomeActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val focusManager = LocalFocusManager.current

    val onOpenDatePicker = {
        val initialDate = contact.birthday ?: LocalDate.now()
        actions.onOpenBirthdayPicker(
            contact.lookupKey,
            contact.birthday?.let { if (it.hasYear) it.year else null },
            initialDate.monthValue,
            initialDate.dayOfMonth,
        )
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(SpacingNormal)
            .testTag("birthday_detail_pane"),
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
                    lookupKey = contact.lookupKey,
                    secondImageUri = contact.secondImageUri,
                    secondInitials = contact.secondInitials,
                    secondFullName = contact.secondFullName,
                    size = ContactImageSizeLarge
                )

                // Name des Kontakts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = contact.fullName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (contact.isFavorite) {
                        Spacer(modifier = Modifier.width(SpacingSmall))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.contact_favorite_desc),
                            modifier = Modifier.size(IconSizeMedium),
                            tint = birthdayGoldColor
                        )
                    }
                }

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
                    daysUntilNext = contact.daysUntilNext,
                    onEditBirthday = onOpenDatePicker,
                )

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
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow)
                )

                // Aktionen (Anrufen, SMS, WhatsApp)
                ContactActionRow(
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    phoneNumber = contact.phoneNumber,
                    hasBirthday = contact.daysUntilNext != null,
                    onAddBirthday = onOpenDatePicker,
                    actions = actions,
                    isCouple = contact.isCouple
                )

                HorizontalDivider(
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
                        shape = MaterialTheme.shapes.small,
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
                    },
                    onDone = { idea ->
                        if (idea.text.isNotBlank()) {
                            actions.onAddGiftIdea(contact.lookupKey)
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(SpacingSmall)
                    .testTag("detail_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.detail_close_desc)
                )
            }
        }
    }
}

@Preview(
    name = "Light Theme - Tablet",
    showBackground = true,
    device = Devices.TABLET
)
@Preview(
    name = "Dark Theme - Tablet",
    showBackground = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BirthdayDetailPanePreview() {
    val sampleContact = ContactUiModel(
        id = "1",
        contactId = "1",
        lookupKey = "k1",
        fullName = "Max Mustermann",
        dateText = "15. August (in 12 Tagen)",
        monthName = "August",
        imageUri = null,
        phoneNumber = "+49 123 4567890",
        initials = "MM",
        nextAge = 35,
        daysUntilNext = 12,
        isToday = false,
        isFavorite = true,
        hasWhatsApp = true,
        hasSignal = true,
        labels = listOf("Familie", "Freunde"),
        giftIdeas = listOf(
            GiftIdea(id = "1", text = "Leder-Geldbörse", isChecked = false),
            GiftIdea(id = "2", text = "Konzertkarte", isChecked = true),
            GiftIdea(id = "3", text = "Espressobohnen", isChecked = false)
        ),
        birthday = LocalDate.of(1991, 8, 15),
        birthdayTier = BirthdayTier.REGULAR
    )

    BirthdayBuddyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BirthdayDetailPane(
                contact = sampleContact,
                newlyAddedIdeaId = null,
                actions = HomeActions.previewDefaults(),
                onClose = {}
            )
        }
    }
}
