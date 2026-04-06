package com.heckmannch.birthdaybuddy.data.source

import com.heckmannch.birthdaybuddy.model.BirthdayContact

/**
 * Interface für den Zugriff auf System-Kontakte.
 */
interface ContactDataSource {
    suspend fun getBirthdays(): List<BirthdayContact>
}
