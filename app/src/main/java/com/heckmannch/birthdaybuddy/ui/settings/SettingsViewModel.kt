package com.heckmannch.birthdaybuddy.ui.settings

import androidx.lifecycle.ViewModel
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val filterManager: FilterManager
) : ViewModel() {
    val themeFlow = filterManager.preferencesFlow.map { it.theme }
}
