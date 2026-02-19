package com.example.a100pushupchallenge.ui.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.a100pushupchallenge.R
import com.example.a100pushupchallenge.util.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var switchNotifications: SwitchCompat // <<<<<<<<<< 2. ENSURE THIS VARIABLE TYPE IS CORRECT
    private lateinit var buttonStartTime: Button
    private lateinit var buttonEndTime: Button

    private lateinit var sharedPreferences: SharedPreferences
    private var startHour = 9
    private var startMinute = 0
    private var endHour = 17
    private var endMinute = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedPreferences = requireActivity().getSharedPreferences(NotificationReceiver.PREFS_NAME, Context.MODE_PRIVATE)

        switchNotifications = view.findViewById(R.id.settings_switch_notifications)
        buttonStartTime = view.findViewById(R.id.settings_button_start_time)
        buttonEndTime = view.findViewById(R.id.settings_button_end_time)

        loadSettings()
        setupListeners()

        return view
    }

    // The rest of your file is correct and does not need any changes.
    // ... (loadSettings, setupListeners, etc.)

    private fun loadSettings() {
        switchNotifications.isChecked = sharedPreferences.getBoolean("notif_enabled", false)
        startHour = sharedPreferences.getInt(NotificationReceiver.KEY_NOTIF_START_HOUR, 9)
        startMinute = sharedPreferences.getInt("notif_start_minute", 0)
        endHour = sharedPreferences.getInt(NotificationReceiver.KEY_NOTIF_END_HOUR, 17)
        endMinute = sharedPreferences.getInt("notif_end_minute", 0)
        updateButtonText()
    }

    private fun setupListeners() {
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notif_enabled", isChecked).apply()
            if (isChecked) {
                scheduleHourlyNotifications()
                Toast.makeText(context, "Hourly reminders enabled.", Toast.LENGTH_SHORT).show()

            } else {
                cancelNotifications()
                Toast.makeText(context, "Reminders disabled.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        buttonEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val initialHour = if (isStartTime) startHour else endHour
        val initialMinute = if (isStartTime) startMinute else endMinute

        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            if (isStartTime) {
                startHour = hourOfDay
                startMinute = minute
            } else {
                endHour = hourOfDay
                endMinute = minute
            }
            saveTimeSettings()
            updateButtonText()
            if (switchNotifications.isChecked) {
                scheduleHourlyNotifications()
            }
        }, initialHour, initialMinute, false).show()
    }

    private fun saveTimeSettings() {
        sharedPreferences.edit().apply {
            putInt(NotificationReceiver.KEY_NOTIF_START_HOUR, startHour)
            putInt("notif_start_minute", startMinute)
            putInt(NotificationReceiver.KEY_NOTIF_END_HOUR, endHour)
            putInt("notif_end_minute", endMinute)
            apply()
        }
    }

    private fun updateButtonText() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val calStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMinute) }
        val calEnd = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMinute) }
        buttonStartTime.text = timeFormat.format(calStart.time)
        buttonEndTime.text = timeFormat.format(calEnd.time)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            requireContext(),
            NotificationReceiver.NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleHourlyNotifications() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntent()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_HOUR,
            pendingIntent
        )
    }

    private fun cancelNotifications() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntent()
        alarmManager.cancel(pendingIntent)
    }
}
