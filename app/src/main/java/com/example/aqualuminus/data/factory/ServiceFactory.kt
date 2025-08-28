package com.example.aqualuminus.data.factory

import android.content.Context
import com.example.aqualuminus.data.network.ScheduleService
import com.example.aqualuminus.data.network.ScheduleWorkManager
import com.example.aqualuminus.data.notification.UVNotificationManager
import com.example.aqualuminus.data.utils.TimeCalculator

object ServiceFactory {
    private var scheduleService: ScheduleService? = null
    private var notificationManager: UVNotificationManager? = null
    private var workManager: ScheduleWorkManager? = null
    private var timeCalculator: TimeCalculator? = null

    fun getScheduleService(context: Context): ScheduleService {
        return scheduleService ?: run {
            val service = ScheduleService(
                workManager = getWorkManager(context),
                notificationManager = getNotificationManager(context),
                timeCalculator = getTimeCalculator()
            )
            scheduleService = service
            service
        }
    }

    fun getNotificationManager(context: Context): UVNotificationManager {
        return notificationManager ?: run {
            val manager = UVNotificationManager(context.applicationContext)
            notificationManager = manager
            manager
        }
    }

    fun getWorkManager(context: Context): ScheduleWorkManager {
        return workManager ?: run {
            val manager = ScheduleWorkManager(context.applicationContext)
            workManager = manager
            manager
        }
    }

    fun getTimeCalculator(): TimeCalculator {
        return timeCalculator ?: run {
            val calculator = TimeCalculator()
            timeCalculator = calculator
            calculator
        }
    }

    // Call this when app is destroyed to clean up singletons
    fun cleanup() {
        scheduleService = null
        notificationManager = null
        workManager = null
        timeCalculator = null
    }
}