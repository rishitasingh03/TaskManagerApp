package com.example.taskmanagerapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.model.TaskPriority
import java.util.Calendar


    //this file is for scheduling and cancelling task reminder.
    //ViewModel calls this class whenever a task is added, edited, completed, or deleted.

object TaskReminderScheduler {


     // Schedule all required reminders for a task.
     // HIGH priority -- daily reminders until completion date
     // Other reminders -- 1/2/3 days before completion date

    fun scheduleReminders(context: Context, task: Task) {

        // Cancel old reminders first to avoid duplicates when task is edited
        cancelAllReminders(context, task)

        // High priority tasks get daily reminders
        if (task.priority == TaskPriority.HIGH) {
            scheduleDailyReminder(context, task)
        }

        // Schedule reminders for 1/2/3 days before completion (if selected)
        task.reminderDaysBefore.forEach { daysBefore ->
            scheduleOneTimeReminder(context, task, daysBefore)
        }
    }


      //Schedules a repeating daily reminder for HIGH priority tasks.
    private fun scheduleDailyReminder(context: Context, task: Task) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = buildIntent(context, task) //which component to trigger
        val pendingIntent = buildPendingIntent(context, task.id, intent) //wraps intent

        // Start reminders from next 9 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        // schedule repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,//wakes device if sleeping
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,//repeats daily
            pendingIntent
        )
    }


    //Schedules a one-time reminder N days before task completion.
    private fun scheduleOneTimeReminder(
        context: Context,
        task: Task,
        daysBefore: Int
    ) {
        val triggerTime =
            task.completionTimeMillis - daysBefore * 24 * 60 * 60 * 1000L

        // Do not schedule reminders in the past
        if (triggerTime <= System.currentTimeMillis()) return
        //required for scheduling alarm
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = buildIntent(context, task)
        val pendingIntent =
            buildPendingIntent(context, task.id + daysBefore, intent)

        // Android 12+ requires explicit permission for exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            // If exact alarms are allowed, schedule exact reminder
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle( //precise timing
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback: schedule inexact alarm to avoid crash
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

        } else {
            // Pre-Android 12 devices can use exact alarms freely
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }



      //Cancels all alarms associated with a task.
     // Called when task is deleted or completed.

    fun cancelAllReminders(context: Context, task: Task) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val ids = mutableListOf(task.id)//Each reminder has a unique request code
        task.reminderDaysBefore.forEach { ids.add(task.id + it) } //Need to cancel all of them

        ids.forEach { id ->
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = buildPendingIntent(context, id, intent)
            alarmManager.cancel(pendingIntent)//cancel alarms
        }
    }

    //   Helpers Functions

//Defines what runs when alarm fires
    private fun buildIntent(context: Context, task: Task): Intent =
        Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("task_title", task.title)
            putExtra("task_id", task.id)
        }

    private fun buildPendingIntent(
        context: Context,
        id: Long,
        intent: Intent
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or //updates existing alarm
                    if (Build.VERSION.SDK_INT >= 23)
                        PendingIntent.FLAG_IMMUTABLE else 0 //required for Android 12+
        )

}

// Computes reminder trigger times for a task.
// This is pure business logic and is separated for unit testing.
// AlarmManager calls are NOT included here.
internal fun computeReminderTimes(task: Task): List<Long> {
    val times = mutableListOf<Long>()

    // High priority tasks need daily reminders
    if (task.priority == TaskPriority.HIGH) {
        times.add(-1L) // marker for daily reminder
    }

    // Calculate reminders for 1 / 2 / 3 days before completion
    task.reminderDaysBefore.forEach { days ->
        val triggerTime =
            task.completionTimeMillis - days * 24 * 60 * 60 * 1000L
        if (triggerTime > System.currentTimeMillis()) {
            times.add(triggerTime)
        }
    }

    return times
}
