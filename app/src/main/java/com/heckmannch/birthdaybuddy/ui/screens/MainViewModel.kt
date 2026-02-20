package com.heckmannch.birthdaybuddy.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.heckmannch.birthdaybuddy.BirthdayApplication
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val contacts: List<BirthdayContact> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val availableLabels: Set<String> = emptySet(),
    val selectedLabels: Set<String> = emptySet(),
    val hiddenDrawerLabels: Set<String> = emptySet()
)

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Loading : SyncStatus
    object Success : SyncStatus
    data class Error(val message: String) : SyncStatus
}

private data class VmFilterState(
    val selected: Set<String>,
    val excluded: Set<String>,
    val hidden: Set<String>
)

/**
 * Das MainViewModel verwaltet den globalen State der App.
 */
class MainViewModel(
    application: Application,
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val vmFilterStateFlow = combine(
        filterManager.selectedLabelsFlow,
        filterManager.excludedLabelsFlow,
        filterManager.hiddenDrawerLabelsFlow
    ) { selected, excluded, hidden ->
        VmFilterState(selected, excluded, hidden)
    }

    val uiState: StateFlow<MainUiState> = combine(
        repository.allBirthdays,
        _isLoading,
        _searchQuery,
        vmFilterStateFlow
    ) { contacts, loading, query, filterState ->
        val selected = filterState.selected
        val excluded = filterState.excluded
        val hidden = filterState.hidden

        val filtered = when {
            contacts.isEmpty() -> emptyList()
            query.isNotEmpty() -> {
                contacts.filter { contact ->
                    contact.name.contains(query, ignoreCase = true)
                }
            }
            selected.isEmpty() -> emptyList()
            else -> {
                contacts.filter { contact ->
                    if (contact.labels.any { excluded.contains(it) }) return@filter false
                    contact.labels.any { label ->
                        selected.contains(label) && !hidden.contains(label)
                    }
                }
            }
        }.distinctBy { it.id }.sortedBy { it.remainingDays }

        val labels = contacts.flatMap { it.labels }.toSortedSet()

        MainUiState(
            contacts = filtered,
            isLoading = loading,
            searchQuery = query,
            availableLabels = labels,
            selectedLabels = selected,
            hiddenDrawerLabels = hidden
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(isLoading = true)
    )

    init {
        loadContacts(isInitial = true)
        
        viewModelScope.launch {
            val isInitialized = filterManager.isInitializedFlow.first()
            
            uiState.map { it.availableLabels }
                .filter { it.isNotEmpty() }
                .first()
                .let { labels ->
                    if (!isInitialized) {
                        filterManager.saveSelectedLabels(labels)
                        filterManager.saveNotificationSelectedLabels(labels)
                        filterManager.saveWidgetSelectedLabels(labels)
                        filterManager.saveExcludedLabels(emptySet())
                        filterManager.saveNotificationExcludedLabels(emptySet())
                        filterManager.saveWidgetExcludedLabels(emptySet())
                        filterManager.saveHiddenDrawerLabels(emptySet())
                        filterManager.setInitialized(true)
                    }
                }
        }
    }

    fun loadContacts(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (!isInitial) _syncStatus.value = SyncStatus.Loading
            _isLoading.value = true
            
            val context = getApplication<Application>()
            val hasPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                if (!isInitial) {
                    _syncStatus.value = SyncStatus.Error(context.getString(R.string.sync_error_permission))
                }
                _isLoading.value = false
                return@launch
            }

            try {
                repository.refreshBirthdays()
                if (!isInitial) _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                if (!isInitial) {
                    _syncStatus.value = SyncStatus.Error(
                        context.getString(R.string.sync_error_unknown, e.message ?: "Unknown")
                    )
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLabel(label: String) {
        viewModelScope.launch {
            val currentSelected = filterManager.selectedLabelsFlow.first()
            val newSelection = currentSelected.toMutableSet()
            if (newSelection.contains(label)) newSelection.remove(label) else newSelection.add(label)
            filterManager.saveSelectedLabels(newSelection)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as BirthdayApplication
                return MainViewModel(
                    application = application,
                    repository = application.container.birthdayRepository,
                    filterManager = application.container.filterManager
                ) as T
            }
        }
    }
}
