package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.ContactActionRow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy.ui.theme.ContactAvatarHeaderSize
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.KidColors
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.birthdayGoldColor
import com.heckmannch.birthdaybuddy.ui.theme.birthdayKidAmberColor
import com.heckmannch.birthdaybuddy.util.hasYear
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayItem(
    contact: ContactUiModel,
    isExpanded: Boolean,
    newlyAddedIdeaId: String?,
    onExpand: () -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current

    val onOpenDatePicker = {
        val initialDate = contact.birthday ?: LocalDate.now()
        actions.onOpenBirthdayPicker(
            contact.lookupKey,
            contact.birthday?.let { if (it.hasYear) it.year else null },
            initialDate.monthValue,
            initialDate.dayOfMonth,
        )
    }

    val borderStroke = remember(contact.isToday, contact.birthdayTier) {
        if (contact.isToday) {
            when (contact.birthdayTier) {
                BirthdayTier.MILESTONE_GOLD -> BorderStroke(BirthdayBorderWidth, BirthdayGold)
                BirthdayTier.MILESTONE_SILVER -> BorderStroke(BirthdayBorderWidth, BirthdaySilver)
                BirthdayTier.CHILD -> BorderStroke(
                    BirthdayBorderWidth,
                    Brush.linearGradient(KidColors)
                )

                BirthdayTier.REGULAR -> BorderStroke(BirthdayBorderWidth, BirthdaySilver)
            }
        } else null
    }

    val confettiColors = remember(contact.isToday, contact.birthdayTier) {
        if (!contact.isToday) return@remember emptyList()
        when (contact.birthdayTier) {
            BirthdayTier.MILESTONE_GOLD -> listOf(BirthdayGold)
            BirthdayTier.MILESTONE_SILVER -> listOf(BirthdaySilver, Color.White)
            BirthdayTier.CHILD -> KidColors
            BirthdayTier.REGULAR -> listOf(BirthdaySilver, Color.White)
        }
    }

    var showConfetti by remember { mutableStateOf(value = false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded && contact.isToday) {
            showConfetti = true
        } else {
            showConfetti = false
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaSurfaceContainerHigh)
            isExpanded -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardContainerColor"
    )

    val imageSize by animateDpAsState(
        targetValue = if (isExpanded) ContactAvatarHeaderSize else ContactImageSizeSmall,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contactImageSize"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingNormal)
            .padding(bottom = SpacingSmall),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = borderStroke ?: if (isSelected) BorderStroke(
            SelectedBorderWidth,
            MaterialTheme.colorScheme.primary.copy(alpha = AlphaEmphasisLow)
        ) else null,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
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
                            Text(
                                text = contact.fullName,
                                style = if (isExpanded) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
                            )
                            if (contact.isFavorite) {
                                Spacer(modifier = Modifier.width(SpacingSmall))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = stringResource(R.string.contact_favorite_desc),
                                    modifier = Modifier.size(IconSizeSmall),
                                    tint = birthdayGoldColor
                                )
                            }
                            if (contact.hasGiftIdeas) {
                                Spacer(modifier = Modifier.width(SpacingSmall))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeExtraSmall),
                                    tint = birthdayKidAmberColor
                                )
                            }
                        }
                    },
                    supportingContent = {
                        Column {
                            Text(
                                text = contact.dateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isExpanded && contact.labels.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(SpacingExtraSmall))
                                Text(
                                    text = contact.labels.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaEmphasisMedium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    leadingContent = {
                        ContactImage(
                            imageUri = contact.imageUri,
                            fullName = contact.fullName,
                            initials = contact.initials,
                            lookupKey = contact.lookupKey,
                            secondImageUri = contact.secondImageUri,
                            secondInitials = contact.secondInitials,
                            secondFullName = contact.secondFullName,
                            size = imageSize
                        )
                    },
                    trailingContent = {
                        BirthdayStatus(
                            isToday = contact.isToday,
                            nextAge = contact.nextAge,
                            daysUntilNext = contact.daysUntilNext,
                            onEditBirthday = onOpenDatePicker,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                ) {
                    Column {
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
                            modifier = Modifier.padding(horizontal = SpacingNormal),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow)
                        )

                        BirthdayItemGiftIdeaSection(
                            contact = contact,
                            isExpanded = isExpanded,
                            newlyAddedIdeaId = newlyAddedIdeaId,
                            actions = actions,
                        )
                    }
                }
            }

            if (showConfetti) {
                ConfettiEffect(
                    colors = confettiColors,
                    modifier = Modifier.matchParentSize(),
                    onAnimationEnd = { showConfetti = false }
                )
            }
        }
    }
}

@Composable
private fun BirthdayItemGiftIdeaSection(
    contact: ContactUiModel,
    isExpanded: Boolean,
    newlyAddedIdeaId: String?,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    var giftIdeasExpanded by remember(isExpanded) { mutableStateOf(value = false) }
    val focusManager = LocalFocusManager.current

    // Automatisches Aufklappen, wenn eine neue Idee hinzugefügt wurde
    LaunchedEffect(newlyAddedIdeaId) {
        if (newlyAddedIdeaId != null) {
            giftIdeasExpanded = true
        }
    }

    Column(modifier = modifier) {
        // Toggle-Bereich für Geschenkideen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { giftIdeasExpanded = !giftIdeasExpanded }
                .padding(horizontal = SpacingNormal, vertical = SpacingMedium)
                .testTag("gift_ideas_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Surface(
                modifier = Modifier.size(IconSizeLarge),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                if (it.text.isNotBlank()) actions.onAddGiftIdea(contact.lookupKey)
                else focusManager.clearFocus()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirthdayItemPreview() {
    val actions = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenBirthdayPicker = { _, _, _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {}
    )

    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(SpacingNormal)) {
            BirthdayItem(
                contact = SampleData.contact1,
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = actions
            )
        }
    }
}
