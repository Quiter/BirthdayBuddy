package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.heckmannch.birthdaybuddy.ui.components.LabelSelectionScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch

@Composable
fun SettingsWidgetIncludeLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
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
