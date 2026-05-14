package com.heckmannch.birthdaybuddy.repository

import com.heckmannch.birthdaybuddy.database.AppSettings
import com.heckmannch.birthdaybuddy.database.AppSettingsDao
import com.heckmannch.birthdaybuddy.database.NotificationRule
import com.heckmannch.birthdaybuddy.database.NotificationRuleDao
import com.heckmannch.birthdaybuddy.database.PendingNotification
import com.heckmannch.birthdaybuddy.database.PendingNotificationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val appSettingsDao: AppSettingsDao,
) {
    val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()

    val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { it ?: AppSettings() }

    suspend fun updateSettings(
        notificationsEnabled: Boolean? = null,
        persistentNotifications: Boolean? = null,
        swipeHintShown: Boolean? = null,
        onboardingCompleted: Boolean? = null,
        lastSyncTimestamp: Long? = null
    ) {
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
