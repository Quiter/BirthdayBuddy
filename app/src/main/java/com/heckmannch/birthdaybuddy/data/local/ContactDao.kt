package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val SQLITE_BIND_CHUNK_SIZE = 500

/**
 * Data Access Object for managing cached contacts and potential couple matches in the database.
 */
@Dao
interface ContactDao {
    /**
     * Observes all cached contacts ordered chronologically by birthday.
     */
    @Query("SELECT * FROM contacts ORDER BY birthday ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    /**
     * Retrieves all cached contacts immediately.
     */
    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsImmediate(): List<ContactEntity>

    /**
     * Retrieves all contact lookup keys currently stored in the database.
     */
    @Query("SELECT lookupKey FROM contacts")
    suspend fun getAllLookupKeys(): List<String>

    /**
     * Inserts or updates a list of contacts.
     */
    @Upsert
    suspend fun upsertContacts(contacts: List<ContactEntity>)

    /**
     * Inserts or updates a single contact.
     */
    @Upsert
    suspend fun upsertContact(contact: ContactEntity)

    /**
     * Retrieves a single contact by its unique [lookupKey], or null if not found.
     */
    @Query("SELECT * FROM contacts WHERE lookupKey = :lookupKey")
    suspend fun getContactByLookupKey(lookupKey: String): ContactEntity?

    /**
     * Deletes all contacts from the database.
     */
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    /**
     * Deletes contacts matching the provided list of [keys].
     */
    @Query("DELETE FROM contacts WHERE lookupKey IN (:keys)")
    suspend fun deleteContactsByLookupKeys(keys: List<String>)

    /**
     * Synchronizes the database cache with the given list of contacts atomically.
     *
     * To prevent exceeding SQLite's host parameter limit (`SQLITE_MAX_VARIABLE_NUMBER`,
     * which defaults to 999 on several Android versions), stale contacts are identified
     * by diffing existing lookup keys against incoming keys and deleted in chunks of 500.
     */
    @Transaction
    suspend fun refreshContacts(contacts: List<ContactEntity>) {
        if (contacts.isEmpty()) {
            deleteAllContacts()
        } else {
            val existingKeys = getAllLookupKeys().toSet()
            val incomingKeys = contacts.map { it.lookupKey }.toSet()
            val staleKeys = (existingKeys - incomingKeys).toList()

            upsertContacts(contacts)

            if (staleKeys.isNotEmpty()) {
                staleKeys.chunked(SQLITE_BIND_CHUNK_SIZE).forEach { chunk ->
                    deleteContactsByLookupKeys(chunk)
                }
            }
        }
    }

    /**
     * Observes potential couples sharing the same anniversary date who are not yet explicitly paired.
     */
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
    fun getPotentialCouples(): Flow<List<CoupleProjection>>
}
