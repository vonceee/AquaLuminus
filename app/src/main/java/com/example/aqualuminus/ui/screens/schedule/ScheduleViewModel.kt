package com.example.aqualuminus.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.repository.ScheduleRepository
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
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
                    _schedules.value = scheduleList
                    _error.value = null
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
                    _schedules.value = scheduleList
                }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to delete schedule"
                }
        }
    }

    fun updateScheduleStatus(scheduleId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateScheduleStatus(scheduleId, isActive)
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to update schedule status"
                }
        }
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
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            return ScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}