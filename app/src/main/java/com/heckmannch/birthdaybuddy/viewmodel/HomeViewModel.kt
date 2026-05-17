package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val mapper: ContactMapper,
    timeRepository: TimeRepository,
) : ViewModel() {

    // --- Search & Filter State ---
    private val _searchQuery = MutableStateFlow("")
    private val _selectedLabel = MutableStateFlow<String?>(null)
    private val _isResettingFilter = MutableStateFlow(value = false)
    private val _isSyncing = MutableStateFlow(value = false)
    private val _searchFocusRequested = MutableStateFlow(value = false)

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    fun setIsResettingFilter(isResetting: Boolean) { _isResettingFilter.value = isResetting }

    // --- Data Processing ---
    private val ignoredLabels: Flow<Set<String>> = contactRepository.labelConfigs
        .map { configs -> 
            configs.asSequence()
                .filter { it.isIgnored }
                .map { it.name }
                .toSet() 
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private val allUiContacts: Flow<List<ContactUiModel>> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
    ) { list, today ->
        list.asSequence()
            .map { mapper.toUiModel(it, today) }
            .sortedBy { it.daysUntilNext }
            .toList()
    }.flowOn(Dispatchers.Default)

    init {
        // Pre-fetch der ersten Kontaktbilder
        viewModelScope.launch {
            allUiContacts.filter { it.isNotEmpty() }.first().take(20).forEach { contact ->
                if (contact.imageUri != null) {
                    val request = ImageRequest.Builder(context)
                        .data(contact.imageUri)
                        .size(150)
                        .build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }

    private val searchKeywords = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .map { if (it.isEmpty()) emptyList() else it.split("\\s+".toRegex()) }
        .flowOn(Dispatchers.Default)

    private val filteredContacts: Flow<List<ContactUiModel>?> = combine(
        allUiContacts,
        searchKeywords,
        _selectedLabel,
        ignoredLabels,
    ) { uiList, keywords, label, ignoredLabels ->
        val isSearching = keywords.isNotEmpty()
        val startTime = System.currentTimeMillis()

        val result = uiList.asSequence()
            .filter { contact ->
                val isIgnored = contact.labels.any { it in ignoredLabels }
                if (isIgnored && !isSearching) return@filter false

                val matchesQuery = !isSearching || keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true)
                }
                val matchesLabel = (label == null) || contact.labels.contains(label)
                matchesQuery && matchesLabel
            }
            .toList()
        
        if (uiList.size > 1000) {
            Log.d("HomeViewModel", "Filtering ${uiList.size} contacts took ${System.currentTimeMillis() - startTime}ms")
        }
        result
    }

    val availableLabels: Flow<List<String>> = combine(
        contactRepository.allContacts,
        contactRepository.labelConfigs,
    ) { contacts, configs ->
        val inUseLabels = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }
        
        if (inUseLabels.none { configMap[it]?.isSystem == false }) return@combine emptyList()

        inUseLabels.asSequence()
            .filter { name ->
                val config = configMap[name]
                !(config?.isHiddenFromFilter ?: false) && !(config?.isIgnored ?: false)
            }
            .sorted()
            .toList()
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        _searchQuery,
        _selectedLabel,
        _isResettingFilter,
        _isSyncing,
        availableLabels,
        _searchFocusRequested,
    ) { flows ->
        HomeUiState(
            contacts = flows[0] as List<ContactUiModel>?,
            searchQuery = flows[1] as String,
            selectedLabel = flows[2] as String?,
            isResettingFilter = flows[3] as Boolean,
            isSyncing = flows[4] as Boolean,
            availableLabels = flows[5] as List<String>,
            searchFocusRequested = flows[6] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // --- Actions ---
    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
        if (newQuery.isNotEmpty() && wasEmpty) {
            _selectedLabel.value = null
        }
        _searchQuery.value = newQuery
        if (newQuery.isEmpty() && !wasEmpty) triggerScrollToTop()
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.value = if (_selectedLabel.value == label) null else label
        triggerScrollToTop()
    }

    fun resetFilters() {
        if ((_searchQuery.value.isNotEmpty()) || (_selectedLabel.value != null)) {
            _searchQuery.value = ""
            _selectedLabel.value = null
            triggerScrollToTop()
        }
    }

    fun updateGiftIdeas(lookupKey: String, ideas: String) = viewModelScope.launch {
        contactRepository.updateGiftIdeas(lookupKey, ideas)
    }

    fun syncContacts(showLoading: Boolean = false) = viewModelScope.launch {
        if (showLoading) _isSyncing.value = true
        val startTime = System.currentTimeMillis()
        
        contactRepository.syncContacts()
        updateWidget()
        
        if (showLoading) {
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 800) {
                delay(800 - elapsedTime)
            }
            _isSyncing.value = false
        }
    }

    fun triggerScrollToTop() = viewModelScope.launch {
        _scrollToTopEvent.emit(Unit)
    }

    fun triggerSearchFocus() {
        _searchFocusRequested.value = true
    }

    fun consumeSearchFocus() {
        _searchFocusRequested.value = false
    }

    private fun updateWidget() = viewModelScope.launch {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Widget update failed", e)
        }
    }
}
