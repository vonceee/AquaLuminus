package com.example.aqualuminus.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.aqualuminus.MainActivity
import com.example.aqualuminus.R
import com.example.aqualuminus.data.repository.ScheduleRepository
import com.example.aqualuminus.data.repository.UVLightRepository
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduleService {
    companion object {
        private const val TAG = "ScheduleService"
        private const val SCHEDULE_WORK_PREFIX = "schedule_work_"
        private const val NOTIFICATION_WORK_PREFIX = "notification_work_"

        // Notification constants
        const val CHANNEL_ID = "uv_cleaning_channel"
        const val NOTIFICATION_ID_BASE = 1000

        // Notification timing (in minutes before the cleaning starts)
        const val NOTIFICATION_ADVANCE_TIME = 5

        fun scheduleUVCleaning(context: Context, schedule: SavedSchedule) {
            val workManager = WorkManager.getInstance(context)

            // Cancel existing work if updating
            workManager.cancelUniqueWork("${SCHEDULE_WORK_PREFIX}${schedule.id}")
            workManager.cancelUniqueWork("${NOTIFICATION_WORK_PREFIX}${schedule.id}")

            if (!schedule.isActive) {
                Log.d(TAG, "Schedule ${schedule.name} is inactive, skipping")
                return
            }

            // Calculate next run time
            val nextRunTime = calculateNextRunTime(schedule)
            val delay = nextRunTime - System.currentTimeMillis()

            if (delay <= 0) {
                Log.w(TAG, "Schedule ${schedule.name} is in the past, skipping")
                return
            }

            // Create notification channel
            createNotificationChannel(context)

            // Schedule notification (5 minutes before cleaning)
            scheduleNotification(context, schedule, nextRunTime, delay)

            // Schedule the actual UV cleaning work
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

            Log.d(TAG, "Scheduled UV cleaning for ${schedule.name} at ${Date(nextRunTime)}")
        }

        private fun scheduleNotification(
            context: Context,
            schedule: SavedSchedule,
            cleaningStartTime: Long,
            cleaningDelay: Long
        ) {
            val notificationDelay = cleaningDelay - TimeUnit.MINUTES.toMillis(NOTIFICATION_ADVANCE_TIME.toLong())

            // Only schedule notification if there's enough time
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

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${NOTIFICATION_WORK_PREFIX}${schedule.id}",
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )

            Log.d(TAG, "Scheduled notification for ${schedule.name} at ${Date(cleaningStartTime - TimeUnit.MINUTES.toMillis(NOTIFICATION_ADVANCE_TIME.toLong()))}")
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "UV Cleaning Notifications"
                val descriptionText = "Notifications for scheduled UV cleaning sessions"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun cancelSchedule(context: Context, scheduleId: String) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("${SCHEDULE_WORK_PREFIX}$scheduleId")
            workManager.cancelUniqueWork("${NOTIFICATION_WORK_PREFIX}$scheduleId")
            Log.d(TAG, "Cancelled schedule and notifications: $scheduleId")
        }

        fun rescheduleAll(context: Context, schedules: List<SavedSchedule>) {
            schedules.forEach { schedule ->
                scheduleUVCleaning(context, schedule)
            }
        }

        // ... rest of the existing methods (calculateNextRunTime, parseTime) remain the same ...
        private fun calculateNextRunTime(schedule: SavedSchedule): Long {
            val calendar = Calendar.getInstance()
            val currentDay = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            val currentTime = calendar.timeInMillis

            // Parse schedule time - handle both 24-hour (HH:mm) and 12-hour (HH:mm AM/PM) formats
            val (scheduleHour, scheduleMinute) = parseTime(schedule.time)

            // Convert day abbreviations to Calendar constants
            val dayMap = mapOf(
                "Sun" to Calendar.SUNDAY,
                "Mon" to Calendar.MONDAY,
                "Tue" to Calendar.TUESDAY,
                "Wed" to Calendar.WEDNESDAY,
                "Thu" to Calendar.THURSDAY,
                "Fri" to Calendar.FRIDAY,
                "Sat" to Calendar.SATURDAY
            )

            // Find the next occurrence
            var nextRunCalendar = Calendar.getInstance()
            var foundNext = false

            // Check today first
            if (schedule.days.contains(currentDay)) {
                nextRunCalendar.set(Calendar.HOUR_OF_DAY, scheduleHour)
                nextRunCalendar.set(Calendar.MINUTE, scheduleMinute)
                nextRunCalendar.set(Calendar.SECOND, 0)
                nextRunCalendar.set(Calendar.MILLISECOND, 0)

                if (nextRunCalendar.timeInMillis > currentTime) {
                    foundNext = true
                }
            }

            // If not found today, check the next 7 days
            if (!foundNext) {
                for (i in 1..7) {
                    nextRunCalendar = Calendar.getInstance()
                    nextRunCalendar.add(Calendar.DAY_OF_YEAR, i)

                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(nextRunCalendar.time)
                    if (schedule.days.contains(dayName)) {
                        nextRunCalendar.set(Calendar.HOUR_OF_DAY, scheduleHour)
                        nextRunCalendar.set(Calendar.MINUTE, scheduleMinute)
                        nextRunCalendar.set(Calendar.SECOND, 0)
                        nextRunCalendar.set(Calendar.MILLISECOND, 0)
                        break
                    }
                }
            }

            return nextRunCalendar.timeInMillis
        }

        /**
         * Parse time string that can be in either 24-hour (HH:mm) or 12-hour (HH:mm AM/PM) format
         * Returns Pair<hour24, minute>
         */
        private fun parseTime(timeString: String): Pair<Int, Int> {
            return try {
                if (timeString.contains("AM") || timeString.contains("PM")) {
                    // 12-hour format: "10:30 AM" or "02:45 PM"
                    val isAM = timeString.contains("AM")
                    val timePart = timeString.replace("AM", "").replace("PM", "").trim()
                    val parts = timePart.split(":")

                    var hour = parts[0].toInt()
                    val minute = parts[1].toInt()

                    // Convert to 24-hour format
                    hour = when {
                        isAM && hour == 12 -> 0  // 12:xx AM = 00:xx
                        !isAM && hour != 12 -> hour + 12  // x:xx PM = (x+12):xx (except 12 PM)
                        else -> hour  // AM (not 12) or 12 PM stays the same
                    }

                    Pair(hour, minute)
                } else {
                    // 24-hour format: "14:30"
                    val parts = timeString.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    Pair(hour, minute)
                }
            } catch (e: Exception) {
                Log.e("ScheduleService", "Error parsing time: $timeString", e)
                // Default to current time if parsing fails
                val now = Calendar.getInstance()
                Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            }
        }
    }
}

