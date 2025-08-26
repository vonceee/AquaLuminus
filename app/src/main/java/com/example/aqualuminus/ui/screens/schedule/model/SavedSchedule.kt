package com.example.aqualuminus.ui.screens.schedule.model

data class SavedSchedule(
    val id: String,
    val name: String,
    val days: List<String>,
    val time: String,
    var isActive: Boolean,
    val nextRun: String? = null
)