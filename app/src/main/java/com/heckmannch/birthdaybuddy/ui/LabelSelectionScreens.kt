package com.heckmannch.birthdaybuddy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.heckmannch.birthdaybuddy.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch

@Composable
fun BlockLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
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

@Composable
fun HideLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
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

@Composable
fun WidgetIncludeLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val selectedLabels by filterManager.widgetSelectedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen(
        title = "Anzeigen",
        description = "Diese Labels im Widget anzeigen.",
        availableLabels = availableLabels,
        activeLabels = selectedLabels,
        isLoading = isLoading,
        onToggle = { label, checked ->
            val newSet = selectedLabels.toMutableSet()
            if (checked) newSet.add(label) else newSet.remove(label)
            scope.launch { 
                filterManager.saveWidgetSelectedLabels(newSet)
                updateWidget(context)
            }
        },
        onBack = onBack
    )
}

@Composable
fun WidgetExcludeLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val labels by filterManager.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen(
        title = "Blockieren",
        description = "Diese Labels im Widget immer ignorieren.",
        availableLabels = availableLabels,
        activeLabels = labels,
        isLoading = isLoading,
        onToggle = { label, checked ->
            val newSet = labels.toMutableSet()
            if (checked) newSet.add(label) else newSet.remove(label)
            scope.launch {
                filterManager.saveWidgetExcludedLabels(newSet)
                updateWidget(context)
            }
        },
        onBack = onBack
    )
}
