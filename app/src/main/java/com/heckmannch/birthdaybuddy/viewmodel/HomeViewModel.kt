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
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
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
    private val _newlyAddedIdeaId = MutableStateFlow<String?>(null)

    companion object {
        const val LABEL_NO_BIRTHDAY = "special:no_birthday"
    }

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
            .sortedWith(
                compareBy<ContactUiModel> { it.daysUntilNext }
                    .thenBy { it.fullName },
            )
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
                // Kontakte ohne Geburtstag werden in der Hauptliste ausgeblendet, 
                // ES SEI DENN wir suchen gerade oder haben den "Ohne Datum" Filter aktiv.
                val isMissingBirthday = contact.dateText == "-"
                val isNoBirthdayFilter = label == LABEL_NO_BIRTHDAY
                
                if (isMissingBirthday && !isSearching && !isNoBirthdayFilter) return@filter false

                val isIgnored = contact.labels.any { it in ignoredLabels }
                if (isIgnored && !isSearching) return@filter false

                val matchesQuery = !isSearching || keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true)
                }
                
                val matchesLabel = when (label) {
                    null -> true
                    LABEL_NO_BIRTHDAY -> isMissingBirthday
                    else -> contact.labels.contains(label)
                }
                
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
        
        val hasUserLabels = inUseLabels.any { configMap[it]?.isSystem == false }
        val hasMissingBirthdays = contacts.any { it.birthday == null }
        
        // Wenn weder User-Label noch fehlende Geburtstage da sind -> Bar verstecken
        if (!hasUserLabels && !hasMissingBirthdays) return@combine emptyList()

        val labels = mutableListOf<String>()
        
        // Zuerst die User-Label
        if (hasUserLabels) {
            inUseLabels.asSequence()
                .filter { name ->
                    val config = configMap[name]
                    (config?.isSystem == false) && !(config.isHiddenFromFilter) && !(config.isIgnored)
                }
                .sorted()
                .forEach { labels.add(it) }
        }

        // "Ohne Datum" immer als letztes, falls vorhanden
        if (hasMissingBirthdays) {
            labels.add(LABEL_NO_BIRTHDAY)
        }
        
        labels
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
        _newlyAddedIdeaId,
    ) { params ->
        HomeUiState(
            contacts = params[0] as List<ContactUiModel>?,
            searchQuery = params[1] as String,
            selectedLabel = params[2] as String?,
            isResettingFilter = params[3] as Boolean,
            isSyncing = params[4] as Boolean,
            availableLabels = params[5] as List<String>,
            searchFocusRequested = params[6] as Boolean,
            newlyAddedIdeaId = params[7] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // --- Actions ---
    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
        if (newQuery.isNotEmpty() && wasEmpty) {
            _selectedLabel.value = null
        }
        
        // Bei jeder Änderung der Suche scrollen wir hoch (Reset-Mode)
        if (_searchQuery.value != newQuery) {
            _isResettingFilter.value = true
            triggerScrollToTop()
        }
        
        _searchQuery.value = newQuery
    }

    fun onLabelSelected(label: String?) {
        val newLabel = if (_selectedLabel.value == label) null else label
        if (_selectedLabel.value != newLabel) {
            _isResettingFilter.value = true
            _selectedLabel.value = newLabel
            triggerScrollToTop()
        }
    }

    fun resetFilters() {
        if ((_searchQuery.value.isNotEmpty()) || (_selectedLabel.value != null)) {
            _isResettingFilter.value = true
            _searchQuery.value = ""
            _selectedLabel.value = null
            triggerScrollToTop()
        }
    }

    private suspend fun updateContactGiftIdeas(lookupKey: String, transform: (List<GiftIdea>) -> List<GiftIdea>) {
        val contact = allUiContacts.first().find { it.lookupKey == lookupKey } ?: return
        val newIdeas = transform(contact.giftIdeas)
        contactRepository.updateGiftIdeas(lookupKey, GiftIdea.toString(newIdeas))
    }

    fun addGiftIdea(lookupKey: String) = viewModelScope.launch {
        val newIdea = GiftIdea(text = "")
        _newlyAddedIdeaId.value = newIdea.id
        
        updateContactGiftIdeas(lookupKey) { currentIdeas ->
            val ideas = currentIdeas.toMutableList()
            val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
            if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newIdea)
            else ideas.add(newIdea)
            ideas
        }
    }

    fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) = viewModelScope.launch {
        updateContactGiftIdeas(lookupKey) { currentIdeas ->
            val ideas = currentIdeas.toMutableList()
            val idx = ideas.indexOfFirst { it.id == idea.id }
            if (idx != -1) {
                ideas.removeAt(idx)
                val newItem = idea.copy(isChecked = isChecked)
                if (isChecked) {
                    ideas.add(newItem)
                } else {
                    val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
                    if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newItem)
                    else ideas.add(0, newItem)
                }
            }
            ideas
        }
    }

    fun deleteGiftIdea(lookupKey: String, ideaId: String) = viewModelScope.launch {
        updateContactGiftIdeas(lookupKey) { currentIdeas ->
            currentIdeas.filter { it.id != ideaId }
        }
    }

    fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) = viewModelScope.launch {
        updateContactGiftIdeas(lookupKey) { currentIdeas ->
            currentIdeas.map {
                if (it.id == ideaId) it.copy(text = newText) else it
            }
        }
    }

    fun updateBirthday(contactId: String, birthday: java.time.LocalDate) = viewModelScope.launch {
        contactRepository.updateContactBirthday(contactId, birthday)
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