// New NotificationWorker class
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: return Result.failure()
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            val durationMinutes = inputData.getInt("duration_minutes", 30)
            val cleaningStartTime = inputData.getLong("cleaning_start_time", 0)

            showNotification(scheduleId, scheduleName, durationMinutes, cleaningStartTime)

            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Failed to show notification", e)
            Result.failure()
        }
    }

    private fun showNotification(
        scheduleId: String,
        scheduleName: String,
        durationMinutes: Int,
        cleaningStartTime: Long
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTimeString = timeFormat.format(Date(cleaningStartTime))

        // Intent to open the app when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ScheduleService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UV Cleaning Starting Soon")
            .setContentText("$scheduleName will start at $startTimeString for $durationMinutes minutes")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$scheduleName is scheduled to start at $startTimeString and will run for $durationMinutes minutes. Make sure the aquarium area is clear.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(ScheduleService.NOTIFICATION_ID_BASE + scheduleId.hashCode(), notification)
        }

        Log.d("NotificationWorker", "Notification shown for $scheduleName")
    }
}

// Enhanced UVCleaningWorker with start notification
class UVCleaningWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val uvLightRepository = UVLightRepository()
    private val scheduleRepository = ScheduleRepository()

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: return Result.failure()
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            val durationMinutes = inputData.getInt("duration_minutes", 30)

            Log.d("UVCleaningWorker", "Starting UV cleaning: $scheduleName")

            // Show "cleaning started" notification
            showCleaningStartedNotification(scheduleId, scheduleName, durationMinutes)

            // Turn on UV light
            val turnOnResult = uvLightRepository.turnOnUVLight()
            if (turnOnResult.isFailure) {
                Log.e("UVCleaningWorker", "Failed to turn on UV light: ${turnOnResult.exceptionOrNull()?.message}")
                showErrorNotification(scheduleName, "Failed to start UV cleaning")
                return Result.retry()
            }

            Log.d("UVCleaningWorker", "UV light turned on, running for $durationMinutes minutes")

            // Schedule the turn-off work
            val turnOffWork = OneTimeWorkRequestBuilder<UVTurnOffWorker>()
                .setInitialDelay(durationMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(
                    Data.Builder()
                        .putString("schedule_id", scheduleId)
                        .putString("schedule_name", scheduleName)
                        .build()
                )
                .build()

            WorkManager.getInstance(applicationContext).enqueue(turnOffWork)

            // Reschedule this schedule for next occurrence
            rescheduleNext(scheduleId)

            Result.success()
        } catch (e: Exception) {
            Log.e("UVCleaningWorker", "UV cleaning failed", e)
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            showErrorNotification(scheduleName, "UV cleaning encountered an error")
            Result.retry()
        }
    }

    private fun showCleaningStartedNotification(scheduleId: String, scheduleName: String, durationMinutes: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ScheduleService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UV Cleaning Started")
            .setContentText("$scheduleName is now running for $durationMinutes minutes")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(ScheduleService.NOTIFICATION_ID_BASE + scheduleId.hashCode() + 1, notification)
        }
    }

    private fun showErrorNotification(scheduleName: String, errorMessage: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ScheduleService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("UV Cleaning Error")
            .setContentText("$scheduleName: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(ScheduleService.NOTIFICATION_ID_BASE + scheduleName.hashCode() + 100, notification)
        }
    }

    private suspend fun rescheduleNext(scheduleId: String) {
        try {
            val schedulesResult = scheduleRepository.getSchedules()
            if (schedulesResult.isSuccess) {
                val schedule = schedulesResult.getOrNull()?.find { it.id == scheduleId }
                if (schedule != null && schedule.isActive) {
                    ScheduleService.scheduleUVCleaning(applicationContext, schedule)
                }
            }
        } catch (e: Exception) {
            Log.e("UVCleaningWorker", "Failed to reschedule", e)
        }
    }
}

