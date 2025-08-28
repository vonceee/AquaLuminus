package com.example.aqualuminus.data.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeCalculator {
    companion object {
        private const val TAG = "TimeCalculator"
    }

    fun calculateNextRunTime(days: List<String>, time: String): Long {
        val calendar = Calendar.getInstance()
        val currentDay = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
        val currentTime = calendar.timeInMillis

        val (scheduleHour, scheduleMinute) = parseTime(time)

        var nextRunCalendar = Calendar.getInstance()
        var foundNext = false

        // Check today first
        if (days.contains(currentDay)) {
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
                if (days.contains(dayName)) {
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

    fun parseTime(timeString: String): Pair<Int, Int> {
        return try {
            if (timeString.contains("AM") || timeString.contains("PM")) {
                val isAM = timeString.contains("AM")
                val timePart = timeString.replace("AM", "").replace("PM", "").trim()
                val parts = timePart.split(":")

                var hour = parts[0].toInt()
                val minute = parts[1].toInt()

                hour = when {
                    isAM && hour == 12 -> 0
                    !isAM && hour != 12 -> hour + 12
                    else -> hour
                }

                Pair(hour, minute)
            } else {
                val parts = timeString.split(":")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                Pair(hour, minute)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeString", e)
            val now = Calendar.getInstance()
            Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }
    }

    fun isScheduleInPast(days: List<String>, time: String): Boolean {
        val nextRunTime = calculateNextRunTime(days, time)
        return nextRunTime <= System.currentTimeMillis()
    }
}