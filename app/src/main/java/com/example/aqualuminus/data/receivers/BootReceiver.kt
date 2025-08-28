package com.example.aqualuminus.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.aqualuminus.data.factory.ServiceFactory
import com.example.aqualuminus.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, rescheduling UV cleaning tasks")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = ScheduleRepository()
                    val schedulesResult = repository.getSchedules()

                    if (schedulesResult.isSuccess) {
                        val schedules = schedulesResult.getOrNull() ?: emptyList()
                        val activeSchedules = schedules.filter { it.isActive }

                        Log.d(TAG, "Found ${schedules.size} total schedules, ${activeSchedules.size} active")

                        if (activeSchedules.isNotEmpty()) {
                            val scheduleService = ServiceFactory.getScheduleService(context)
                            scheduleService.rescheduleAll(context, activeSchedules)
                            Log.d(TAG, "Successfully rescheduled ${activeSchedules.size} active schedules")
                        } else {
                            Log.d(TAG, "No active schedules to reschedule")
                        }
                    } else {
                        Log.e(TAG, "Failed to get schedules: ${schedulesResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule on boot", e)
                }
            }
        }
    }
}