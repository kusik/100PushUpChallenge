package com.example.a100pushupchallenge.ui.workout // Replace with your actual package name

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.a100pushupchallenge.R // Replace with your actual R file

class WorkoutFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private lateinit var textViewPushupCount: TextView
    private lateinit var buttonReset: Button

    private var pushupCount = 0
    private var isNear = false // To track if the sensor is currently detecting proximity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false) // Or your new layout file

        textViewPushupCount = view.findViewById(R.id.textViewPushupCount)
        buttonReset = view.findViewById(R.id.buttonReset)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            Toast.makeText(requireContext(), "Proximity sensor not available!", Toast.LENGTH_SHORT).show()
            // Handle the case where the sensor is not available
        }

        buttonReset.setOnClickListener {
            resetPushupCount()
        }

        updatePushupCountDisplay() // Initialize display

        return view
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
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
                    pushupCount++
                    updatePushupCountDisplay()
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

    private fun updatePushupCountDisplay() {
        textViewPushupCount.text = pushupCount.toString()
    }

    private fun resetPushupCount() {
        pushupCount = 0
        isNear = false // Reset state
        updatePushupCountDisplay()
    }
}
