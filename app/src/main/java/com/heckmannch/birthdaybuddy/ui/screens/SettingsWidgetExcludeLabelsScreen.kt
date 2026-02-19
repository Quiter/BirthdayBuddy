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
fun SettingsWidgetExcludeLabelsScreen(
    filterManager: FilterManager, 
    availableLabels: Set<String>, 
    isLoading: Boolean, 
    onBack: () -> Unit
) {
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
