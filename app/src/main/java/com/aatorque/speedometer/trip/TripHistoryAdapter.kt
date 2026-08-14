package com.aatorque.speedometer.trip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aatorque.speedometer.databinding.ItemTripHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripHistoryAdapter(
    private val onDeleteClick: (FinishedTrip) -> Unit
) : ListAdapter<FinishedTrip, TripHistoryAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripViewHolder(
        private val binding: ItemTripHistoryBinding,
        private val onDeleteClick: (FinishedTrip) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

        fun bind(trip: FinishedTrip) {
            val dateStr = dateFormat.format(Date(trip.startTime))
            binding.tvTripDate.text = dateStr
            binding.tvTripDistance.text = String.format(Locale.getDefault(), "%.1f km", trip.distanceKm)
            binding.tvTripMovingTime.text = formatDuration(trip.movingTimeMillis)
            binding.tvTripMaxSpeed.text = String.format(Locale.getDefault(), "%.0f km/h", trip.maxSpeedKmh)
            binding.tvTripAvgSpeed.text = String.format(Locale.getDefault(), "%.0f km/h", trip.averageSpeedKmh)

            binding.btnDeleteTrip.setOnClickListener {
                onDeleteClick(trip)
            }
        }

        private fun formatDuration(millis: Long): String {
            val totalSec = millis / 1000
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private class TripDiffCallback : DiffUtil.ItemCallback<FinishedTrip>() {
        override fun areItemsTheSame(oldItem: FinishedTrip, newItem: FinishedTrip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FinishedTrip, newItem: FinishedTrip): Boolean {
            return oldItem == newItem
        }
    }
}
