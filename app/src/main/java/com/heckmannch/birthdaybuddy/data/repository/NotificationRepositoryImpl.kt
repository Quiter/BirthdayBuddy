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
     * Updates application settings by applying the provided [transform] function.
     *
     * Thread safety is guaranteed via an internal mutex lock.
     *
     * **Side effect:** Triggers [syncScheduling] after updating settings to ensure
     * alarms reflect any changes in configuration (e.g., enabling/disabling notifications).
     *
     * @param transform A lambda that receives the current [AppSettings] snapshot and returns the updated [AppSettings].
     */
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings): Unit =
        withContext(ioDispatcher) {
            settingsMutex.withLock {
                val currentEntity = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                val currentDomain = appSettingsMapper.toDomain(currentEntity)
                val updatedDomain = transform(currentDomain)
                val updatedEntity = appSettingsMapper.toEntity(updatedDomain)
                appSettingsDao.upsertSettings(updatedEntity)
            }
            syncScheduling()
        }

    /**
     * Retrieves a one-time snapshot of the current application settings directly from the database.
     *
     * @return The current [AppSettings].
     */
    override suspend fun getSettingsImmediate(): AppSettings = withContext(ioDispatcher) {
        val currentEntity = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
        appSettingsMapper.toDomain(currentEntity)
    }

    /**
     * Retrieves a one-time snapshot list of all configured notification rules directly from the database.
     *
     * @return List of all [NotificationRule] objects currently stored.
     */
    override suspend fun getAllRulesImmediate(): List<NotificationRule> =
        withContext(ioDispatcher) {
            notificationRuleDao.getAllRulesImmediate().map { notificationRuleMapper.toDomain(it) }
        }

    /**
     * Inserts or updates a notification rule in the database.
     *
     * **Side effect:** Triggers [syncScheduling] to reschedule alarms based on updated rules.
     *
     * @param rule The [NotificationRule] to insert or update.
     */
    override suspend fun insertRule(rule: NotificationRule): Unit = withContext(ioDispatcher) {
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
    override suspend fun updateRule(rule: NotificationRule): Unit = withContext(ioDispatcher) {
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
    override suspend fun deleteRule(rule: NotificationRule): Unit = withContext(ioDispatcher) {
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
        withContext(ioDispatcher) {
            pendingNotificationDao.getActiveNotificationsImmediate()
                .map { pendingNotificationMapper.toDomain(it) }
        }

    /**
     * Inserts or updates a pending notification entry in the database.
     *
     * @param notification The [PendingNotification] domain model to save.
     * @return The row ID of the inserted or updated pending notification record.
     */
    override suspend fun insertPendingNotification(notification: PendingNotification): Long =
        withContext(ioDispatcher) {
            pendingNotificationDao.upsert(pendingNotificationMapper.toEntity(notification))
        }

    /**
     * Retrieves a pending notification record by its unique database identifier.
     *
     * @param id The unique integer ID of the pending notification.
     * @return The matching [PendingNotification] if found, or `null` otherwise.
     */
    override suspend fun getPendingNotificationById(id: Int): PendingNotification? =
        withContext(ioDispatcher) {
            pendingNotificationDao.getNotificationById(id)
                ?.let { pendingNotificationMapper.toDomain(it) }
        }

    /**
     * Retrieves all contact lookup keys for notifications that have already been scheduled
     * for the specified year and days-before lead time in a single batch query.
     *
     * @param year The calendar year of the notification event.
     * @param daysBefore The number of days before the event when the notification is triggered.
     * @return A [Set] of scheduled contact lookup keys for fast O(1) deduplication.
     */
    override suspend fun getScheduledContactLookupKeys(year: Int, daysBefore: Int): Set<String> =
        withContext(ioDispatcher) {
            pendingNotificationDao.getScheduledNotifications(year, daysBefore)
                .flatMap { it.contactLookupKeys }
                .toSet()
        }

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
    ): Boolean = withContext(ioDispatcher) {
        val escapedLookupKey = escapeLikePattern(lookupKey)
        val pattern = "%\"$escapedLookupKey\"%"
        pendingNotificationDao.hasNotificationBeenScheduled(year, daysBefore, pattern)
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
    override suspend fun incrementDismissCount(id: Int): Unit = withContext(ioDispatcher) {
        pendingNotificationDao.incrementDismissCount(id)
    }

    /**
     * Marks a pending notification as done, preventing it from being treated as active.
     *
     * @param id The unique integer ID of the pending notification to complete.
     */
    override suspend fun markAsDone(id: Int): Unit = withContext(ioDispatcher) {
        pendingNotificationDao.markAsDone(id)
    }

    /**
     * Removes old completed pending notification records preceding the specified year.
     *
     * @param currentYear The current calendar year threshold; records prior to this year will be deleted.
     */
    override suspend fun deleteOldNotifications(currentYear: Int): Unit = withContext(ioDispatcher) {
        pendingNotificationDao.deleteOldNotifications(currentYear)
    }

    companion object {
        private const val TAG = "NotificationRepo"
    }
}
