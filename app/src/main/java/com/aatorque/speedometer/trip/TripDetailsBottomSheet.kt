package com.aatorque.speedometer.trip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.aatorque.speedometer.databinding.DialogTripDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TripDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogTripDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseDetail.setOnClickListener {
            dismiss()
        }

        binding.btnOpenHistory.setOnClickListener {
            dismiss()
            startActivity(Intent(requireContext(), TripHistoryActivity::class.java))
        }

        observeTripData()
    }


    private fun observeTripData() {
        viewLifecycleOwner.lifecycleScope.launch {
            TripManager.tripData.collectLatest { data ->
                binding.tvDetailDistance.text = String.format("%.1f km", data.distanceKm)
                binding.tvDetailMaxSpeed.text = String.format("%.0f km/h", data.maxSpeedKmh)
                binding.tvDetailAvgSpeed.text = String.format("%.0f km/h", data.averageSpeedKmh)
                binding.tvDetailTotalTime.text = formatDuration(data.elapsedTimeMillis)
                binding.tvDetailMovingTime.text = formatDuration(data.movingTimeMillis)
                binding.tvDetailStoppedTime.text = formatDuration(data.stoppedTimeMillis)

                binding.tvDetailState.text = when (data.state) {
                    TripState.IDLE -> "NÃO INICIADA"
                    TripState.RUNNING -> "EM ANDAMENTO"
                    TripState.PAUSED -> "PAUSADA"
                    TripState.FINISHED -> "FINALIZADA"
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TripDetailsBottomSheet"
        fun newInstance() = TripDetailsBottomSheet()
    }
}
