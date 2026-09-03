package com.htj.habitzy.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.htj.habitzy.domain.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    companion object {
        const val ACTION_DONE = "com.htj.habitzy.action.DONE"
        const val ACTION_SNOOZE = "com.htj.habitzy.action.SNOOZE"

        private const val SNOOZE_MINUTES = 15L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationScheduler.EXTRA_REMINDER_ID, -1L)
        val habitId = intent.getLongExtra(NotificationScheduler.EXTRA_HABIT_ID, -1L)

        when (intent.action) {
            ACTION_DONE -> {
                if (habitId >= 0) {
                    runBlocking {
                        habitRepository.toggleCompletion(habitId, LocalDate.now())
                    }
                }
            }
            ACTION_SNOOZE -> {
                val habitName = intent.getStringExtra(NotificationScheduler.EXTRA_HABIT_NAME) ?: "Habit"
                val message = intent.getStringExtra(NotificationScheduler.EXTRA_MESSAGE)
                notificationScheduler.scheduleSnooze(
                    reminderId = reminderId,
                    habitId = habitId,
                    habitName = habitName,
                    message = message,
                    minutes = SNOOZE_MINUTES,
                )
            }
        }

        if (reminderId >= 0) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(reminderId.toInt())
        }
    }
}
