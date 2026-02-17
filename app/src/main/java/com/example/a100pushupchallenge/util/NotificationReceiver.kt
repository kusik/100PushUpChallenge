package com.example.a100pushupchallenge.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.a100pushupchallenge.MainActivity
import com.example.a100pushupchallenge.R
import com.example.a100pushupchallenge.R.drawable.ic_notifications_black_24dp
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val PREFS_NAME = "PushupAppPrefs"
        const val KEY_NOTIF_START_HOUR = "notif_start_hour"
        const val KEY_NOTIF_END_HOUR = "notif_end_hour"

        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "pushup_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Only show notification if the feature is enabled
        if (!sharedPreferences.getBoolean("notif_enabled", false)) {
            return
        }

        val startHour = sharedPreferences.getInt(KEY_NOTIF_START_HOUR, 9)
        val endHour = sharedPreferences.getInt(KEY_NOTIF_END_HOUR, 17)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only show notification if the current time is within the user-defined range
        if (currentHour in startHour until endHour) {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        // Create the notification channel (essential for Android 8.0+)
        createNotificationChannel(context)

        // Create an intent to launch the app when the notification is tapped
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ic_notifications_black_24dp) // IMPORTANT: Replace with a real drawable icon
            .setContentTitle("Time for Push-ups!")
            .setContentText("Don't forget to do your push-up session.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Set the tap action
            .setAutoCancel(true) // Dismiss the notification on tap

        // Show the notification
        // Use a try-catch block to handle potential SecurityExceptions if permission is missing
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // This can happen on Android 13+ if POST_NOTIFICATIONS permission is denied
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = "Push-up Reminders"
        val descriptionText = "Hourly reminders to do push-ups"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
