package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain repository interface for managing contacts, gift ideas, and label configs.
 */
interface ContactRepository {
    val allContacts: Flow<List<Contact>>
    val potentialCouples: Flow<List<PotentialCouple>>
    val labelConfigs: Flow<List<LabelConfig>>
    val otherEventsEnabled: Flow<Boolean>
    val ignoredCouplePairs: Flow<List<String>>
    val labelsEnabled: Flow<Boolean>

    suspend fun getAllContactsImmediate(): List<Contact>
    suspend fun syncContacts()
    suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea)
    suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean)
    suspend fun deleteGiftIdea(lookupKey: String, ideaId: String)
    suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String)
    suspend fun updateLabelConfig(config: LabelConfig)
    suspend fun updateContactBirthday(contactId: String, birthday: LocalDate): Boolean
    suspend fun exportGiftIdeas(): String
    suspend fun importGiftIdeas(jsonString: String): Int
    suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String)
    suspend fun unlinkCouple(lookupKey: String)
    suspend fun ignoreCoupleSuggestion(lookupKey1: String, lookupKey2: String)
    suspend fun clearIgnoredCouplePairs()
    suspend fun updateLabelsEnabled(enabled: Boolean)
}
