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
import com.heckmannch.birthdaybuddy.data.UserPreferences
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

    val uiState: StateFlow<MainUiState> = combine(
        repository.allBirthdays,
        _isLoading,
        _searchQuery,
        filterManager.preferencesFlow
    ) { contacts, loading, query, prefs ->
        val filtered = filterContacts(contacts, query, prefs)
        val availableLabels = contacts.flatMap { it.labels }.toSortedSet()

        MainUiState(
            contacts = filtered,
            isLoading = loading,
            searchQuery = query,
            availableLabels = availableLabels,
            selectedLabels = prefs.selectedLabels,
            hiddenDrawerLabels = prefs.hiddenDrawerLabels
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(isLoading = true)
    )

    init {
        loadContacts(isInitial = true)
        initializeDefaultLabels()
    }

    private fun filterContacts(
        contacts: List<BirthdayContact>,
        query: String,
        prefs: UserPreferences
    ): List<BirthdayContact> {
        if (contacts.isEmpty()) return emptyList()

        return contacts.asSequence()
            .filter { contact ->
                // 1. Suche hat Vorrang
                if (query.isNotEmpty()) {
                    contact.name.contains(query, ignoreCase = true)
                } else {
                    // 2. Regulärer Filter
                    val isNotExcluded = contact.labels.none { prefs.excludedLabels.contains(it) }
                    val isSelected = contact.labels.any { label ->
                        prefs.selectedLabels.contains(label) && !prefs.hiddenDrawerLabels.contains(label)
                    }
                    isNotExcluded && isSelected
                }
            }
            .distinctBy { it.id }
            .sortedBy { it.remainingDays }
            .toList()
    }

    private fun initializeDefaultLabels() {
        viewModelScope.launch {
            val prefs = filterManager.preferencesFlow.first()
            if (!prefs.isInitialized) {
                uiState.map { it.availableLabels }
                    .filter { it.isNotEmpty() }
                    .first()
                    .let { labels ->
                        filterManager.saveSelectedLabels(labels)
                        filterManager.saveNotificationSelectedLabels(labels)
                        filterManager.saveWidgetSelectedLabels(labels)
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
