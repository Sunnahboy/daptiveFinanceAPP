package com.finadapt.adaptivefinance.worker
import android.app.NotificationChannel
import android.app.NotificationManager
import  android.app.PendingIntent
import  android.content.Context
import  android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finadapt.adaptivefinance.MainActivity
import java.util.Calendar

class StreakReminderWorker (
    private val context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context,workerParams){

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        val lastLoggedMidnight = prefs.getLong("LAST_LOGGED_MIDNIGHT", 0L)
        val currentStreak = prefs.getInt("CURRENT_STREAK", 0)
        val shieldCount = prefs.getInt("STREAK_SHIELDS", 0)

        val todayMidnight = getMidnightTimestamp()
        val hasLoggedToday = (todayMidnight == lastLoggedMidnight)

        //Check what day of the week it is for League Events
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // --- 1. LEAGUE NOTIFICATIONS (Runs independently of the streak) ---
        if (currentDayOfWeek == Calendar.SUNDAY) {
            // Sunday Evening: Urgency to secure their rank
            showNotification(
                title = "League Ends Tonight! 🏆",
                message = "The weekly leaderboard resets at midnight. Log any final expenses to secure your tier!",
                notificationId = 1002 // Separate ID so it doesn't overwrite the streak notification
            )
        } else if (currentDayOfWeek == Calendar.MONDAY && !hasLoggedToday) {
            // Monday: Fresh start motivation (Only if they haven't already logged today)
            showNotification(
                title = "New League Week! 🌍",
                message = "The global leaderboard just reset. Log your first expense of the week to jump ahead!",
                notificationId = 1002
            )
        }

        // --- 2. STREAK NOTIFICATIONS
        if (!hasLoggedToday) {
            val title: String
            val message: String

            if (currentStreak > 0) {
                if (shieldCount > 0) {
                    val titles = listOf("Shield Activation Imminent! \uD83D\uDEE1\uFE0F", "Auto-Protect Engaging! \uD83D\uDEE1\uFE0F", "Shield Standby! \uD83D\uDEE1\uFE0F")
                    val messages = listOf(
                        "Your $currentStreak-day streak is cooling off. A Shield will be consumed at midnight to protect it!",
                        "Don't waste your shield! Log an expense to protect your $currentStreak-day streak natively.",
                        "System alert: 1 Shield will be burned tonight to save your $currentStreak-day streak."
                    )
                    title = titles.random()
                    message = messages.random()
                } else {
                    val titles = listOf("Streak in Danger! \uD83D\uDD25", "Don't break the chain! \uD83D\uDD17", "Midnight is approaching! \u23F0")
                    val messages = listOf(
                        "Don't lose your $currentStreak-day streak! Log an expense before midnight.",
                        "Your $currentStreak-day streak is hanging by a thread. Take 10 seconds to log your daily spend!",
                        "You've worked hard for this $currentStreak-day streak. Don't let it reset to zero today!"
                    )
                    title = titles.random()
                    message = messages.random()
                }
            } else {
                val titles = listOf("Time to check in! \uD83D\uDCDD", "Blank slate! \uD83D\uDE80", "Ready to track? \uD83D\uDCCA")
                val messages = listOf(
                    "You haven't logged any expenses today. Start your saving streak now!",
                    "Every master was once a beginner. Start your Day 1 streak today!",
                    "Take control of your cash. Log your first expense of the day."
                )
                title = titles.random()
                message = messages.random()
            }

            // Call with the default streak ID
            showNotification(title, message, 1001)
        }
        return Result.success()
    }


    private fun showNotification(title: String, message: String, notificationId: Int = 1001) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gamification_alerts" // Renamed to be more generic

        //Android 8.0+ requires a Notification channel
        val channel = NotificationChannel(
            channelId,
            "Gamification Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to keep your financial streak alive and league updates"
        }
        notificationManager.createNotificationChannel(channel)

        // Make the notification clickable
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) //will add app icon later
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        //specific ID here so notifications don't overwrite each other
        notificationManager.notify(notificationId, notification)
    }


    private fun getMidnightTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
