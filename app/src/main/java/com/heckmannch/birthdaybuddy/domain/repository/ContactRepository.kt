package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain repository interface for managing contacts, contact caching, and label configurations.
 */
interface ContactRepository {
    val allContacts: Flow<List<Contact>>
    val labelConfigs: Flow<List<LabelConfig>>
    val otherEventsEnabled: Flow<Boolean>
    val labelsEnabled: Flow<Boolean>

    /**
     * Emits an event whenever the system contacts provider detects a change in contacts.
     */
    val contactChanges: Flow<Unit>

    suspend fun getAllContactsImmediate(): List<Contact>
    suspend fun syncContacts()
    suspend fun updateLabelConfig(config: LabelConfig)
    suspend fun updateContactBirthday(contactId: String, birthday: LocalDate): Boolean
    suspend fun updateLabelsEnabled(enabled: Boolean)
}
