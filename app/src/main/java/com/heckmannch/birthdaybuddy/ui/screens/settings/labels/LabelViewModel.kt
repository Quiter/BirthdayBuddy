package com.heckmannch.birthdaybuddy.ui.screens.settings.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
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
) : ViewModel() {

    /**
     * StateFlow indicating whether label management is enabled by the user.
     */
    val labelsEnabled: StateFlow<Boolean> = contactRepository.labelsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactRepository.labelConfigs,
        contactRepository.allContacts,
    ) { configs, contacts ->
        val labelsInUse = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }

        // Standard-Labels sortiert nach Name (ohne das Pseudo-Label)
        val standardList = configs.asSequence()
            .filter { it.name in labelsInUse && it.name != ContactLabels.LABEL_NO_BIRTHDAY }
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
            val pseudoConfig = configMap[ContactLabels.LABEL_NO_BIRTHDAY]
                ?: LabelConfig(ContactLabels.LABEL_NO_BIRTHDAY)
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
        }

    /**
     * Updates the status of the label management feature.
     */
    fun setLabelsEnabled(enabled: Boolean) = viewModelScope.launch {
        contactRepository.updateLabelsEnabled(enabled)
    }
}
