package com.example.a100pushupchallenge.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // We only want to act when the boot is completed.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device has booted, checking notification settings.")

            val sharedPreferences = context.getSharedPreferences(NotificationReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            val notificationsEnabled = sharedPreferences.getBoolean("notif_enabled", false)

            if (notificationsEnabled) {
                Log.d("BootReceiver", "Notifications were enabled, re-scheduling them now.")
                scheduleHourlyNotifications(context)
            }
        }
    }

    private fun scheduleHourlyNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val notificationIntent = Intent(context, NotificationReceiver::class.java)
        // This is the crucial flag needed for Android 12+
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationReceiver.NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
            AlarmManager.INTERVAL_HOUR,
            pendingIntent
        )
    }
}
