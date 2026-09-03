package com.htj.habitzy.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.htj.habitzy.R
import com.htj.habitzy.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationScheduler.EXTRA_REMINDER_ID, -1L)
        val habitId = intent.getLongExtra(NotificationScheduler.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(NotificationScheduler.EXTRA_HABIT_NAME) ?: "Habit"
        val message = intent.getStringExtra(NotificationScheduler.EXTRA_MESSAGE)

        val showActions = runCatching {
            runBlocking { settingsRepository.notificationActions.first() }
        }.getOrDefault(true)

        val builder = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_streak)
            .setContentTitle(habitName)
            .setContentText(message ?: "Time for $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (showActions) {
            val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_DONE
                putExtra(NotificationScheduler.EXTRA_HABIT_ID, habitId)
                putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
            }
            val donePendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                reminderId.toInt() * 10 + 1,
                doneIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )

            val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_SNOOZE
                putExtra(NotificationScheduler.EXTRA_HABIT_ID, habitId)
                putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
                putExtra(NotificationScheduler.EXTRA_HABIT_NAME, habitName)
                putExtra(NotificationScheduler.EXTRA_MESSAGE, message)
            }
            val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                reminderId.toInt() * 10 + 2,
                snoozeIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )

            builder
                .addAction(0, "Done", donePendingIntent)
                .addAction(0, "Snooze", snoozePendingIntent)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(reminderId.toInt(), builder.build())
    }
}
