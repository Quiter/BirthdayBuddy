package com.heckmannch.birthdaybuddy.data.repository

import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.data.mapper.NotificationRuleMapper
import com.heckmannch.birthdaybuddy.data.mapper.PendingNotificationMapper
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationRuleDao: NotificationRuleDao,
    private val pendingNotificationDao: PendingNotificationDao,
    private val appSettingsDao: AppSettingsDao,
    private val notificationScheduler: NotificationScheduler,
    private val appSettingsMapper: AppSettingsMapper,
    private val notificationRuleMapper: NotificationRuleMapper,
    private val pendingNotificationMapper: PendingNotificationMapper,
) : NotificationRepository {
    private val settingsMutex = Mutex()

    override val allRules: Flow<List<NotificationRule>> = notificationRuleDao.getAllRules()
        .map { entities -> entities.map { notificationRuleMapper.toDomain(it) } }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    override val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { entity -> appSettingsMapper.toDomain(entity ?: AppSettingsEntity()) }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    override suspend fun syncScheduling() = withContext(Dispatchers.IO) {
        val enabled = appSettingsDao.getSettingsImmediate()?.notificationsEnabled ?: false
        val rules = notificationRuleDao.getAllRulesImmediate()
        if (enabled && rules.isNotEmpty()) {
            notificationScheduler.scheduleNext(rules.map { notificationRuleMapper.toDomain(it) })
        } else {
            notificationScheduler.cancelNotification()
        }
    }

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
        themeMode: String?,
        themeAmoled: Boolean?,
        themeAccent: String?
    ) {
        settingsMutex.withLock {
            val current = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
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

    override suspend fun getAllRulesImmediate(): List<NotificationRule> =
        notificationRuleDao.getAllRulesImmediate().map { notificationRuleMapper.toDomain(it) }

    override suspend fun insertRule(rule: NotificationRule) {
        notificationRuleDao.upsertRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    override suspend fun updateRule(rule: NotificationRule) {
        notificationRuleDao.updateRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    override suspend fun deleteRule(rule: NotificationRule) {
        notificationRuleDao.deleteRule(notificationRuleMapper.toEntity(rule))
        syncScheduling()
    }

    // Pending Notifications
    override suspend fun getActiveNotificationsImmediate(): List<PendingNotification> =
        pendingNotificationDao.getActiveNotificationsImmediate()
            .map { pendingNotificationMapper.toDomain(it) }

    override suspend fun insertPendingNotification(notification: PendingNotification): Long =
        pendingNotificationDao.upsert(pendingNotificationMapper.toEntity(notification))

    override suspend fun getPendingNotificationById(id: Int): PendingNotification? =
        pendingNotificationDao.getNotificationById(id)
            ?.let { pendingNotificationMapper.toDomain(it) }

    override suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKey: String
    ): Boolean {
        val pattern = "%\"$lookupKey\"%"
        return pendingNotificationDao.hasNotificationBeenScheduled(year, daysBefore, pattern)
    }

    override suspend fun incrementDismissCount(id: Int) {
        pendingNotificationDao.incrementDismissCount(id)
    }

    override suspend fun markAsDone(id: Int) {
        pendingNotificationDao.markAsDone(id)
    }

    override suspend fun deleteOldNotifications(currentYear: Int) {
        pendingNotificationDao.deleteOldNotifications(currentYear)
    }
}
