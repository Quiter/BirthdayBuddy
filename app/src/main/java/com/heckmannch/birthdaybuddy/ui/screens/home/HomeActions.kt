package com.heckmannch.birthdaybuddy.ui.screens.home

import com.heckmannch.birthdaybuddy.ui.model.GiftIdea

/**
 * Bündelt alle Benutzeraktionen des HomeScreens, um "Prop Drilling" zu reduzieren.
 */
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
    val onRefresh: () -> Unit,
)
