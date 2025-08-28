package com.example.aqualuminus.data.network

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aqualuminus.data.notification.NotificationConstants
import com.example.aqualuminus.data.workers.NotificationWorker
import com.example.aqualuminus.data.workers.UVCleaningWorker
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import java.util.concurrent.TimeUnit

class ScheduleWorkManager(private val context: Context) {

    companion object {
        private const val TAG = "ScheduleWorkManager"
        private const val SCHEDULE_WORK_PREFIX = "schedule_work_"
        private const val NOTIFICATION_WORK_PREFIX = "notification_work_"
    }

    private val workManager = WorkManager.getInstance(context)

    fun enqueueCleaningWork(schedule: SavedSchedule, delay: Long) {
        val inputData = Data.Builder()
            .putString("schedule_id", schedule.id)
            .putString("schedule_name", schedule.name)
            .putInt("duration_minutes", schedule.durationMinutes)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<UVCleaningWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("uv_cleaning")
            .addTag(schedule.id)
            .build()

        workManager.enqueueUniqueWork(
            "${SCHEDULE_WORK_PREFIX}${schedule.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Cleaning work enqueued for ${schedule.name}")
    }

    fun enqueueNotificationWork(schedule: SavedSchedule, cleaningStartTime: Long, cleaningDelay: Long) {
        val notificationDelay = cleaningDelay - TimeUnit.MINUTES.toMillis(
            NotificationConstants.NOTIFICATION_ADVANCE_TIME_MINUTES.toLong()
        )

        if (notificationDelay <= 0) {
            Log.d(TAG, "Not enough time for advance notification for ${schedule.name}")
            return
        }

        val notificationData = Data.Builder()
            .putString("schedule_id", schedule.id)
            .putString("schedule_name", schedule.name)
            .putInt("duration_minutes", schedule.durationMinutes)
            .putLong("cleaning_start_time", cleaningStartTime)
            .build()

        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(notificationDelay, TimeUnit.MILLISECONDS)
            .setInputData(notificationData)
            .addTag("uv_notification")
            .addTag(schedule.id)
            .build()

        workManager.enqueueUniqueWork(
            "${NOTIFICATION_WORK_PREFIX}${schedule.id}",
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )

        Log.d(TAG, "Notification work enqueued for ${schedule.name}")
    }

    fun cancelScheduleWork(scheduleId: String) {
        workManager.cancelUniqueWork("${SCHEDULE_WORK_PREFIX}$scheduleId")
        workManager.cancelUniqueWork("${NOTIFICATION_WORK_PREFIX}$scheduleId")
        Log.d(TAG, "Cancelled work for schedule: $scheduleId")
    }

    fun cancelAllWork() {
        workManager.cancelAllWorkByTag("uv_cleaning")
        workManager.cancelAllWorkByTag("uv_notification")
        Log.d(TAG, "Cancelled all UV cleaning work")
    }
}