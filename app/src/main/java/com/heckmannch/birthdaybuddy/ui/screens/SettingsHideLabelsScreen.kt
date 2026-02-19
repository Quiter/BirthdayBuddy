package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.runtime.*
import com.heckmannch.birthdaybuddy.ui.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import kotlinx.coroutines.launch

@Composable
fun SettingsHideLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
    val hiddenLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    
    val visibleLabels = remember(availableLabels, hiddenLabels) { 
        availableLabels.filter { !hiddenLabels.contains(it) }.toSet() 
    }

    LabelSelectionScreen(
        title = "Anzeigen",
        description = "Diese Labels im Seitenmenü anzeigen.",
        availableLabels = availableLabels,
        activeLabels = visibleLabels,
        isLoading = isLoading,
        onToggle = { label, isVisible ->
            val newHiddenSet = hiddenLabels.toMutableSet()
            if (isVisible) newHiddenSet.remove(label) else newHiddenSet.add(label)
            scope.launch { filterManager.saveHiddenDrawerLabels(newHiddenSet) }
        },
        onBack = onBack
    )
}
