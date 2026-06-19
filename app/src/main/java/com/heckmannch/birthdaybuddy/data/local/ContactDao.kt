package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY birthday ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsImmediate(): List<Contact>

    @Upsert
    suspend fun upsertContacts(contacts: List<Contact>)

    @Upsert
    suspend fun upsertContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE lookupKey = :lookupKey")
    suspend fun getContactByLookupKey(lookupKey: String): Contact?

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("DELETE FROM contacts WHERE lookupKey NOT IN (:keys)")
    suspend fun deleteContactsNotIn(keys: List<String>)

    @Transaction
    suspend fun refreshContacts(contacts: List<Contact>) {
        if (contacts.isEmpty()) {
            deleteAllContacts()
        } else {
            upsertContacts(contacts)
            val currentKeys = contacts.map { it.lookupKey }
            deleteContactsNotIn(currentKeys)
        }
    }

    @Query(
        """
        SELECT 
            c1.lookupKey AS firstLookupKey, 
            c1.fullName AS firstName, 
            c1.imageUri AS firstImageUri,
            c2.lookupKey AS secondLookupKey, 
            c2.fullName AS secondName, 
            c2.imageUri AS secondImageUri
        FROM contacts c1
        JOIN contacts c2 ON SUBSTR(c1.anniversary, 6) = SUBSTR(c2.anniversary, 6) AND c1.lookupKey < c2.lookupKey
        WHERE c1.anniversary IS NOT NULL AND c1.spouseLookupKey IS NULL
          AND c2.anniversary IS NOT NULL AND c2.spouseLookupKey IS NULL
    """
    )
    fun getPotentialCouples(): Flow<List<PotentialCouple>>
}

