package com.example.aqualuminus.ui.screens.schedule.model

data class SavedSchedule(
    val id: String = "",
    val name: String = "",
    val days: List<String> = emptyList(),
    val time: String = "",
    val durationMinutes: Int = 30, // Default 30 minutes
    var isActive: Boolean = true,
    val nextRun: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", emptyList(), "", 30, true, null, System.currentTimeMillis())
}