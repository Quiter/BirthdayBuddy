package com.heckmannch.birthdaybuddy.data.repository

import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotification
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val appSettingsDao: AppSettingsDao,
) {
    private val settingsMutex = Mutex()

    val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()

    val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { it ?: AppSettings() }

    suspend fun updateSettings(
        notificationsEnabled: Boolean? = null,
        persistentNotifications: Boolean? = null,
        swipeHintShown: Boolean? = null,
        onboardingCompleted: Boolean? = null,
        lastSyncTimestamp: Long? = null
    ) = settingsMutex.withLock {
        val current = appSettingsDao.getSettingsImmediate() ?: AppSettings()
        appSettingsDao.upsertSettings(
            current.copy(
                notificationsEnabled = notificationsEnabled ?: current.notificationsEnabled,
                persistentNotifications = persistentNotifications ?: current.persistentNotifications,
                swipeHintShown = swipeHintShown ?: current.swipeHintShown,
                onboardingCompleted = onboardingCompleted ?: current.onboardingCompleted,
                lastSyncTimestamp = lastSyncTimestamp ?: current.lastSyncTimestamp
            )
        )
    }

    suspend fun getAllRulesImmediate(): List<NotificationRule> = notificationRuleDao.getAllRulesImmediate()

    suspend fun insertRule(rule: NotificationRule) = notificationRuleDao.upsertRule(rule)

    suspend fun updateRule(rule: NotificationRule) = notificationRuleDao.updateRule(rule)

    suspend fun deleteRule(rule: NotificationRule) = notificationRuleDao.deleteRule(rule)

    // Pending Notifications
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification> = 
        pendingNotificationDao.getActiveNotificationsImmediate()

    suspend fun insertPendingNotification(notification: PendingNotification) = 
        pendingNotificationDao.upsert(notification)

    suspend fun getPendingNotificationById(id: Int) = pendingNotificationDao.getNotificationById(id)

    suspend fun incrementDismissCount(id: Int) = pendingNotificationDao.incrementDismissCount(id)

    suspend fun markAsDone(id: Int) = pendingNotificationDao.markAsDone(id)
}
