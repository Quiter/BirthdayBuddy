package com.heckmannch.birthdaybuddy.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.data.UserPreferences
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val contacts: List<BirthdayContact> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val availableLabels: Set<String> = emptySet(),
    val selectedLabels: Set<String> = emptySet(),
    val hiddenDrawerLabels: Set<String> = emptySet()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

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
    }

    private fun filterContacts(
        contacts: List<BirthdayContact>,
        query: String,
        prefs: UserPreferences
    ): List<BirthdayContact> {
        if (contacts.isEmpty()) return emptyList()

        return contacts.asSequence()
            .filter { contact ->
                if (query.isNotEmpty()) {
                    contact.name.contains(query, ignoreCase = true)
                } else {
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

    fun loadContacts(isInitial: Boolean = false) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            // Sofort als ladend markieren
            _isLoading.value = true

            if (!isInitial) {
                // Kurze Pause für System-Sync
                delay(500)
            }
            
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(), 
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _isLoading.value = false
                return@launch
            }

            try {
                repository.refreshBirthdays()
                
                // Einmalige Initialisierung der Standard-Labels
                val currentPrefs = filterManager.preferencesFlow.first()
                if (!currentPrefs.isInitialized) {
                    val labels = repository.allBirthdays.first().flatMap { it.labels }.toSet()
                    if (labels.isNotEmpty()) {
                        filterManager.saveSelectedLabels(labels)
                        filterManager.saveNotificationSelectedLabels(labels)
                        filterManager.saveWidgetSelectedLabels(labels)
                    }
                    filterManager.setInitialized(true)
                }

                if (!isInitial) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val minDisplayTime = 1200L 
                    if (elapsedTime < minDisplayTime) {
                        delay(minDisplayTime - elapsedTime)
                    }
                }
            } catch (_: Exception) {
                // Fehler werden im UI über leere Listen/EmptyStates abgefangen
            } finally {
                _isLoading.value = false
            }
        }
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
}
