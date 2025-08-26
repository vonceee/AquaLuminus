package com.example.aqualuminus.utils

/**
 * Format duration from milliseconds to human readable format
 * @param durationMs Duration in milliseconds
 * @return Formatted string like "2h 30m 45s" or "45s" or "2m 30s"
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0s"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/**
 * Format duration in a compact format
 * @param durationMs Duration in milliseconds
 * @return Formatted string like "2:30:45" or "30:45" or "0:45"
 */
fun formatDurationCompact(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}