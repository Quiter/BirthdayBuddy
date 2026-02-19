package com.heckmannch.birthdaybuddy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.FilterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BirthdayRepository(application)
    private val filterManager = FilterManager(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered Contacts Logic
    val filteredContacts: StateFlow<List<BirthdayContact>> = combine(
        repository.allBirthdays,
        _searchQuery,
        filterManager.selectedLabelsFlow,
        filterManager.excludedLabelsFlow,
        filterManager.hiddenDrawerLabelsFlow
    ) { contacts, query, selected, excluded, hidden ->
        if (contacts.isEmpty()) return@combine emptyList()

        // Wenn noch nichts ausgewählt wurde (erster Start), zeigen wir erst mal nichts,
        // bis die Initialisierung (unten im init) gegriffen hat.
        val selectedLabels = selected
        if (selectedLabels.isEmpty()) return@combine emptyList()
        
        withContext(Dispatchers.Default) {
            contacts.filter { contact ->
                // 1. Ausschluss-Filter
                if (contact.labels.any { excluded.contains(it) }) return@filter false
                
                // 2. Suche
                if (query.isNotEmpty() && !contact.name.contains(query, ignoreCase = true)) return@filter false
                
                // 3. Auswahl-Filter (Einschließen)
                // Wichtig: Nur Labels zählen, die NICHT im Drawer versteckt sind
                contact.labels.any { label -> 
                    selectedLabels.contains(label) && !hidden.contains(label)
                }
            }
            .distinctBy { it.id }
            .sortedBy { it.remainingDays }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLabels: StateFlow<Set<String>> = repository.allBirthdays
        .map { contacts -> contacts.flatMap { it.labels }.toSortedSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // 1. Daten laden/synchronisieren
        loadContacts()

        // 2. Auto-Initialisierung: Wenn Labels da sind, aber noch nichts selektiert wurde
        viewModelScope.launch {
            combine(availableLabels, filterManager.selectedLabelsFlow) { available, selected ->
                available to selected
            }.collect { (available, selected) ->
                if (available.isNotEmpty() && selected.isEmpty()) {
                    filterManager.saveSelectedLabels(available)
                }
            }
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshBirthdays()
            _isLoading.value = false
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
