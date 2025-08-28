package com.example.aqualuminus.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aqualuminus.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UVNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "UVNotificationManager"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "UV Cleaning Notifications"
            val descriptionText = "Notifications for scheduled UV cleaning sessions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NotificationConstants.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAdvanceNotification(
        scheduleId: String,
        scheduleName: String,
        durationMinutes: Int,
        cleaningStartTime: Long
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTimeString = timeFormat.format(Date(cleaningStartTime))

        val pendingIntent = createMainActivityPendingIntent(scheduleId.hashCode())

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UV Cleaning Starting Soon")
            .setContentText("$scheduleName will start at $startTimeString for $durationMinutes minutes")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$scheduleName is scheduled to start at $startTimeString and will run for $durationMinutes minutes. Make sure the aquarium area is clear.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        val notificationId = NotificationConstants.NOTIFICATION_ID_BASE +
                scheduleId.hashCode() +
                NotificationConstants.ADVANCE_NOTIFICATION_OFFSET

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Advance notification shown for $scheduleName")
    }

    fun showStartNotification(scheduleId: String, scheduleName: String, durationMinutes: Int) {
        val pendingIntent = createMainActivityPendingIntent(
            scheduleId.hashCode() + NotificationConstants.START_NOTIFICATION_OFFSET
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UV Cleaning Started")
            .setContentText("$scheduleName is now running for $durationMinutes minutes")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NotificationConstants.NOTIFICATION_ID_BASE +
                scheduleId.hashCode() +
                NotificationConstants.START_NOTIFICATION_OFFSET

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Start notification shown for $scheduleName")
    }

    fun showCompletionNotification(scheduleId: String, scheduleName: String) {
        val pendingIntent = createMainActivityPendingIntent(
            scheduleId.hashCode() + NotificationConstants.COMPLETION_NOTIFICATION_OFFSET
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("UV Cleaning Complete")
            .setContentText("$scheduleName has finished successfully")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NotificationConstants.NOTIFICATION_ID_BASE +
                scheduleId.hashCode() +
                NotificationConstants.COMPLETION_NOTIFICATION_OFFSET

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Completion notification shown for $scheduleName")
    }

    fun showErrorNotification(scheduleId: String, scheduleName: String, errorMessage: String) {
        val pendingIntent = createMainActivityPendingIntent(
            scheduleId.hashCode() + NotificationConstants.ERROR_NOTIFICATION_OFFSET
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("UV Cleaning Error")
            .setContentText("$scheduleName: $errorMessage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = NotificationConstants.NOTIFICATION_ID_BASE +
                scheduleId.hashCode() +
                NotificationConstants.ERROR_NOTIFICATION_OFFSET

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Error notification shown for $scheduleName")
    }

    private fun createMainActivityPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}