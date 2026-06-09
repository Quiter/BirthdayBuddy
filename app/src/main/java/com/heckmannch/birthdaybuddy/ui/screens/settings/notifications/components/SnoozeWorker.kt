package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
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
        val daysBefore = inputData.getInt("DAYS_BEFORE", 0)
        val pendingId = inputData.getInt("PENDING_ID", -1)
        val lookupKeys = inputData.getStringArray("LOOKUP_KEYS") ?: return Result.failure()

        val rawKeys = lookupKeys.map { it.substringAfter(":") }.toSet()
        val allContacts = contactRepository.allContacts.first()
        val targetContacts = allContacts.filter { it.lookupKey in rawKeys }

        if (targetContacts.isNotEmpty()) {
            val firstKey = lookupKeys.firstOrNull() ?: ""
            val eventType = when {
                firstKey.startsWith("anniversary:") -> "anniversary"
                firstKey.startsWith("nameday:") -> "nameday"
                else -> "birthday"
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
