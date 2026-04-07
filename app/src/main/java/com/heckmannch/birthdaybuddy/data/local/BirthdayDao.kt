package com.heckmannch.birthdaybuddy.data.local

import androidx.room.*
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für die Geburtstags-Datenbank.
 */
@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays")
    fun getAllBirthdays(): Flow<List<BirthdayContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthdays(birthdays: List<BirthdayContact>)

    @Update
    suspend fun updateBirthday(birthday: BirthdayContact)

    @Query("UPDATE birthdays SET giftIdea = :giftIdea WHERE id = :id")
    suspend fun updateGiftIdea(id: String, giftIdea: String)

    @Query("SELECT id, giftIdea FROM birthdays WHERE giftIdea != ''")
    suspend fun getAllGiftIdeas(): List<GiftIdeaUpdate>

    @Query("DELETE FROM birthdays")
    suspend fun deleteAllBirthdays()

    @Transaction
    suspend fun syncBirthdays(freshBirthdays: List<BirthdayContact>) {
        // 1. Bestehende Geschenkideen sichern
        val existingIdeas = getAllGiftIdeas().associate { it.id to it.giftIdea }
        
        // 2. Alle alten Einträge löschen (um Kontakte zu entfernen, die im Telefonbuch gelöscht wurden)
        deleteAllBirthdays()
        
        // 3. Neue Kontakte mit den gesicherten Geschenkideen verknüpfen
        val birthdaysToInsert = freshBirthdays.map { contact ->
            existingIdeas[contact.id]?.let { idea ->
                contact.copy(giftIdea = idea)
            } ?: contact
        }
        
        // 4. Alles neu einfügen
        insertBirthdays(birthdaysToInsert)
    }
}

data class GiftIdeaUpdate(
    val id: String,
    val giftIdea: String
)
