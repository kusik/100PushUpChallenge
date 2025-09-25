package com.example.a100pushupchallenge.ui.workout

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.tts.TextToSpeech // Import TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log // For logging TTS errors
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.fragment.app.Fragment
import com.example.a100pushupchallenge.R
import java.util.Locale // For TTS Language

class WorkoutFragment : Fragment(), SensorEventListener, OnInitListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private lateinit var textViewPushUpCount: TextView
    private lateinit var textViewMaxPushupValue: TextView // TextView for max push-ups
    private lateinit var buttonReset: Button
    private lateinit var imageViewAnimatedGif: ImageView
    private var isNear = false // To track if the sensor is currently detecting proximity

    // TTS Variables
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false

    private var currentPushupCount = 0 // Renamed for clarity
    private var maxPushupCount = 0     // To store the highest score

    // SharedPreferences setup
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "PushUpPrefs"
    private val KEY_MAX_PUSHUPS = "maxPushups"
    // Removed: Ringtone related variables and SharedPreferences key

    // Removed: ringtonePickerLauncher

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure this layout name is correct for your fragment.
        // It should NOT contain the "Select Sound" button if you're removing that feature.
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        textViewPushUpCount = view.findViewById(R.id.textViewPushupCount)
        textViewMaxPushupValue = view.findViewById(R.id.textViewMaxPushupValue)
        buttonReset = view.findViewById(R.id.buttonReset)
        // Removed: Initialization of buttonSelectSound
        imageViewAnimatedGif = view.findViewById(R.id.imageView) // Initialize your ImageView
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            Toast.makeText(requireContext(), "Proximity sensor not available!", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadMaxPushupCount() // Load saved max count

        buttonReset.setOnClickListener {
            saveCurrentSessionAsMaxIfNeeded() // Check and save before resetting
            resetCurrentPushUpCount()
        }

        if (!::tts.isInitialized) {
            tts = TextToSpeech(requireContext(), this)
        }
        updatePushUpCountDisplay()
        updateMaxPushupDisplay()

        // Initialize TextToSpeech
        // Check if tts has been initialized already to avoid re-creating on configuration change if not necessary
        if (!::tts.isInitialized) {
            tts = TextToSpeech(requireContext(), this)
        }


        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the GIF using Glide        // Make sure 'animated_pushup' is the name of your GIF file in res/drawable
        // (without the .gif extension when referring to it via R.drawable)
        Glide.with(this) // Use 'this' for Fragment, or 'requireContext()'
            .asGif() // Explicitly state that you are loading a GIF
            .load(R.drawable.animated_pushup) // Your GIF resource
            // Optional: Control caching
            // .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Default, good for most cases
            // Optional: Placeholder while loading
            // .placeholder(R.drawable.loading_placeholder)
            // Optional: Error image if loading fails
            // .error(R.drawable.error_image)
            .into(imageViewAnimatedGif)

        // If your GIF is in res/raw:
        // Glide.with(this)
        //     .asGif()
        //     .load(R.raw.animated_pushup_raw) // R.raw.your_gif_name
        //     .into(imageViewAnimatedGif)
    }

    // --- TextToSpeech.OnInitListener Implementation ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language (e.g., US English). Change Locale.US to Locale.getDefault()
            // to use the device's current language if supported by TTS.
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
                Toast.makeText(requireContext(), "TTS language not supported.", Toast.LENGTH_SHORT).show()
                isTtsInitialized = false
            } else {
                isTtsInitialized = true
                Log.i("TTS", "TTS Initialized successfully.")
                // You could optionally speak an initial message here if needed, e.g., "Ready to count"
            }
        } else {
            Log.e("TTS", "TTS Initialization Failed! Status: $status")
            Toast.makeText(requireContext(), "TTS initialization failed.", Toast.LENGTH_SHORT).show()
            isTtsInitialized = false
        }
    }
    // --- End TextToSpeech.OnInitListener ---

    private fun speakPushUpCount() {
        if (isTtsInitialized && ::tts.isInitialized) { // Check if tts is initialized before using
            val textToSpeak = currentPushupCount.toString()
            // QUEUE_FLUSH ensure it interrupts previous speech and speaks immediately.
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTS", "TTS not initialized or not ready, cannot speak count.")
            // Optionally, provide a silent fallback or a different non-speech cue
        }
    }
    private fun loadMaxPushupCount() {
        maxPushupCount = sharedPreferences.getInt(KEY_MAX_PUSHUPS, 0) // Default to 0 if not found
    }

    private fun saveMaxPushupCount(newMax: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_MAX_PUSHUPS, newMax)
            apply() // Use apply() for asynchronous save
        }
        maxPushupCount = newMax // Update in-memory variable
        updateMaxPushupDisplay()
    }

    private fun saveCurrentSessionAsMaxIfNeeded() {
        if (currentPushupCount > maxPushupCount) {
            saveMaxPushupCount(currentPushupCount)
            Toast.makeText(requireContext(), "New Max Record: $currentPushupCount!", Toast.LENGTH_SHORT).show()
        }
    }

    // Removed: loadSavedRingtonePreference, saveRingtonePreference, getDefaultNotificationUri,
    // loadAndPrepareRingtone, openRingtonePicker, playCurrentSound (related to ringtones)

    override fun onResume() {
        super.onResume()
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // If TTS wasn't initialized or failed, try re-initializing.
        // This handles cases where the TTS engine might become available later
        // or if the fragment is resumed after being paused for a long time.
        if (!::tts.isInitialized || !isTtsInitialized) {
            if (::tts.isInitialized && !isTtsInitialized) { // If instance exists but failed init
                // Shutdown existing failed instance before creating new
                tts.stop()
                tts.shutdown()
            }
            tts = TextToSpeech(requireContext(), this)
            Log.d("TTS", "Re-attempting TTS initialization in onResume.")
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        // Don't shutdown TTS in onPause if you want speech to complete if the app is briefly paused.
        // TTS will be shut down in onDestroy.
    }

    override fun onDestroyView() {
        // It's good practice to stop TTS if it's speaking when the view is destroyed,
        // though onDestroy will handle the full shutdown.
        if (::tts.isInitialized && tts.isSpeaking) {
            tts.stop()
        }
        super.onDestroyView()
    }


    override fun onDestroy() {
        // Shutdown TTS when the Fragment is destroyed to free resources.
        // This is very important.
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
            Log.i("TTS", "TTS shutdown.")
        }
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            // Proximity sensors usually report a small value (e.g., 0 or 5 cm) when an object is near
            // and a larger value when it's far. The exact values can vary by device.
            // You might need to experiment with this threshold.
            val threshold = proximitySensor?.maximumRange ?: 5.0f // Use max range or a default

            if (distance < threshold) {
                // Object is near
                if (!isNear) { // Only count when transitioning from far to near
                    isNear = true
                    currentPushupCount++
                    updatePushUpCountDisplay()
                    speakPushUpCount() // Speak the new count
                }
            } else {
                // Object is far
                if (isNear) {
                    isNear = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // You can handle accuracy changes here if needed, but for proximity, it's often not critical.
    }

    private fun updatePushUpCountDisplay() {
        textViewPushUpCount.text = currentPushupCount.toString()
    }

    private fun updateMaxPushupDisplay() {
        textViewMaxPushupValue.text = maxPushupCount.toString()
    }

    private fun resetCurrentPushUpCount() {
        currentPushupCount = 0
        isNear = false
        updatePushUpCountDisplay()
        if (isTtsInitialized && ::tts.isInitialized) {
            tts.speak("Count reset", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}

