package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.data.mapper.NotificationRuleMapper
import com.heckmannch.birthdaybuddy.data.mapper.PendingNotificationMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [NotificationRepository] that coordinates notification rules,
 * pending notification states, and application settings.
 *
 * This repository coordinates data storage through local DAOs and manages scheduling
 * by delegating to [NotificationScheduler]. Thread safety during settings updates
 * is ensured using a coroutine [Mutex].
 */
class NotificationRepositoryImpl @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val appSettingsDao: AppSettingsDao,
    private val notificationScheduler: NotificationScheduler,
    private val appSettingsMapper: AppSettingsMapper,
    private val notificationRuleMapper: NotificationRuleMapper,
    private val pendingNotificationMapper: PendingNotificationMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : NotificationRepository {
    private val settingsMutex = Mutex()

    override val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()
        .map { entities -> entities.map { notificationRuleMapper.toDomain(it) } }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { entity -> appSettingsMapper.toDomain(entity ?: AppSettingsEntity()) }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    /**
     * Synchronizes notification alarm scheduling with the current database settings and rules.
     *
     * Catches and logs non-fatal exceptions (e.g. [SecurityException] on Android 12+ if alarm
     * permissions are missing) defensively to prevent scheduling failures from propagating
     * to callers after database mutations have already completed successfully.
     * Re-throws [CancellationException] to adhere to structured concurrency.
     */
    override suspend fun syncScheduling(): Unit = withContext(ioDispatcher) {
        try {
            val enabled = appSettingsDao.getSettingsImmediate()?.notificationsEnabled ?: false
            val rules = notificationRuleDao.getAllRulesImmediate()
            if (enabled && rules.isNotEmpty()) {
                notificationScheduler.scheduleNext(rules.map { notificationRuleMapper.toDomain(it) })
            } else {
                notificationScheduler.cancelNotification()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synchronize notification scheduling", e)
        }
    }

    /**
     * Updates application settings with the provided non-null values.
     *
     * Thread safety is guaranteed via an internal mutex lock.
     *
     * **Side effect:** Triggers [syncScheduling] after updating settings to ensure
     * alarms reflect any changes in configuration (e.g., enabling/disabling notifications).
     *
     * @param notificationsEnabled Whether notifications are enabled globally.
     * @param persistentNotifications Whether notifications should be persistent (ongoing).
     * @param onboardingCompleted Whether the user has completed the onboarding flow.
     * @param lastSyncTimestamp Timestamp (in milliseconds) of the last contact synchronization.
     * @param calendarSyncEnabled Whether calendar synchronization is active.
     * @param calendarId The ID of the target calendar for synchronization.
     * @param clearCalendarId When `true`, explicitly clears the existing calendar ID to `null`.
     * @param otherEventsEnabled Whether non-birthday events (anniversaries, etc.) are enabled.
     * @param birthdayCalendarColor Color code used for birthday events in the calendar.
     * @param anniversaryCalendarColor Color code used for anniversary events in the calendar.
     * @param nameDayCalendarColor Color code used for name day events in the calendar.
     * @param themeMode The selected UI theme mode ([ThemeMode]).
     * @param themeAmoled Whether pure black AMOLED theme mode is active.
     * @param themeAccent The selected accent color preset ([ThemeAccent]).
     * @param customAccentColor Hex string representation when using a custom accent color.
     */
    override suspend fun updateSettings(
        notificationsEnabled: Boolean?,
        persistentNotifications: Boolean?,
        onboardingCompleted: Boolean?,
        lastSyncTimestamp: Long?,
        calendarSyncEnabled: Boolean?,
        calendarId: Long?,
        clearCalendarId: Boolean,
        otherEventsEnabled: Boolean?,
        birthdayCalendarColor: Int?,
        anniversaryCalendarColor: Int?,
        nameDayCalendarColor: Int?,
        themeMode: ThemeMode?,
        themeAmoled: Boolean?,
        themeAccent: ThemeAccent?,
        customAccentColor: String?
    ) {
        settingsMutex.withLock {
            val current = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            val newThemeAccent = when (themeAccent) {
                ThemeAccent.CUSTOM -> customAccentColor ?: current.themeAccent
                null -> current.themeAccent
                else -> themeAccent.name
            }
            appSettingsDao.upsertSettings(
                current.copy(
                    notificationsEnabled = notificationsEnabled ?: current.notificationsEnabled,
                    persistentNotifications = persistentNotifications
                        ?: current.persistentNotifications,
                    onboardingCompleted = onboardingCompleted ?: current.onboardingCompleted,
                    lastSyncTimestamp = lastSyncTimestamp ?: current.lastSyncTimestamp,
                    calendarSyncEnabled = calendarSyncEnabled ?: current.calendarSyncEnabled,
                    calendarId = if (clearCalendarId) null else (calendarId ?: current.calendarId),
                    otherEventsEnabled = otherEventsEnabled ?: current.otherEventsEnabled,
                    birthdayCalendarColor = birthdayCalendarColor ?: current.birthdayCalendarColor,
                    anniversaryCalendarColor = anniversaryCalendarColor
                        ?: current.anniversaryCalendarColor,
                    nameDayCalendarColor = nameDayCalendarColor ?: current.nameDayCalendarColor,
                    themeMode = themeMode ?: current.themeMode,
                    themeAmoled = themeAmoled ?: current.themeAmoled,
                    themeAccent = newThemeAccent
                )
            )
        }
        syncScheduling()
    }

    /**
     * Retrieves a one-time snapshot list of all configured notification rules directly from the database.
     *
     * @return List of all [NotificationRule] objects currently stored.
     */
    override suspend fun getAllRulesImmediate(): List<NotificationRule> =
        notificationRuleDao.getAllRulesImmediate().map { notificationRuleMapper.toDomain(it) }

    /**
     * Inserts or updates a notification rule in the database.
     *
     * **Side effect:** Triggers [syncScheduling] to reschedule alarms based on updated rules.
     *
     * @param rule The [NotificationRule] to insert or update.
     */
    override suspend fun insertRule(rule: NotificationRule) {
        notificationRuleDao.upsertRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    /**
     * Updates an existing notification rule in the database.
     *
     * **Side effect:** Triggers [syncScheduling] to recalculate and reschedule alarm timings.
     *
     * @param rule The [NotificationRule] with updated values.
     */
    override suspend fun updateRule(rule: NotificationRule) {
        notificationRuleDao.upsertRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    /**
     * Deletes a notification rule from the database.
     *
     * **Side effect:** Triggers [syncScheduling] to cancel or readjust upcoming alarms.
     *
     * @param rule The [NotificationRule] to delete.
     */
    override suspend fun deleteRule(rule: NotificationRule) {
        notificationRuleDao.deleteRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    // Pending Notifications

    /**
     * Retrieves all currently active (not marked as done) pending notifications.
     *
     * @return List of active [PendingNotification] instances.
     */
    override suspend fun getActiveNotificationsImmediate(): List<PendingNotification> =
        pendingNotificationDao.getActiveNotificationsImmediate()
            .map { pendingNotificationMapper.toDomain(it) }

    /**
     * Inserts or updates a pending notification entry in the database.
     *
     * @param notification The [PendingNotification] domain model to save.
     * @return The row ID of the inserted or updated pending notification record.
     */
    override suspend fun insertPendingNotification(notification: PendingNotification): Long =
        pendingNotificationDao.upsert(pendingNotificationMapper.toEntity(notification))

    /**
     * Retrieves a pending notification record by its unique database identifier.
     *
     * @param id The unique integer ID of the pending notification.
     * @return The matching [PendingNotification] if found, or `null` otherwise.
     */
    override suspend fun getPendingNotificationById(id: Int): PendingNotification? =
        pendingNotificationDao.getNotificationById(id)
            ?.let { pendingNotificationMapper.toDomain(it) }

    /**
     * Checks whether a notification for the specified year, days-before lead time, and contact lookup key
     * has already been scheduled or created.
     *
     * @param year The calendar year of the notification event.
     * @param daysBefore The number of days before the event when the notification is triggered.
     * @param lookupKey The contact lookup key to search for within the notification metadata.
     * @return `true` if a matching notification already exists, `false` otherwise.
     */
    override suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKey: String
    ): Boolean {
        val escapedLookupKey = escapeLikePattern(lookupKey)
        val pattern = "%\"$escapedLookupKey\"%"
        return pendingNotificationDao.hasNotificationBeenScheduled(year, daysBefore, pattern)
    }

    /**
     * Escapes special characters (`\`, `%`, `_`) for SQLite `LIKE` queries.
     */
    private fun escapeLikePattern(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Increments the dismissal counter for the specified pending notification.
     *
     * @param id The unique integer ID of the pending notification.
     */
    override suspend fun incrementDismissCount(id: Int) {
        pendingNotificationDao.incrementDismissCount(id)
    }

    /**
     * Marks a pending notification as done, preventing it from being treated as active.
     *
     * @param id The unique integer ID of the pending notification to complete.
     */
    override suspend fun markAsDone(id: Int) {
        pendingNotificationDao.markAsDone(id)
    }

    /**
     * Removes old completed pending notification records preceding the specified year.
     *
     * @param currentYear The current calendar year threshold; records prior to this year will be deleted.
     */
    override suspend fun deleteOldNotifications(currentYear: Int) {
        pendingNotificationDao.deleteOldNotifications(currentYear)
    }

    companion object {
        private const val TAG = "NotificationRepo"
    }
}
