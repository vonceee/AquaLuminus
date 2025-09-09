package com.example.aqualuminus.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.aqualuminus.data.factory.ServiceFactory
import com.example.aqualuminus.data.repository.ScheduleRepository
import com.example.aqualuminus.data.repository.UVLightRepository
import java.util.concurrent.TimeUnit

class UVCleaningWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UVCleaningWorker"
    }

    private val uvLightRepository = UVLightRepository(applicationContext)
    private val scheduleRepository = ScheduleRepository()

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: return Result.failure()
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            val durationMinutes = inputData.getInt("duration_minutes", 30)

            Log.d(TAG, "Starting UV cleaning: $scheduleName for $durationMinutes minutes")

            // Get notification manager from factory
            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)

            // Show "cleaning started" notification
            notificationManager.showStartNotification(scheduleId, scheduleName, durationMinutes)

            // Turn on UV light
            val turnOnResult = uvLightRepository.turnOnUVLight()
            if (turnOnResult.isFailure) {
                val errorMsg = "Failed to start UV cleaning"
                val error = turnOnResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "$errorMsg: $error")

                notificationManager.showErrorNotification(scheduleId, scheduleName, errorMsg)
                return Result.retry()
            }

            Log.d(TAG, "UV light turned on successfully, scheduling turn-off in $durationMinutes minutes")

            // Schedule the turn-off work
            scheduleTurnOff(scheduleId, scheduleName, durationMinutes)

            // Reschedule this schedule for next occurrence
            rescheduleNext(scheduleId)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "UV cleaning failed with exception", e)
            val scheduleId = inputData.getString("schedule_id") ?: "unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)
            notificationManager.showErrorNotification(
                scheduleId,
                scheduleName,
                "UV cleaning encountered an error: ${e.message}"
            )

            Result.retry()
        }
    }

    private fun scheduleTurnOff(scheduleId: String, scheduleName: String, durationMinutes: Int) {
        try {
            val turnOffWork = OneTimeWorkRequestBuilder<UVTurnOffWorker>()
                .setInitialDelay(durationMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(
                    Data.Builder()
                        .putString("schedule_id", scheduleId)
                        .putString("schedule_name", scheduleName)
                        .build()
                )
                .addTag("uv_turn_off")
                .addTag(scheduleId)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(turnOffWork)
            Log.d(TAG, "Turn-off work scheduled for $scheduleName in $durationMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule turn-off work for $scheduleName", e)
        }
    }

    private suspend fun rescheduleNext(scheduleId: String) {
        try {
            val schedulesResult = scheduleRepository.getSchedules()
            if (schedulesResult.isSuccess) {
                val schedule = schedulesResult.getOrNull()?.find { it.id == scheduleId }
                if (schedule != null && schedule.isActive) {
                    val scheduleService = ServiceFactory.getScheduleService(applicationContext)
                    scheduleService.scheduleUVCleaning(applicationContext, schedule)
                    Log.d(TAG, "Successfully rescheduled next occurrence for ${schedule.name}")
                } else {
                    Log.d(TAG, "Schedule $scheduleId not found or inactive, skipping reschedule")
                }
            } else {
                Log.w(TAG, "Failed to get schedules for rescheduling: ${schedulesResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule next occurrence", e)
        }
    }
}
