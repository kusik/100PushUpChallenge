package com.example.a100pushupchallenge.ui.workout

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.a100pushupchallenge.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.navigation.fragment.findNavController

class WorkoutFragment : Fragment(), SensorEventListener, OnInitListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    // ... (existing variables) ...
    private var gravitySensor: Sensor? = null // For detecting if phone is flat
    private lateinit var textViewPhoneState: TextView
    private var isPhoneLyingFlat = false
    private val gravityValues = FloatArray(3) // To store gravity sensor data
    private lateinit var textViewPushUpCount: TextView
    private lateinit var textViewMaxPushupValue: TextView
    private lateinit var textViewTimerDisplay: TextView
    private lateinit var textViewTimeAchievedAtMax: TextView
    private lateinit var editTextTimerMinutes: EditText
    private lateinit var buttonSetTimer: Button
    private lateinit var buttonReset: Button
    private lateinit var imageViewAnimatedGif: ImageView

    private var isNear = false
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false

    private var currentPushupCount = 0
    private var maxPushupCount = 0
    private var timeRemainingAtMax: Long = 0 // Stores time left on clock when max was achieved
    private var isSessionActive = false

    private var countDownTimer: CountDownTimer? = null
    private var userSetCountdownTimeMs: Long = 60000L // Default 1 minute, will be loaded
    private var timeLeftInMillis: Long = 60000L      // Current time left in an active session

    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "PushUpPrefs"
    private val KEY_MAX_PUSHUPS = "maxPushups"
    private val KEY_TIME_REMAINING_AT_MAX = "timeRemainingAtMax"
    private val KEY_USER_COUNTDOWN_MINUTES = "userCountdownMinutes"

    // Optional: To store the countdown duration that was active when a max was achieved.
    // This helps in fairly comparing "best times" if the user changes the countdown duration.
    private val KEY_COUNTDOWN_FOR_MAX_TIME = "countdownForMaxTime"
    private val KEY_ALL_WORKOUT_RECORDS = "allWorkoutRecords"

    // We'll still keep the overall best for quick display on the workout screen
    private var overallMaxPushupCount = 0
    private var overallTimeRemainingAtMax: Long = 0
    private var overallCountdownForMax: Long = 0
    // Thresholds for flat detection (tweak these)
    private val Z_AXIS_GRAVITY_THRESHOLD =8.5f // m/s^2 (gravity is ~9.8)
    private val XY_AXIS_GRAVITY_THRESHOLD = 2.5f // m/s^2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize Views
        textViewPhoneState = view.findViewById(R.id.textViewPhoneState)
        textViewPushUpCount = view.findViewById(R.id.textViewPushupCount)
        textViewMaxPushupValue = view.findViewById(R.id.textViewMaxPushupValue)
        textViewTimerDisplay = view.findViewById(R.id.textViewTimerDisplay)
        textViewTimeAchievedAtMax = view.findViewById(R.id.textViewTimeAchievedAtMax)
        editTextTimerMinutes = view.findViewById(R.id.editTextTimerMinutes)
        buttonSetTimer = view.findViewById(R.id.buttonSetTimer)
        buttonReset = view.findViewById(R.id.buttonReset)
        imageViewAnimatedGif = view.findViewById(R.id.imageView)

        // Initialize Sensor
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            Toast.makeText(requireContext(), "Proximity sensor not available!", Toast.LENGTH_SHORT)
                .show()
        }

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadUserPreferencesAndOverallBest() // Modified loading function

        // Initialize TTS
        if (!::tts.isInitialized) {
            tts = TextToSpeech(requireContext(), this)
        }

        // Set Listeners
        buttonReset.setOnClickListener { resetWorkoutState() }

        buttonSetTimer.setOnClickListener { handleSetTimer() }
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (gravitySensor == null) {
            // Fallback to accelerometer if gravity sensor is not available
            Log.w("WorkoutFragment", "Gravity sensor not available, falling back to Accelerometer for orientation.")
            // You might want to use accelerometer directly in onSensorChanged if gravitySensor is null
            // For now, we'll assume TYPE_GRAVITY is often available or the app can tolerate not having this specific check.
            textViewPhoneState.text = "Phone state: Gravity sensor N/A"
        }
        // Initial UI Update
        updatePushUpCountDisplay()
        updateMaxPushupDisplay()
        updateTimeAtMaxDisplay()
        updateTimerTextDisplay() // Show initial countdown value
        updateOverallMaxDisplay()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this)
            .asGif()
            .load(R.drawable.animated_pushup) // Ensure you have this drawable
            .into(imageViewAnimatedGif)
    }

    private fun loadUserPreferencesAndOverallBest() {
        // Load user's preferred countdown duration
        val defaultCountdownMinutes = 1L
        val savedMinutes =
            sharedPreferences.getLong(KEY_USER_COUNTDOWN_MINUTES, defaultCountdownMinutes)
        userSetCountdownTimeMs = TimeUnit.MINUTES.toMillis(savedMinutes)
        editTextTimerMinutes.setText(savedMinutes.toString())
        timeLeftInMillis = userSetCountdownTimeMs // Initialize for display or new session

        // Load all records and determine overall best
        val allRecords =
            sharedPreferences.getStringSet(KEY_ALL_WORKOUT_RECORDS, HashSet()) ?: HashSet()
        if (allRecords.isNotEmpty()) {
            var tempOverallMax = 0
            var tempOverallTimeRemaining = 0L
            var tempOverallCountdown = 0L

            for (recordString in allRecords) {
                try {
                    val parts = recordString.split("_")
                    if (parts.size == 3) {
                        // val duration = parts[0].toLong() // We don't need duration to find *overall* max here
                        val pushups = parts[1].toInt()
                        val timeRemaining = parts[2].toLong()
                        // val countdownUsed = parts[0].toLong() // This is the countdown for THIS record

                        // Logic for overall best: highest pushups, then most time remaining for that count
                        if (pushups > tempOverallMax) {
                            tempOverallMax = pushups
                            tempOverallTimeRemaining = timeRemaining
                            tempOverallCountdown =
                                parts[0].toLong() // Store the countdown of this overall best
                        } else if (pushups == tempOverallMax) {
                            if (timeRemaining > tempOverallTimeRemaining) { // More time left is better
                                tempOverallTimeRemaining = timeRemaining
                                tempOverallCountdown = parts[0].toLong()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutFragment", "Error parsing record: $recordString", e)
                }
            }
            overallMaxPushupCount = tempOverallMax
            overallTimeRemainingAtMax = tempOverallTimeRemaining
            overallCountdownForMax = tempOverallCountdown
        } else {
            overallMaxPushupCount = 0
            overallTimeRemainingAtMax = 0
            overallCountdownForMax = 0
        }
        updateOverallMaxDisplay() // Update UI for overall max
        updateTimerTextDisplay() // Update timer display based on user preference
    }

    private fun updateOverallMaxDisplay() {
        textViewMaxPushupValue.text = overallMaxPushupCount.toString()
        if (overallMaxPushupCount > 0 && overallCountdownForMax > 0) {
            val timeAchievedFormatted = formatTime(overallTimeRemainingAtMax)
            textViewTimeAchievedAtMax.text =
                "Overall Best: $overallMaxPushupCount with $timeAchievedFormatted left (Timer: ${
                    formatTime(overallCountdownForMax)
                })"
        } else {
            textViewTimeAchievedAtMax.text = "Overall Best: N/A"
        }
    }

    private fun saveUserCountdownPreference(minutes: Long) {
        with(sharedPreferences.edit()) {
            putLong(KEY_USER_COUNTDOWN_MINUTES, minutes)
            apply()
        }
    }

    private fun handleSetTimer() {
        if (isSessionActive) {
            Toast.makeText(
                requireContext(),
                "Cannot change timer during an active session.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            val minutesStr = editTextTimerMinutes.text.toString()
            if (minutesStr.isNotEmpty()) {
                val minutes = minutesStr.toLong()
                if (minutes > 0 && minutes <= 99) { // Max 99 minutes
                    userSetCountdownTimeMs = TimeUnit.MINUTES.toMillis(minutes)
                    saveUserCountdownPreference(minutes)
                    timeLeftInMillis = userSetCountdownTimeMs // Update current timeLeft
                    updateTimerTextDisplay()
                    Toast.makeText(
                        requireContext(),
                        "Countdown set to $minutes minute(s).",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter minutes between 1 and 99.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(
                requireContext(),
                "Invalid number format for minutes.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- CountdownTimer Logic ---
    private fun startNewCountdownSession() {
        if (isSessionActive) { // Should not happen if logic is correct, but as a safeguard
            countDownTimer?.cancel()
        }
        currentPushupCount = 0
        updatePushUpCountDisplay()
        timeLeftInMillis = userSetCountdownTimeMs // Start with the user-set time
        isSessionActive = true
        // Disable timer setting during session
        editTextTimerMinutes.isEnabled = false
        buttonSetTimer.isEnabled = false
        buttonReset.text = "Stop Session"

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) { // Update UI every second
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerTextDisplay()
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                updateTimerTextDisplay()
                endSession("Time's up!")
            }
        }.start()
        Toast.makeText(
            requireContext(),
            "Workout Started! You have ${formatTime(userSetCountdownTimeMs)}.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // endSession needs to be significantly changed to save record for current duration
    private fun endSession(reason: String) {
        if (!isSessionActive) return

        isSessionActive = false
        countDownTimer?.cancel()
        countDownTimer = null

        editTextTimerMinutes.isEnabled = true
        buttonSetTimer.isEnabled = true
        buttonReset.text = "Reset / Start"

        val achievedPushupsThisSession = currentPushupCount
        val countdownDurationForThisSession =
            userSetCountdownTimeMs // Crucial: the timer used for THIS session
        val timeRemainingThisSession = timeLeftInMillis

        if (achievedPushupsThisSession > 0) {
            val allRecords =
                sharedPreferences.getStringSet(KEY_ALL_WORKOUT_RECORDS, HashSet())?.toMutableSet()
                    ?: HashSet() // Important: make it mutable

            var currentRecordForThisDuration: String? = null
            var maxForThisDuration = 0
            var timeRemainingForThisDurationMax = 0L

            // Find if there's an existing record for THIS specific countdownDuration
            val iterator = allRecords.iterator()
            while (iterator.hasNext()) {
                val recordString = iterator.next()
                try {
                    val parts = recordString.split("_")
                    if (parts.size == 3 && parts[0].toLong() == countdownDurationForThisSession) {
                        currentRecordForThisDuration =
                            recordString // Found existing record for this timer setting
                        maxForThisDuration = parts[1].toInt()
                        timeRemainingForThisDurationMax = parts[2].toLong()
                        iterator.remove() // Remove old record, we'll add the updated one
                        break
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutFragment", "Error parsing record string: $recordString", e)
                }
            }

            var isNewOrBetterForThisDuration = false
            if (achievedPushupsThisSession > maxForThisDuration) {
                isNewOrBetterForThisDuration = true
            } else if (achievedPushupsThisSession == maxForThisDuration) {
                if (timeRemainingThisSession > timeRemainingForThisDurationMax) { // More time left is better
                    isNewOrBetterForThisDuration = true
                }
            }

            if (isNewOrBetterForThisDuration) {
                val newRecordString =
                    "${countdownDurationForThisSession}_${achievedPushupsThisSession}_${timeRemainingThisSession}"
                allRecords.add(newRecordString) // Add new or updated record for this duration

                with(sharedPreferences.edit()) {
                    putStringSet(KEY_ALL_WORKOUT_RECORDS, allRecords)
                    apply()
                }
                Toast.makeText(
                    requireContext(),
                    "New record for ${formatTime(countdownDurationForThisSession)} timer: $achievedPushupsThisSession pushups!",
                    Toast.LENGTH_LONG
                ).show()

                // After saving, reload overall best to see if this new record is the new overall best
                loadUserPreferencesAndOverallBest()

            } else {
                if (currentRecordForThisDuration != null) allRecords.add(
                    currentRecordForThisDuration
                ) // Add back old if not better
                Toast.makeText(
                    requireContext(),
                    "$reason Final Count for ${formatTime(countdownDurationForThisSession)} timer: $achievedPushupsThisSession. Not a new record for this timer setting.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {
            Toast.makeText(requireContext(), "$reason No push-ups recorded.", Toast.LENGTH_SHORT)
                .show()
        }

        currentPushupCount = 0
        updatePushUpCountDisplay()
        timeLeftInMillis = userSetCountdownTimeMs
        updateTimerTextDisplay()
    }


    private fun updateTimerTextDisplay() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) % 60
        textViewTimerDisplay.text =
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
    // --- End CountdownTimer Logic ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "TTS Language not supported!")
                isTtsInitialized = false
            } else {
                isTtsInitialized = true
                Log.i("TTS", "TTS Initialized successfully.")
            }
        } else {
            Log.e("TTS", "TTS Initialization Failed! Status: $status")
            isTtsInitialized = false
        }
    }

    private fun speakPushUpCount() {
        if (isTtsInitialized && ::tts.isInitialized && !tts.isSpeaking) {
            tts.speak(currentPushupCount.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }


    private fun resetWorkoutState() {
        if (isSessionActive) {
            endSession("Session stopped by user.")
        } else { // If no session active, just reset the counter and timer display
            currentPushupCount = 0
            isNear = false
            updatePushUpCountDisplay()
            timeLeftInMillis = userSetCountdownTimeMs
            updateTimerTextDisplay()
            editTextTimerMinutes.isEnabled = true // Ensure timer setting is enabled
            buttonSetTimer.isEnabled = true
            Toast.makeText(requireContext(), "Counters reset.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        // Register gravity sensor
        gravitySensor?.also { grav ->
            sensorManager.registerListener(this, grav, SensorManager.SENSOR_DELAY_UI)
        }
        if (!::tts.isInitialized || !isTtsInitialized) {
            // Re-initialize TTS if needed
            if (::tts.isInitialized) {
                tts.shutdown()
            } // Shutdown existing if not initialized properly
            tts = TextToSpeech(requireContext(), this)
        }
        // Do not automatically resume countdown. User should explicitly start.
        // Ensure UI reflects current state
        editTextTimerMinutes.isEnabled = !isSessionActive
        buttonSetTimer.isEnabled = !isSessionActive
        buttonReset.text = if (isSessionActive) "Stop Session" else "Reset / Save Max"
        updateTimerTextDisplay()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        // If a session is active, canceling the timer is one option.
        // User will lose progress in that specific timed session if they leave.
        // Or, you could try to persist the exact state of countDownTimer and resume it,
        // but that's more complex. For now, cancel it.
        if (isSessionActive) {
            countDownTimer?.cancel()
            // isSessionActive remains true, so onResume won't auto-start a new one.
            // User would effectively have to "Stop Session" via button or let timer run out if they return.
            // Or we can auto-end it:
            // endSession("Session ended due to app pause");
            Toast.makeText(
                requireContext(),
                "Timer paused. Resume not supported, stop session to save.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        if (::tts.isInitialized && tts.isSpeaking) {
            tts.stop()
        }
        super.onDestroyView() // call super after your cleanup
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                // ... (your existing proximity sensor logic) ...
                // You might want to only allow push-up counting if isPhoneLyingFlat is true
                val distance = event.values[0]
                val threshold = proximitySensor?.maximumRange?.let { if (it > 1) 1.5f else it * 0.8f } ?: 1.5f

                if (distance < threshold) {
                    if (!isNear) {
                        isNear = true
                        if (isPhoneLyingFlat) { // <<< CHECK IF PHONE IS FLAT
                            if (!isSessionActive && currentPushupCount == 0) {
                                startNewCountdownSession()
                            }
                            if (isSessionActive) {
                                currentPushupCount++
                                updatePushUpCountDisplay()
                                speakPushUpCount()
                            }
                        } else {
                            if (isSessionActive) { // Optional: provide feedback if they try to count but phone isn't flat
                                Toast.makeText(context, "Place phone flat to count push-ups", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    if (isNear) {
                        isNear = false
                    }
                }
            }
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, gravityValues, 0, gravityValues.size)
                val x = gravityValues[0]
                val y = gravityValues[1]
                val z = gravityValues[2]

                // Check if phone is lying flat (either screen up or screen down)
                // This means the gravity vector is mostly along the Z-axis
                if (Math.abs(z) > Z_AXIS_GRAVITY_THRESHOLD &&
                    Math.abs(x) < XY_AXIS_GRAVITY_THRESHOLD &&
                    Math.abs(y) < XY_AXIS_GRAVITY_THRESHOLD
                ) {
                    isPhoneLyingFlat = true
                    if (z > 0) { // Gravity positive on Z: phone is screen down (or Z-axis definition)
                        textViewPhoneState.text = "Phone state: Flat (Screen Down)"
                    } else { // Gravity negative on Z: phone is screen up
                        textViewPhoneState.text = "Phone state: Flat (Screen Up)"
                    }
                } else {
                    isPhoneLyingFlat = false
                    // Determine if it's portrait or landscape based on which axis has more gravity component
                    if (Math.abs(y) > Math.abs(x) && Math.abs(y) > Z_AXIS_GRAVITY_THRESHOLD / 2) { // rough check
                        textViewPhoneState.text = "Phone state: Upright (Portrait)"
                    } else if (Math.abs(x) > Math.abs(y) && Math.abs(x) > Z_AXIS_GRAVITY_THRESHOLD / 2) {
                        textViewPhoneState.text = "Phone state: Sideways (Landscape)"
                    } else {
                        textViewPhoneState.text = "Phone state: Tilted/Moving"
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not critical for proximity
    }

    private fun updatePushUpCountDisplay() {
        textViewPushUpCount.text = currentPushupCount.toString()
    }

    private fun updateMaxPushupDisplay() {
        textViewMaxPushupValue.text = maxPushupCount.toString()
    }

    private fun updateTimeAtMaxDisplay() {
        if (maxPushupCount > 0) {
            val countdownDurationWhenMaxWasAchieved =
                sharedPreferences.getLong(KEY_COUNTDOWN_FOR_MAX_TIME, 0L)
            if (countdownDurationWhenMaxWasAchieved > 0L) { // Check if a valid countdown was stored
                if (timeRemainingAtMax == 0L) {
                    textViewTimeAchievedAtMax.text = "Best: Max at 0s left (Timer: ${
                        formatTime(countdownDurationWhenMaxWasAchieved)
                    })"
                } else {
                    val timeAchievedFormatted = formatTime(timeRemainingAtMax)
                    textViewTimeAchievedAtMax.text =
                        "Best: Max with $timeAchievedFormatted left (Timer: ${
                            formatTime(countdownDurationWhenMaxWasAchieved)
                        })"
                }
            } else { // No specific countdown stored for the current max, or it's an old record
                textViewTimeAchievedAtMax.text = "Best Time: N/A (or old record)"
            }
        } else {
            textViewTimeAchievedAtMax.text = "Best Time: N/A"
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

}
