package com.example.aqualuminus.ui.screens.dashboard.model

data class SystemStatus(
    val type: StatusType,
    val message: String
)

enum class StatusType {
    NORMAL, WARNING, ERROR
}