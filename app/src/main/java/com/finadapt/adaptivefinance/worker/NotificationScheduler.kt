package com.finadapt.adaptivefinance.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    // 🟢 NEW: Accepts a list of Pair<Hour, Minute>
    fun scheduleDailyReminders(context: Context, times: List<Pair<Int, Int>>) {
        val workManager = WorkManager.getInstance(context)

        // 1. First, cancel ALL existing reminders so we don't get duplicates
        workManager.cancelAllWorkByTag("DailyReminderTag")

        // 2. Loop through the user's chosen times and schedule a unique worker for each
        times.forEachIndexed { index, time ->
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, time.first)
                set(Calendar.MINUTE, time.second)
                set(Calendar.SECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val reminderRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("DailyReminderTag") // Groups them together so we can cancel them easily later
                .build()

            // Enqueue uniquely as Reminder_0, Reminder_1, etc.
            workManager.enqueueUniquePeriodicWork(
                "DailyExpenseReminder_$index",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
            )
        }
    }

    // Helper to save the string to SharedPreferences (e.g. "09:00,20:30")
    fun saveTimesToPrefs(context: Context, times: List<Pair<Int, Int>>) {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        val timeString = times.joinToString(",") { "${it.first}:${it.second}" }
        prefs.edit().putString("REMINDER_TIMES", timeString).apply()
    }

    // Helper to read the string back into a List of Pairs
    fun getTimesFromPrefs(context: Context): List<Pair<Int, Int>> {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        val timeString = prefs.getString("REMINDER_TIMES", "20:00") ?: "20:00"

        if (timeString.isBlank()) return emptyList()

        return timeString.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) {
                Pair(parts[0].toInt(), parts[1].toInt())
            } else null
        }
    }
}