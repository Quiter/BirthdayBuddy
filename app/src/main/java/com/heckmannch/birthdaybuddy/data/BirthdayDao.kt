package com.heckmannch.birthdaybuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays")
    fun getAllBirthdays(): Flow<List<BirthdayContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthdays(birthdays: List<BirthdayContact>)

    @Query("DELETE FROM birthdays")
    suspend fun deleteAllBirthdays()

    /**
     * Synchronisiert die Datenbank, indem alle alten Einträge gelöscht und die neuen eingefügt werden.
     * Dies stellt sicher, dass auch gelöschte Kontakte aus der App verschwinden.
     */
    @Transaction
    suspend fun syncBirthdays(birthdays: List<BirthdayContact>) {
        deleteAllBirthdays()
        insertBirthdays(birthdays)
    }
}
