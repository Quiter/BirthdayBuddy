package com.heckmannch.birthdaybuddy.data.source

import android.content.Context
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-spezifische Implementierung von [ContactDataSource].
 */
@Singleton
class SystemContactDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactDataSource {
    override suspend fun getBirthdays(): List<BirthdayContact> {
        return fetchBirthdays(context)
    }
}
