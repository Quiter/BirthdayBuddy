package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.usecase.CategorizedLabel
import com.heckmannch.birthdaybuddy.domain.usecase.GetCategorizedLabelsUseCase
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    getCategorizedLabelsUseCase: GetCategorizedLabelsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val categorizedLabelsFlow = getCategorizedLabelsUseCase(
        contacts = contactRepository.allContacts,
        configs = contactRepository.labelConfigs,
        otherEventsEnabled = contactRepository.otherEventsEnabled,
        disabledOtherEventLabels = contactRepository.disabledOtherEventLabels,
    )

    /**
     * Unified UI state combining label enablement status and categorized label sections with
     * their unfiltered assigned contact counts.
     */
    val uiState: StateFlow<LabelUiState> = combine(
        contactRepository.labelsEnabled,
        categorizedLabelsFlow,
    ) { labelsEnabled, categorized ->
        LabelUiState(
            labelsEnabled = labelsEnabled,
            activeLabels = categorized.activeLabels.map { it.toUiModel() },
            otherEventLabels = categorized.otherEventLabels.map { it.toUiModel() },
            hiddenLabels = categorized.hiddenLabels.map { it.toUiModel() },
            ignoredLabels = categorized.ignoredLabels.map { it.toUiModel() },
            allLabeledContactsCount = categorized.totalLabeledContactsCount,
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LabelUiState(),
        )

    private fun CategorizedLabel.toUiModel(): LabelItemUiModel = LabelItemUiModel(
        label = LabelManagementModel(
            name = config.name,
            isHiddenFromFilter = config.isHiddenFromFilter,
            isIgnored = config.isIgnored,
            isSystem = config.isSystem,
            notificationsEnabled = config.notificationsEnabled,
            showInWidget = config.showInWidget,
        ),
        contactsCount = assignedContacts.size,
    )

    fun onIntent(intent: LabelIntent) {
        when (intent) {
            is LabelIntent.UpdateLabelConfig -> {
                updateLabelConfig(
                    intent.name,
                    intent.hidden,
                    intent.ignored,
                    intent.isSystem,
                    intent.notificationsEnabled,
                    intent.showInWidget,
                )
            }

            is LabelIntent.SetLabelsEnabled -> {
                setLabelsEnabled(intent.enabled)
            }
        }
    }

    private fun updateLabelConfig(
        name: String,
        hidden: Boolean,
        ignored: Boolean,
        isSystem: Boolean,
        notificationsEnabled: Boolean,
        showInWidget: Boolean,
    ) = viewModelScope.launch {
        contactRepository.updateLabelConfig(
            LabelConfig(
                name = name,
                isHiddenFromFilter = hidden,
                isIgnored = ignored,
                isSystem = isSystem,
                notificationsEnabled = notificationsEnabled,
                showInWidget = showInWidget,
            )
        )
    }

    /**
     * Updates the status of the label management feature.
     */
    private fun setLabelsEnabled(enabled: Boolean) = viewModelScope.launch {
        contactRepository.updateLabelsEnabled(enabled)
    }
}

sealed interface LabelIntent {
    data class UpdateLabelConfig(
        val name: String,
        val hidden: Boolean,
        val ignored: Boolean,
        val isSystem: Boolean,
        val notificationsEnabled: Boolean = true,
        val showInWidget: Boolean = true,
    ) : LabelIntent

    data class SetLabelsEnabled(val enabled: Boolean) : LabelIntent
}
