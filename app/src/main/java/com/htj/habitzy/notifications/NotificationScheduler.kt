package com.htj.habitzy.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.htj.habitzy.R
import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.ReminderDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val reminderDao: ReminderDao,
) {
    companion object {
        const val CHANNEL_ID = "habit_reminders"

        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SNOOZE = "snooze"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.description = "Reminders for your habits"
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** (Re)schedule alarms for all enabled reminders. Call on app start and after reboot. */
    fun rescheduleAll() {
        val (reminders, habits) = runBlocking {
            reminderDao.getEnabledReminders() to habitDao.getAll().associateBy { it.id }
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        reminders.forEach { reminder ->
            val habit = habits[reminder.habitId] ?: return@forEach
            if (habit.isArchived) return@forEach
            cancelReminderAlarm(reminder.id)
            val triggerAt = nextTriggerMillis(reminder.hour, reminder.minute)
            val pendingIntent = buildReminderPendingIntent(
                reminderId = reminder.id,
                habitId = habit.id,
                habitName = habit.name,
                message = reminder.message,
            )
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun scheduleReminder(
        reminderId: Long,
        habitId: Long,
        habitName: String,
        message: String?,
    ) {
        val reminder = runBlocking {
            reminderDao.getRemindersForHabit(habitId).firstOrNull { it.id == reminderId }
        } ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelReminderAlarm(reminderId)
        val triggerAt = nextTriggerMillis(reminder.hour, reminder.minute)
        val pendingIntent = buildReminderPendingIntent(reminderId, habitId, habitName, message)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun scheduleSnooze(
        reminderId: Long,
        habitId: Long,
        habitName: String,
        message: String?,
        minutes: Long,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pendingIntent = buildReminderPendingIntent(reminderId, habitId, habitName, message, isSnooze = true)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelReminderAlarm(reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildCancelReminderIntent(reminderId)
        alarmManager.cancel(pendingIntent)
    }

    private fun buildReminderPendingIntent(
        reminderId: Long,
        habitId: Long,
        habitName: String,
        message: String?,
        isSnooze: Boolean = false,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SNOOZE, isSnooze)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags)
    }

    private fun buildCancelReminderIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags)
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    private fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }
}
