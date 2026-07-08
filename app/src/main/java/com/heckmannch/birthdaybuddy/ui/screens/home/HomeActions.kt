package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.runtime.Stable
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea

import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.MessengerApp

/**
 * Bündelt alle Benutzeraktionen des HomeScreens, um "Prop Drilling" zu reduzieren.
 */
@Stable
data class HomeActions(
    val onSearchQueryChange: (String) -> Unit,
    val onLabelSelected: (String?) -> Unit,
    val onClearSearch: () -> Unit,
    val onNavigateToSettings: () -> Unit,
    val onAddContact: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onAddGiftIdea: (String) -> Unit,
    val onToggleGiftIdea: (String, GiftIdea, Boolean) -> Unit,
    val onUpdateGiftIdeaText: (String, String, String) -> Unit,
    val onDeleteGiftIdea: (String, String) -> Unit,
    val onUpdateBirthday: (String, java.time.LocalDate) -> Unit,
    val onOpenContact: (String, String) -> Unit,
    val onDial: (String) -> Unit,
    val onSendSms: (String) -> Unit,
    val onOpenMessengerApp: (MessengerApp, String) -> Unit,
    val onRefresh: () -> Unit,
    val onUnlinkCouple: (String) -> Unit = {},
    val onLinkAsCouple: (String, String) -> Unit = { _, _ -> },
    val onIgnoreCoupleSuggestion: (String, String) -> Unit = { _, _ -> },
)
