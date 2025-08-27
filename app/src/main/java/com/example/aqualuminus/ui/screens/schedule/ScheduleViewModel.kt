package com.example.aqualuminus.ui.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.network.ScheduleService
import com.example.aqualuminus.data.repository.ScheduleRepository
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository(),
    private val context: Context
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<SavedSchedule>>(emptyList())
    val schedules: StateFlow<List<SavedSchedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        loadSchedules()
        observeSchedules()
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
            // Default to current time if parsing fails
            val now = Calendar.getInstance()
            Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }
    }

    fun saveSchedule(schedule: SavedSchedule) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val scheduleWithId = schedule.copy(
                id = if (schedule.id.isEmpty()) UUID.randomUUID().toString() else schedule.id
            )

            repository.saveSchedule(scheduleWithId)
                .onSuccess {
                    _saveResult.value = SaveResult.Success
                    _error.value = null

                    // Schedule the UV cleaning task
                    if (scheduleWithId.isActive) {
                        ScheduleService.scheduleUVCleaning(context, scheduleWithId)
                    }
                }
                .onFailure { exception ->
                    _saveResult.value = SaveResult.Error(exception.message ?: "Failed to save schedule")
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    fun loadSchedules() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getSchedules()
                .onSuccess { scheduleList ->
                    _schedules.value = scheduleList.map { schedule ->
                        schedule.copy(nextRun = calculateNextRun(schedule))
                    }
                    _error.value = null

                    // Reschedule all active schedules
                    ScheduleService.rescheduleAll(context, scheduleList.filter { it.isActive })
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load schedules"
                }

            _isLoading.value = false
        }
    }

    private fun observeSchedules() {
        viewModelScope.launch {
            repository.observeSchedules()
                .catch { exception ->
                    _error.value = exception.message ?: "Failed to observe schedules"
                }
                .collect { scheduleList ->
                    _schedules.value = scheduleList.map { schedule ->
                        schedule.copy(nextRun = calculateNextRun(schedule))
                    }
                }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            // Cancel the scheduled task first
            ScheduleService.cancelSchedule(context, scheduleId)

            repository.deleteSchedule(scheduleId)
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to delete schedule"
                }
        }
    }

    fun updateScheduleStatus(scheduleId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateScheduleStatus(scheduleId, isActive)
                .onSuccess {
                    val schedule = _schedules.value.find { it.id == scheduleId }
                    if (schedule != null) {
                        if (isActive) {
                            // Schedule the task
                            ScheduleService.scheduleUVCleaning(context, schedule.copy(isActive = isActive))
                        } else {
                            // Cancel the task
                            ScheduleService.cancelSchedule(context, scheduleId)
                        }
                    }
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to update schedule status"
                }
        }
    }

    private fun calculateNextRun(schedule: SavedSchedule): String {
        if (!schedule.isActive) return "Paused"

        try {
            val calendar = Calendar.getInstance()
            val currentDay = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
            val currentTime = calendar.timeInMillis

            // Parse schedule time - handle both formats
            val (scheduleHour, scheduleMinute) = parseTime(schedule.time)

            // Check if it's today and time hasn't passed
            if (schedule.days.contains(currentDay)) {
                val todayScheduleTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, scheduleHour)
                    set(Calendar.MINUTE, scheduleMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (todayScheduleTime.timeInMillis > currentTime) {
                    return "Today at ${formatTime(scheduleHour, scheduleMinute)}"
                }
            }

            // Find next occurrence
            val dayMap = mapOf(
                "Sun" to Calendar.SUNDAY,
                "Mon" to Calendar.MONDAY,
                "Tue" to Calendar.TUESDAY,
                "Wed" to Calendar.WEDNESDAY,
                "Thu" to Calendar.THURSDAY,
                "Fri" to Calendar.FRIDAY,
                "Sat" to Calendar.SATURDAY
            )

            for (i in 1..7) {
                val nextDay = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, i)
                }
                val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(nextDay.time)

                if (schedule.days.contains(dayName)) {
                    return when (i) {
                        1 -> "Tomorrow at ${formatTime(scheduleHour, scheduleMinute)}"
                        else -> "$dayName at ${formatTime(scheduleHour, scheduleMinute)}"
                    }
                }
            }

            return "Next run not scheduled"
        } catch (e: Exception) {
            return "Invalid schedule"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun clearError() {
        _error.value = null
    }

    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}

class ScheduleViewModelFactory(
    private val repository: ScheduleRepository = ScheduleRepository(),
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            return ScheduleViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}