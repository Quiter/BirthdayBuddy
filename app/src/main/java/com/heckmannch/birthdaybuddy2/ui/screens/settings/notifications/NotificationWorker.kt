package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.heckmannch.birthdaybuddy2.repository.ContactRepository
import com.heckmannch.birthdaybuddy2.repository.PreferenceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val preferenceRepository: PreferenceRepository
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val isEnabled = preferenceRepository.notificationsEnabled.first()
        
        if (!isEnabled) return Result.success()

        val allContacts = contactRepository.allContacts.first()
        
        val today = LocalDate.now()
        val todayBirthdays = allContacts.filter { 
            (it.birthday.month == today.month) && (it.birthday.dayOfMonth == today.dayOfMonth) 
        }

        if (todayBirthdays.isNotEmpty()) {
            NotificationHelper(context).showBirthdayNotification(todayBirthdays)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "DailyNotificationUpdate"

        fun enqueueDailyNotification(context: Context, hour: Int, minute: Int) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(requiresBatteryNotLow = true)
                .build()

            val request = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateDelay(hour, minute), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag("birthday_notification")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun calculateDelay(hour: Int, minute: Int): Long {
            val now = LocalDateTime.now()
            var target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
            
            if (now.isAfter(target)) {
                target = target.plusDays(1)
            }
            
            return Duration.between(now, target).toMillis()
        }
    }
}
