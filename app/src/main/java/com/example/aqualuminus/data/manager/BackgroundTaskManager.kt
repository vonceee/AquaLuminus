package com.example.aqualuminus.data.manager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackgroundTaskManager(
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "BackgroundTaskManager"
        private const val STATUS_CHECK_INTERVAL = 5000L
        private const val DURATION_UPDATE_INTERVAL = 1000L
        private const val ERROR_RETRY_INTERVAL = 10000L
    }

    private var monitoringJob: Job? = null
    private var durationJob: Job? = null

    fun startStatusMonitoring(
        shouldMonitor: () -> Boolean,
        onStatusCheck: suspend () -> Unit
    ) {
        stopStatusMonitoring()
        monitoringJob = coroutineScope.launch {
            while (true) {
                try {
                    if (shouldMonitor()) {
                        onStatusCheck()
                    }
                    delay(STATUS_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Status monitoring error", e)
                    delay(ERROR_RETRY_INTERVAL)
                }
            }
        }
    }

    fun startDurationTracking(
        shouldTrack: () -> Boolean,
        onUpdateDuration: () -> Unit
    ) {
        stopDurationTracking()
        durationJob = coroutineScope.launch {
            while (true) {
                if (shouldTrack()) {
                    onUpdateDuration()
                }
                delay(DURATION_UPDATE_INTERVAL)
            }
        }
    }

    fun stopAllTasks() {
        stopStatusMonitoring()
        stopDurationTracking()
    }

    private fun stopStatusMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun stopDurationTracking() {
        durationJob?.cancel()
        durationJob = null
    }
}