package com.example.a100pushupchallenge.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.a100pushupchallenge.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    // --- START: Add or modify this method ---
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the description TextView from the binding
        val descriptionTextView: TextView = binding.textDescription

        // Set the descriptive text
        descriptionTextView.text = """
            Welcome to the 100 Push-up Challenge!

            • Workout Tab:
            Place your phone flat on the floor and start your session. Each time your face gets close to the screen, a push-up is counted.

            • Statistics Tab:
            Review your workout history and track your personal bests for different timer settings.

            • Settings Tab:
            Enable hourly reminders to stay consistent. You can set a custom time range for when you want to receive notifications.
            
            Let's get started!
        """.trimIndent()
    }
    // --- END ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
