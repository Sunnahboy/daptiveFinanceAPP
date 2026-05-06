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
        val prefs = context.getSharedPreferences("finadapt_prefs", Context.MODE_PRIVATE)
        val lastLoggedMidnight = prefs.getLong("LAST_LOGGED_MIDNIGHT", 0L)
        val currentStreak = prefs.getInt("CURRENT_STREAK", 0)
        val shieldCount = prefs.getInt("STREAK_SHIELDS", 0)

        val todayMidnight = getMidnightTimestamp()
        val hasLoggedToday = (todayMidnight == lastLoggedMidnight)

        // Trigger notification if they haven't logged today, regardless of streak!
        if (!hasLoggedToday) {
            val title: String
            val message: String

            if (currentStreak > 0) {
                // They have a streak to protect
                if (shieldCount > 0) {
                    // SHIELD IMMINENT - Randomize warning
                    val titles = listOf("Shield Activation Imminent! 🛡️", "Auto-Protect Engaging! 🛡️", "Shield Standby! 🛡️")
                    val messages = listOf(
                        "Your $currentStreak-day streak is cooling off. A Shield will be consumed at midnight to protect it!",
                        "Don't waste your shield! Log an expense to protect your $currentStreak-day streak natively.",
                        "System alert: 1 Shield will be burned tonight to save your $currentStreak-day streak."
                    )
                    title = titles.random()
                    message = messages.random()
                } else {
                    // STREAK IN DANGER - Randomize warning
                    val titles = listOf("Streak in Danger! 🔥", "Don't break the chain! 🔗", "Midnight is approaching! ⏰")
                    val messages = listOf(
                        "Don't lose your $currentStreak-day streak! Log an expense before midnight.",
                        "Your $currentStreak-day streak is hanging by a thread. Take 10 seconds to log your daily spend!",
                        "You've worked hard for this $currentStreak-day streak. Don't let it reset to zero today!"
                    )
                    title = titles.random()
                    message = messages.random()
                }
            } else {
                // ZERO STREAK (MOTIVATION) - Randomize encouragement
                val titles = listOf("Time to check in! 📝", "Blank slate! 🚀", "Ready to track? 📊")
                val messages = listOf(
                    "You haven't logged any expenses today. Start your saving streak now!",
                    "Every master was once a beginner. Start your Day 1 streak today!",
                    "Take control of your cash. Log your first expense of the day."
                )
                title = titles.random()
                message = messages.random()
            }

            showNotification(title, message)
        }
        return Result.success()
    }


    private fun showNotification(title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "streak_reminder"

        //Android 8.0+ requires a Notification channel
        val channel = NotificationChannel(
            channelId,
            "Gamification Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to keep your financial streak alive"
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
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // will add app icon later
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(1001, notification)
    }


    private fun getMidnightTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
