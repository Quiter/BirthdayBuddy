package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.heckmannch.birthdaybuddy.ui.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import kotlinx.coroutines.launch

@Composable
fun SettingsNotificationIncludeLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
    val selectedLabels by filterManager.notificationSelectedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen(
        title = "Anzeigen",
        description = "Diese Labels für Benachrichtigungen nutzen.",
        availableLabels = availableLabels,
        activeLabels = selectedLabels,
        isLoading = isLoading,
        onToggle = { label, checked ->
            val newSet = selectedLabels.toMutableSet()
            if (checked) newSet.add(label) else newSet.remove(label)
            scope.launch { 
                filterManager.saveNotificationSelectedLabels(newSet)
            }
        },
        onBack = onBack
    )
}