// Enhanced UVTurnOffWorker with completion notification
class UVTurnOffWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val uvLightRepository = UVLightRepository()

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: ""
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            Log.d("UVTurnOffWorker", "Turning off UV light for: $scheduleName")

            val result = uvLightRepository.turnOffUVLight()
            if (result.isSuccess) {
                Log.d("UVTurnOffWorker", "UV light turned off successfully")
                showCompletionNotification(scheduleId, scheduleName)
                Result.success()
            } else {
                Log.e("UVTurnOffWorker", "Failed to turn off UV light: ${result.exceptionOrNull()?.message}")
                showErrorNotification(scheduleName, "Failed to turn off UV light")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("UVTurnOffWorker", "Failed to turn off UV light", e)
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            showErrorNotification(scheduleName, "Error turning off UV light")
            Result.retry()
        }
    }

    private fun showCompletionNotification(scheduleId: String, scheduleName: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleId.hashCode() + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ScheduleService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("UV Cleaning Complete")
            .setContentText("$scheduleName has finished successfully")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(ScheduleService.NOTIFICATION_ID_BASE + scheduleId.hashCode() + 2, notification)
        }
    }

    private fun showErrorNotification(scheduleName: String, errorMessage: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            scheduleName.hashCode() + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ScheduleService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("UV Cleaning Error")
            .setContentText("$scheduleName: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(ScheduleService.NOTIFICATION_ID_BASE + scheduleName.hashCode() + 200, notification)
        }
    }
}

// BootReceiver remains the same
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling UV cleaning tasks")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = ScheduleRepository()
                    val schedulesResult = repository.getSchedules()

                    if (schedulesResult.isSuccess) {
                        val schedules = schedulesResult.getOrNull() ?: emptyList()
                        ScheduleService.rescheduleAll(context, schedules)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule on boot", e)
                }
            }
        }
    }
}