package com.example.a100pushupchallenge.ui.statistics // Or your appropriate package

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.a100pushupchallenge.R // Ensure this matches your R file package
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment() {

    private lateinit var recyclerViewStatistics: RecyclerView
    private lateinit var textViewNoStats: TextView
    private lateinit var statisticsAdapter: StatisticsAdapter
    private val statisticRecords = mutableListOf<StatisticRecord>()

    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "PushUpPrefs" // Same as in WorkoutFragment
    private val KEY_ALL_WORKOUT_RECORDS = "allWorkoutRecords" // Same as in WorkoutFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        recyclerViewStatistics = view.findViewById(R.id.recyclerViewStatistics)
        textViewNoStats = view.findViewById(R.id.textViewNoStats)

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setupRecyclerView()
        loadStatistics()

        return view
    }

    private fun setupRecyclerView() {
        statisticsAdapter = StatisticsAdapter(statisticRecords)
        recyclerViewStatistics.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = statisticsAdapter
        }
    }

    private fun loadStatistics() {
        statisticRecords.clear()
        val allRecordsStringSet = sharedPreferences.getStringSet(KEY_ALL_WORKOUT_RECORDS, HashSet()) ?: HashSet()

        if (allRecordsStringSet.isEmpty()) {
            textViewNoStats.visibility = View.VISIBLE
            recyclerViewStatistics.visibility = View.GONE
        } else {
            textViewNoStats.visibility = View.GONE
            recyclerViewStatistics.visibility = View.VISIBLE

            for (recordString in allRecordsStringSet) {
                try {
                    val parts = recordString.split("_")
                    if (parts.size == 3) {
                        val duration = parts[0].toLong()
                        val pushups = parts[1].toInt()
                        val timeRemaining = parts[2].toLong()
                        statisticRecords.add(StatisticRecord(duration, pushups, timeRemaining))
                    }
                } catch (e: Exception) {
                    Log.e("StatisticsFragment", "Error parsing record: $recordString", e)
                }
            }
            // Sort records, e.g., by duration
            statisticRecords.sortBy { it.durationMs }
            statisticsAdapter.notifyDataSetChanged()
        }
    }
}
