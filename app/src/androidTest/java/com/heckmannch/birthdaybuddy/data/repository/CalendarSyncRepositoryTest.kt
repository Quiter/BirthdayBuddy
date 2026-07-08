package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CalendarSyncRepositoryTest {

    private val appSettingsDao: AppSettingsDao = mock()
    private val systemCalendarDataSource: SystemCalendarDataSource = mock()
    private lateinit var repository: CalendarSyncRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = CalendarSyncRepositoryImpl(context, appSettingsDao, systemCalendarDataSource)
    }

    @Test
    fun syncBirthdays_usesSyncAdapterUriForInsertions() = runTest {
        // Given
        val contact = Contact(
            contactId = "1",
            lookupKey = "lookup_key_1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 7, 7)
        )
        val contacts = listOf(contact)

        whenever(systemCalendarDataSource.hasCalendarPermissions()).thenReturn(true)
        whenever(systemCalendarDataSource.queryAllCalendars()).thenReturn(emptyList())
        whenever(appSettingsDao.getSettingsImmediate()).thenReturn(AppSettingsEntity(otherEventsEnabled = false))
        whenever(systemCalendarDataSource.getOrCreateCalendar(any(), any(), any())).thenReturn(123L)
        whenever(systemCalendarDataSource.clearCalendarEvents(123L)).thenReturn(true)
        whenever(systemCalendarDataSource.applyBatch(any())).thenReturn(true)

        // When
        val result = repository.syncBirthdays(contacts)

        // Then
        assertThat(result).isTrue()

        // Verify clearCalendarEvents and applyBatch were called on the datasource
        verify(systemCalendarDataSource).clearCalendarEvents(123L)
        
        val captor = argumentCaptor<List<ContentProviderOperation>>()
        verify(systemCalendarDataSource).applyBatch(captor.capture())

        val operations = captor.firstValue
        assertThat(operations).isNotEmpty()

        val operation = operations.first()
        val uri = operation.uri
        assertThat(uri).isNotNull()
        assertThat(uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo("BirthdayBuddy")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)
    }
}
