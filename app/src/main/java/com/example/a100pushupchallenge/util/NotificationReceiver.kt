package com.example.a100pushupchallenge.util // Create a 'util' package if you don't have one

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.a100pushupchallenge.MainActivity
import com.example.a100pushupchallenge.R
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "pushup_reminder_channel"
        const val NOTIFICATION_ID = 101
        const val PREFS_NAME = "PushUpPrefs" // Same as your other fragments
        const val KEY_NOTIF_START_HOUR = "notif_start_hour"
        const val KEY_NOTIF_END_HOUR = "notif_end_hour"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val startHour = prefs.getInt(KEY_NOTIF_START_HOUR, 9) // Default 9 AM
        val endHour = prefs.getInt(KEY_NOTIF_END_HOUR, 17)   // Default 5 PM
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only show notification if the current hour is within the user's defined range
        if (currentHour in startHour until endHour) {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Push-up Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Hourly reminders to do a set of push-ups"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent to launch app when notification is tapped
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a fitness icon
            .setContentTitle("Time for Push-ups!")
            .setContentText("Let's get a set in to reach your goal.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
