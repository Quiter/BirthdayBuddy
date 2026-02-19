package com.heckmannch.birthdaybuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.FilterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val contacts: List<BirthdayContact> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val availableLabels: Set<String> = emptySet(),
    val selectedLabels: Set<String> = emptySet(),
    val hiddenDrawerLabels: Set<String> = emptySet()
)

class MainViewModel(
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        repository.allBirthdays,
        _isLoading,
        _searchQuery,
        filterManager.selectedLabelsFlow,
        filterManager.excludedLabelsFlow,
        filterManager.hiddenDrawerLabelsFlow
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val contacts = array[0] as List<BirthdayContact>
        val loading = array[1] as Boolean
        val query = array[2] as String
        @Suppress("UNCHECKED_CAST")
        val selected = array[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val excluded = array[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val hidden = array[5] as Set<String>
        
        val filtered = if (contacts.isEmpty() || selected.isEmpty()) {
            emptyList()
        } else {
            contacts.filter { contact ->
                if (contact.labels.any { excluded.contains(it) }) return@filter false
                if (query.isNotEmpty() && !contact.name.contains(query, ignoreCase = true)) return@filter false
                contact.labels.any { label -> 
                    selected.contains(label) && !hidden.contains(label)
                }
            }
            .distinctBy { it.id }
            .sortedBy { it.remainingDays }
        }

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
        loadContacts()
        viewModelScope.launch {
            uiState.map { it.availableLabels to it.selectedLabels }
                .distinctUntilChanged()
                .collect { (available, selected) ->
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

    /**
     * Factory zur Erstellung des ViewModels mit injizierten Abhängigkeiten.
     */
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as BirthdayApplication
                return MainViewModel(
                    repository = application.container.birthdayRepository,
                    filterManager = application.container.filterManager
                ) as T
            }
        }
    }
}
