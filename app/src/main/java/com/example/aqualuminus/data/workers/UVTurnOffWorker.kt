package com.example.aqualuminus.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus.data.factory.ServiceFactory
import com.example.aqualuminus.data.repository.UVLightRepository

class UVTurnOffWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UVTurnOffWorker"
    }

    private val uvLightRepository = UVLightRepository(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: "unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            Log.d(TAG, "Turning off UV light for: $scheduleName")

            // Get notification manager from factory
            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)

            // Turn off UV light
            val result = uvLightRepository.turnOffUVLight()
            if (result.isSuccess) {
                Log.d(TAG, "UV light turned off successfully for $scheduleName")
                notificationManager.showCompletionNotification(scheduleId, scheduleName)
                Result.success()
            } else {
                val errorMsg = "Failed to turn off UV light"
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "$errorMsg for $scheduleName: $error")

                notificationManager.showErrorNotification(scheduleId, scheduleName, errorMsg)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off UV light with exception", e)
            val scheduleId = inputData.getString("schedule_id") ?: "unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)
            notificationManager.showErrorNotification(
                scheduleId,
                scheduleName,
                "Error turning off UV light: ${e.message}"
            )

            Result.retry()
        }
    }
}