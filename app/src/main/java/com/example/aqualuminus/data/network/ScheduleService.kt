package com.example.aqualuminus.data.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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

        fun scheduleUVCleaning(context: Context, schedule: SavedSchedule) {
            val workManager = WorkManager.getInstance(context)

            // Cancel existing work if updating
            workManager.cancelUniqueWork("${SCHEDULE_WORK_PREFIX}${schedule.id}")

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

        fun cancelSchedule(context: Context, scheduleId: String) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("${SCHEDULE_WORK_PREFIX}$scheduleId")
            Log.d(TAG, "Cancelled schedule: $scheduleId")
        }

        fun rescheduleAll(context: Context, schedules: List<SavedSchedule>) {
            schedules.forEach { schedule ->
                scheduleUVCleaning(context, schedule)
            }
        }

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

// 2. Create a Worker to handle UV light operations
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

            // Turn on UV light
            val turnOnResult = uvLightRepository.turnOnUVLight()
            if (turnOnResult.isFailure) {
                Log.e("UVCleaningWorker", "Failed to turn on UV light: ${turnOnResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

            Log.d("UVCleaningWorker", "UV light turned on, running for $durationMinutes minutes")

            // Schedule the turn-off work
            val turnOffWork = OneTimeWorkRequestBuilder<UVTurnOffWorker>()
                .setInitialDelay(durationMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(
                    Data.Builder()
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
            Result.retry()
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

// 3. Worker to turn off UV light after duration
class UVTurnOffWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val uvLightRepository = UVLightRepository()

    override suspend fun doWork(): Result {
        return try {
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            Log.d("UVTurnOffWorker", "Turning off UV light for: $scheduleName")

            val result = uvLightRepository.turnOffUVLight()
            if (result.isSuccess) {
                Log.d("UVTurnOffWorker", "UV light turned off successfully")
                Result.success()
            } else {
                Log.e("UVTurnOffWorker", "Failed to turn off UV light: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("UVTurnOffWorker", "Failed to turn off UV light", e)
            Result.retry()
        }
    }
}

// 4. Boot receiver to reschedule after device restart
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling UV cleaning tasks")

            // Use coroutine to handle async operations
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