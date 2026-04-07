package com.heckmannch.birthdaybuddy.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.repository.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import com.heckmannch.birthdaybuddy.data.preferences.UserPreferences
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    @ApplicationContext private val context: Context,
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    // Heavy computation: Filtering and label extraction runs on Default dispatcher
    private val _filteredData = combine(
        repository.allBirthdays.distinctUntilChanged(),
        _searchQuery,
        filterManager.preferencesFlow.distinctUntilChanged()
    ) { contacts, query, prefs ->
        val filtered = filterContacts(contacts, query, prefs)
        val availableLabels = contacts.flatMap { it.labels }.toSortedSet()
        filtered to availableLabels
    }.flowOn(Dispatchers.Default)

    // UI State combines the results. This is collected on the Main thread.
    // Note: No flowOn(Default) at the end, so _searchQuery updates are immediate in the UI State.
    val uiState: StateFlow<MainUiState> = combine(
        _filteredData,
        _isLoading,
        _searchQuery,
        filterManager.preferencesFlow.distinctUntilChanged()
    ) { filteredResult, loading, query, prefs ->
        val (filtered, availableLabels) = filteredResult
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
            _isLoading.value = true

            if (!isInitial) {
                delay(500)
            }
            
            val hasPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _isLoading.value = false
                return@launch
            }

            try {
                repository.refreshBirthdays()
                
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
            val currentSelected = filterManager.preferencesFlow.first().selectedLabels
            val newSelection = currentSelected.toMutableSet()
            if (newSelection.contains(label)) newSelection.remove(label) else newSelection.add(label)
            filterManager.saveSelectedLabels(newSelection)
        }
    }
}
