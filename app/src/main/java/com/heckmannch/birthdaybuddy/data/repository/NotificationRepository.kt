package com.heckmannch.birthdaybuddy.data.repository

import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotification
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import com.heckmannch.birthdaybuddy.util.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val appSettingsDao: AppSettingsDao,
    private val notificationScheduler: NotificationScheduler,
) {
    private val settingsMutex = Mutex()

    val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()
        .distinctUntilChanged()

    val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { it ?: AppSettings() }
        .distinctUntilChanged()

    suspend fun syncScheduling() = withContext(Dispatchers.IO) {
        val enabled = appSettingsDao.getSettingsImmediate()?.notificationsEnabled ?: false
        val rules = notificationRuleDao.getAllRulesImmediate()
        if (enabled && rules.isNotEmpty()) {
            notificationScheduler.scheduleNext(rules)
        } else {
            notificationScheduler.cancelNotification()
        }
    }

    suspend fun updateSettings(
        notificationsEnabled: Boolean? = null,
        persistentNotifications: Boolean? = null,
        onboardingCompleted: Boolean? = null,
        lastSyncTimestamp: Long? = null,
        calendarSyncEnabled: Boolean? = null,
        calendarId: Long? = null,
        clearCalendarId: Boolean = false,
        otherEventsEnabled: Boolean? = null,
        birthdayCalendarColor: Int? = null,
        anniversaryCalendarColor: Int? = null,
        nameDayCalendarColor: Int? = null,
        themeMode: String? = null,
        themeAmoled: Boolean? = null,
        themeAccent: String? = null
    ) {
        settingsMutex.withLock {
            val current = appSettingsDao.getSettingsImmediate() ?: AppSettings()
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
                    themeAccent = themeAccent ?: current.themeAccent
                )
            )
        }
        syncScheduling()
    }

    suspend fun getAllRulesImmediate(): List<NotificationRule> =
        notificationRuleDao.getAllRulesImmediate()

    suspend fun insertRule(rule: NotificationRule) {
        notificationRuleDao.upsertRule(rule)
        syncScheduling()
    }

    suspend fun updateRule(rule: NotificationRule) {
        notificationRuleDao.updateRule(rule)
        syncScheduling()
    }

    suspend fun deleteRule(rule: NotificationRule) {
        notificationRuleDao.deleteRule(rule)
        syncScheduling()
    }

    // Pending Notifications
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification> =
        pendingNotificationDao.getActiveNotificationsImmediate()

    suspend fun insertPendingNotification(notification: PendingNotification) =
        pendingNotificationDao.upsert(notification)

    suspend fun getPendingNotificationById(id: Int) = pendingNotificationDao.getNotificationById(id)

    suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKey: String
    ): Boolean {
        val pattern = "%\"$lookupKey\"%"
        return pendingNotificationDao.hasNotificationBeenScheduled(year, daysBefore, pattern)
    }

    suspend fun incrementDismissCount(id: Int) = pendingNotificationDao.incrementDismissCount(id)

    suspend fun markAsDone(id: Int) = pendingNotificationDao.markAsDone(id)
}
