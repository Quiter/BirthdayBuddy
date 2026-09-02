package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingNotificationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PendingNotificationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pendingNotificationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deleteOldNotifications_deletesOnlyDoneNotificationsFromPreviousYearAndAllNotificationsOlderThanTwoYears() =
        runTest {
            // Arrange
            val currentYear = 2027

            // 1. Older than 2 years (< 2026), not done -> Should be deleted (year < currentYear - 1)
            val oldPendingId = dao.upsert(
                PendingNotificationEntity(
                    id = 1,
                    contactLookupKeys = listOf("key_old_undone"),
                    daysBefore = 0,
                    year = 2025,
                    isDone = false
                )
            ).toInt()

            // 2. Older than 2 years (< 2026), done -> Should be deleted
            val oldDoneId = dao.upsert(
                PendingNotificationEntity(
                    id = 2,
                    contactLookupKeys = listOf("key_old_done"),
                    daysBefore = 0,
                    year = 2025,
                    isDone = true
                )
            ).toInt()

            // 3. Previous year (2026), NOT done -> Should be KEPT (user hasn't handled notification yet)
            val prevYearPendingId = dao.upsert(
                PendingNotificationEntity(
                    id = 3,
                    contactLookupKeys = listOf("key_prev_undone"),
                    daysBefore = 7,
                    year = 2026,
                    isDone = false
                )
            ).toInt()

            // 4. Previous year (2026), DONE -> Should be DELETED (year < currentYear AND isDone = 1)
            val prevYearDoneId = dao.upsert(
                PendingNotificationEntity(
                    id = 4,
                    contactLookupKeys = listOf("key_prev_done"),
                    daysBefore = 0,
                    year = 2026,
                    isDone = true
                )
            ).toInt()

            // 5. Current year (2027), NOT done -> Should be KEPT
            val currentYearPendingId = dao.upsert(
                PendingNotificationEntity(
                    id = 5,
                    contactLookupKeys = listOf("key_curr_undone"),
                    daysBefore = 0,
                    year = 2027,
                    isDone = false
                )
            ).toInt()

            // 6. Current year (2027), DONE -> Should be KEPT
            val currentYearDoneId = dao.upsert(
                PendingNotificationEntity(
                    id = 6,
                    contactLookupKeys = listOf("key_curr_done"),
                    daysBefore = 0,
                    year = 2027,
                    isDone = true
                )
            ).toInt()

            // Act
            dao.deleteOldNotifications(currentYear)

            // Assert
            assertThat(dao.getNotificationById(oldPendingId)).isNull()
            assertThat(dao.getNotificationById(oldDoneId)).isNull()
            assertThat(dao.getNotificationById(prevYearDoneId)).isNull()

            assertThat(dao.getNotificationById(prevYearPendingId)).isNotNull()
            assertThat(dao.getNotificationById(currentYearPendingId)).isNotNull()
            assertThat(dao.getNotificationById(currentYearDoneId)).isNotNull()
        }

    @Test
    fun getScheduledNotifications_returnsCorrectEntitiesForYearAndDaysBefore() =
        runTest {
            // Arrange
            dao.upsert(
                PendingNotificationEntity(
                    id = 1,
                    contactLookupKeys = listOf("key1", "anniversary:key2"),
                    daysBefore = 0,
                    year = 2026,
                    isDone = false
                )
            )
            dao.upsert(
                PendingNotificationEntity(
                    id = 2,
                    contactLookupKeys = listOf("nameday:key3"),
                    daysBefore = 0,
                    year = 2026,
                    isDone = true
                )
            )
            dao.upsert(
                PendingNotificationEntity(
                    id = 3,
                    contactLookupKeys = listOf("key4"),
                    daysBefore = 1, // Different daysBefore
                    year = 2026,
                    isDone = false
                )
            )
            dao.upsert(
                PendingNotificationEntity(
                    id = 4,
                    contactLookupKeys = listOf("key5"),
                    daysBefore = 0,
                    year = 2025, // Different year
                    isDone = false
                )
            )

            // Act
            val result = dao.getScheduledNotifications(2026, 0)

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result.flatMap { it.contactLookupKeys }).containsExactly("key1", "anniversary:key2", "nameday:key3")
        }
}
