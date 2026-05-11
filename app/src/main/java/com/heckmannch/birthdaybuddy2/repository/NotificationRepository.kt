package com.heckmannch.birthdaybuddy2.repository

import com.heckmannch.birthdaybuddy2.database.NotificationRule
import com.heckmannch.birthdaybuddy2.database.NotificationRuleDao
import com.heckmannch.birthdaybuddy2.database.PendingNotification
import com.heckmannch.birthdaybuddy2.database.PendingNotificationDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
) {
    val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()

    suspend fun getAllRulesImmediate(): List<NotificationRule> = notificationRuleDao.getAllRulesImmediate()

    suspend fun insertRule(rule: NotificationRule) = notificationRuleDao.insertRule(rule)

    suspend fun updateRule(rule: NotificationRule) = notificationRuleDao.updateRule(rule)

    suspend fun deleteRule(rule: NotificationRule) = notificationRuleDao.deleteRule(rule)

    // Pending Notifications
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification> = 
        pendingNotificationDao.getActiveNotificationsImmediate()

    suspend fun insertPendingNotification(notification: PendingNotification) = 
        pendingNotificationDao.insert(notification)

    suspend fun markAsDone(id: Int) = pendingNotificationDao.markAsDone(id)
}
