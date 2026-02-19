package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.heckmannch.birthdaybuddy.ui.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import kotlinx.coroutines.launch

@Composable
fun SettingsMainScreenExcludeLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
    val labels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen(
        title = "Blockieren",
        description = "Kontakte mit hier aktivierten Labels werden immer ausgeblendet.",
        availableLabels = availableLabels,
        activeLabels = labels,
        isLoading = isLoading,
        onToggle = { label, checked ->
            val newSet = labels.toMutableSet()
            if (checked) newSet.add(label) else newSet.remove(label)
            scope.launch { filterManager.saveExcludedLabels(newSet) }
        },
        onBack = onBack
    )
}
