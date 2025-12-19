package com.example.taskmanagerapp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// BroadcastReceiver that listens for alarm events triggered by AlarmManager.
// When the alarm fires, this receiver builds and shows a task reminder notification.
class TaskReminderReceiver : BroadcastReceiver() {

    // This method is called automatically when the alarm is triggered.
    // It reads task details from the Intent and displays a notification.
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {

        // Extract task information sent with the alarm.
        // These values are used to populate the notification content.
        val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
        val taskId = intent.getLongExtra("task_id", 0L)

        // Unique channel ID for task reminder notifications.
        // Must remain constant for proper notification grouping.
        val channelId = "task_reminder_channel"

        // Create notification channel (Android 8+)
        // Notification channels are required to display notifications on newer Android versions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                // Description shown in system notification settings
                description = "Daily reminders for high priority tasks"
            }
            // Register the notification channel with the system
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Build the notification UI.
        // This defines how the notification looks and behaves
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("High Priority Task") //static label
            .setContentText(taskTitle) //actual task name
            .setPriority(NotificationCompat.PRIORITY_HIGH) //more visible notification
            .setAutoCancel(true) //notification disappears when tapped
            .build()

        // Display the notification using NotificationManager.
        // taskId is used so each task notification is uniquely identifiable.
        NotificationManagerCompat.from(context)
            .notify(taskId.toInt(), notification)
    }
}