package com.example.a100pushupchallenge.ui.statistics // Or your appropriate package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.a100pushupchallenge.R
import java.util.Locale
import java.util.concurrent.TimeUnit

class StatisticsAdapter(private val records: List<StatisticRecord>) :
    RecyclerView.Adapter<StatisticsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistic_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
    }

    override fun getItemCount(): Int = records.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewDuration: TextView = itemView.findViewById(R.id.textViewStatDuration)
        private val textViewPushups: TextView = itemView.findViewById(R.id.textViewStatPushups)
        private val textViewTimeRemaining: TextView = itemView.findViewById(R.id.textViewStatTimeRemaining)

        fun bind(record: StatisticRecord) {
            val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(record.durationMs)
            textViewDuration.text = itemView.context.getString(R.string.statistic_timer_duration, durationMinutes)
            textViewPushups.text = itemView.context.getString(R.string.statistic_max_pushups, record.maxPushups)
            textViewTimeRemaining.text = itemView.context.getString(R.string.statistic_time_remaining, formatTime(record.timeRemainingMs))
        }

        private fun formatTime(millis: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
