package com.example.aqualuminus.ui.screens.schedule.utils

object ScheduleFormatter {

    fun formatTime(time: String): String {
        val parts = time.split(":")
        if (parts.size != 2) return time

        val hour = parts[0].toIntOrNull() ?: return time
        val minute = parts[1].toIntOrNull() ?: return time

        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        return String.format("%d:%02d %s", displayHour, minute, ampm)
    }

    fun formatDays(days: List<String>): String {
        return when {
            days.size == 7 -> "Every day"
            days.size == 5 && !days.contains("Sat") && !days.contains("Sun") -> "Weekdays"
            days.size == 2 && days.contains("Sat") && days.contains("Sun") -> "Weekends"
            days.size == 1 -> days.first()
            else -> days.joinToString(", ")
        }
    }
}