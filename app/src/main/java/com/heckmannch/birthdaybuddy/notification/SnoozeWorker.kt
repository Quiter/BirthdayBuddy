package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SnoozeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val daysBefore = inputData.getInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
        val pendingId = inputData.getInt(NotificationActions.EXTRA_PENDING_ID, -1)
        val lookupKeys = inputData.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS) ?: return Result.failure()

        val rawKeys = lookupKeys.map { it.substringAfter(":") }.toSet()
        val allContacts = contactRepository.allContacts.first()
        val targetContacts = allContacts.filter { it.lookupKey in rawKeys }

        if (targetContacts.isNotEmpty()) {
            val eventTypeStr = inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)
            val eventType = if (eventTypeStr != null) {
                try {
                    EventType.valueOf(eventTypeStr)
                } catch (e: IllegalArgumentException) {
                    EventType.BIRTHDAY
                }
            } else {
                val firstKey = lookupKeys.firstOrNull() ?: ""
                when {
                    firstKey.startsWith("anniversary:") -> EventType.ANNIVERSARY
                    firstKey.startsWith("nameday:") -> EventType.NAME_DAY
                    else -> EventType.BIRTHDAY
                }
            }
            notificationHelper.showBirthdayNotification(
                targetContacts,
                daysBefore,
                pendingId,
                eventType
            )
        }

        return Result.success()
    }
}
