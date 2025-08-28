package com.example.aqualuminus.data.network

import android.content.Context
import android.util.Log
import com.example.aqualuminus.data.notification.UVNotificationManager
import com.example.aqualuminus.data.utils.TimeCalculator
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import java.util.Date

class ScheduleService(
    private val workManager: ScheduleWorkManager,
    private val notificationManager: UVNotificationManager,
    private val timeCalculator: TimeCalculator
) {
    companion object {
        private const val TAG = "ScheduleService"
    }

    fun scheduleUVCleaning(context: Context, schedule: SavedSchedule) {
        // Cancel existing work
        workManager.cancelScheduleWork(schedule.id)

        if (!schedule.isActive) {
            Log.d(TAG, "Schedule ${schedule.name} is inactive, skipping")
            return
        }

        // Calculate next run time
        val nextRunTime = timeCalculator.calculateNextRunTime(schedule.days, schedule.time)
        val delay = nextRunTime - System.currentTimeMillis()

        if (delay <= 0) {
            Log.w(TAG, "Schedule ${schedule.name} is in the past, skipping")
            return
        }

        // Schedule notification work
        workManager.enqueueNotificationWork(schedule, nextRunTime, delay)

        // Schedule cleaning work
        workManager.enqueueCleaningWork(schedule, delay)

        Log.d(TAG, "Scheduled UV cleaning for ${schedule.name} at ${Date(nextRunTime)}")
    }

    fun cancelSchedule(context: Context, scheduleId: String) {
        workManager.cancelScheduleWork(scheduleId)
        Log.d(TAG, "Cancelled schedule: $scheduleId")
    }

    fun rescheduleAll(context: Context, schedules: List<SavedSchedule>) {
        schedules.forEach { schedule ->
            scheduleUVCleaning(context, schedule)
        }
        Log.d(TAG, "Rescheduled ${schedules.size} schedules")
    }
}