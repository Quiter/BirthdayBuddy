package com.heckmannch.birthdaybuddy.ui.labels

import androidx.lifecycle.ViewModel
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    val filterManager: FilterManager
) : ViewModel() {
    val hiddenDrawerLabelsFlow = filterManager.preferencesFlow.map { it.hiddenDrawerLabels }
    val widgetSelectedLabelsFlow = filterManager.preferencesFlow.map { it.widgetSelectedLabels }
    val notificationSelectedLabelsFlow = filterManager.preferencesFlow.map { it.notificationSelectedLabels }
    val excludedLabelsFlow = filterManager.preferencesFlow.map { it.excludedLabels }
    val showLabelManagerIntroFlow = filterManager.preferencesFlow.map { it.showLabelManagerIntro }
}
