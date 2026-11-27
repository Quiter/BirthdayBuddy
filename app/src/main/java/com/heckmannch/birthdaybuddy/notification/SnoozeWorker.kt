package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.util.NotificationKeyUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker responsible for re-posting snoozed or dismissed birthday/event notifications.
 *
 * Verifies global notification settings and the pending notification's completion state
 * before querying contacts or displaying a notification.
 */
@HiltWorker
class SnoozeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettingsImmediate()
        if (!settings.notificationsEnabled) {
            return Result.success()
        }

        val daysBefore = inputData.getInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
        val pendingId = inputData.getInt(NotificationActions.EXTRA_PENDING_ID, -1)
        val lookupKeys = inputData.getNullableStringArray(NotificationActions.EXTRA_LOOKUP_KEYS) ?: return Result.failure()

        if (pendingId != -1) {
            val pendingNotification = notificationRepository.getPendingNotificationById(pendingId)
            if (pendingNotification?.isDone == true) {
                return Result.success()
            }
        }

        val rawKeys = lookupKeys.filterNotNull().map { NotificationKeyUtils.extractRawKey(it) }.toSet()
        val allContacts = contactRepository.getAllContactsImmediate()
        val targetContacts = allContacts.filter { it.lookupKey in rawKeys }

        if (targetContacts.isNotEmpty()) {
            val firstKey = lookupKeys.filterNotNull().firstOrNull() ?: ""
            val eventTypeStr = inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)
            val eventType = if (eventTypeStr != null) {
                try {
                    EventType.valueOf(eventTypeStr)
                } catch (e: IllegalArgumentException) {
                    EventType.BIRTHDAY
                }
            } else {
                NotificationKeyUtils.extractEventType(firstKey)
            }
            val customEventKey = NotificationKeyUtils.extractCustomEventKey(firstKey)
            notificationHelper.showBirthdayNotification(
                contacts = targetContacts,
                daysBefore = daysBefore,
                pendingId = pendingId,
                eventType = eventType,
                customEventKey = customEventKey,
            )
        }

        return Result.success()
    }
}
