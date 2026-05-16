package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactRepository.labelConfigs,
        contactRepository.allContacts,
    ) { configs, contacts ->
        val labelsInUse = contacts.asSequence().flatMap { it.labels }.toSet()
        configs.asSequence()
            .filter { it.name in labelsInUse }
            .map { config ->
                LabelManagementModel(
                    config.name,
                    config.isHiddenFromFilter,
                    config.isIgnored,
                    config.isSystem,
                )
            }.sortedBy { it.name }.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateLabelConfig(name: String, hidden: Boolean, ignored: Boolean, isSystem: Boolean) = viewModelScope.launch {
        contactRepository.updateLabelConfig(LabelConfig(name, hidden, ignored, isSystem))
        updateWidget()
    }

    private fun updateWidget() = viewModelScope.launch {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("LabelViewModel", "Widget update failed", e)
        }
    }
}
