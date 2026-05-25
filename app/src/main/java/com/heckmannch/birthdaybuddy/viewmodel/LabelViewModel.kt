package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactRepository.labelConfigs,
        contactRepository.allContacts,
    ) { configs, contacts ->
        val labelsInUse = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }

        // Standard-Labels sortiert nach Name (ohne das Pseudo-Label)
        val standardList = configs.asSequence()
            .filter { it.name in labelsInUse && it.name != HomeViewModel.LABEL_NO_BIRTHDAY }
            .map { config ->
                LabelManagementModel(
                    config.name,
                    config.isHiddenFromFilter,
                    config.isIgnored,
                    config.isSystem,
                )
            }.sortedBy { it.name }.toList()

        // Pseudo-Label "Ohne Datum" am Ende anhängen, falls Kontakte ohne Geburtstag da sind
        val hasMissingBirthdays = contacts.any { it.birthday == null }
        if (hasMissingBirthdays) {
            val pseudoConfig = configMap[HomeViewModel.LABEL_NO_BIRTHDAY]
                ?: LabelConfig(HomeViewModel.LABEL_NO_BIRTHDAY)
            standardList + LabelManagementModel(
                pseudoConfig.name,
                pseudoConfig.isHiddenFromFilter,
                pseudoConfig.isIgnored,
                pseudoConfig.isSystem,
            )
        } else {
            standardList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateLabelConfig(name: String, hidden: Boolean, ignored: Boolean, isSystem: Boolean) =
        viewModelScope.launch {
            contactRepository.updateLabelConfig(LabelConfig(name, hidden, ignored, isSystem))
            updateWidget()
        }

    private fun updateWidget() = viewModelScope.launch {
        widgetUpdater.updateWidget()
    }
}
