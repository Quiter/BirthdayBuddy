package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.heckmannch.birthdaybuddy.ui.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import kotlinx.coroutines.launch

@Composable
fun SettingsNotificationExcludeLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
    val excludedLabels by filterManager.notificationExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen(
        title = "Ausschließen",
        description = "Diese Labels bei Benachrichtigungen ignorieren.",
        availableLabels = availableLabels,
        activeLabels = excludedLabels,
        isLoading = isLoading,
        onToggle = { label, checked ->
            val newSet = excludedLabels.toMutableSet()
            if (checked) newSet.add(label) else newSet.remove(label)
            scope.launch { 
                filterManager.saveNotificationExcludedLabels(newSet)
            }
        },
        onBack = onBack
    )
}
