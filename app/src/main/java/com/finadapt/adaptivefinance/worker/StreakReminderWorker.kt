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

        // 🟢 FIX: Trigger notification if they haven't logged today, regardless of streak!
        if (!hasLoggedToday) {
            val title: String
            val message: String

            if (currentStreak > 0) {
                // They have a streak to protect
                if (shieldCount > 0) {
                    title = "Shield Activation Imminent! 🛡️"
                    message = "Your $currentStreak-day streak is cooling off. A Shield will be consumed at midnight to protect it!"
                } else {
                    title = "Streak in Danger! 🔥"
                    message = "Don't lose your $currentStreak-day streak! Log an expense before midnight."
                }
            } else {
                // 🟢 NEW: They have a 0 streak, encourage them!
                title = "Time to check in! 📝"
                message = "You haven't logged any expenses today. Start your saving streak now!"
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
