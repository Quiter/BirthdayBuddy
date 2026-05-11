package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.components

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy2.repository.ContactRepository
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

        val allContacts = contactRepository.allContacts.first()
        val targetContacts = allContacts.filter { it.lookupKey in lookupKeys }

        if (targetContacts.isNotEmpty()) {
            notificationHelper.showBirthdayNotification(targetContacts, daysBefore, pendingId)
        }

        return Result.success()
    }
}
