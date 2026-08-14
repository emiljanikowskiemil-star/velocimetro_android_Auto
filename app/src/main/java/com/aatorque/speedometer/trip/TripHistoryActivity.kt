package com.aatorque.speedometer.trip

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatorque.speedometer.R
import com.aatorque.speedometer.databinding.ActivityTripHistoryBinding
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme
import com.aatorque.speedometer.theme.ThemeRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class TripHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripHistoryBinding
    private lateinit var adapter: TripHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.tag("TripHistory").i("TripHistoryActivity iniciada")

        ThemeRepository.init(applicationContext)
        TripManager.init(applicationContext)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClearAllHistory.setOnClickListener {
            showClearAllDialog()
        }

        setupRecyclerView()
        observeTheme()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        adapter = TripHistoryAdapter(
            onDeleteClick = { trip ->
                showDeleteSingleDialog(trip)
            }
        )
        binding.rvTripHistory.layoutManager = LinearLayoutManager(this)
        binding.rvTripHistory.adapter = adapter
    }

    private fun loadHistoryData() {
        lifecycleScope.launch {
            val trips = TripManager.loadTripHistory()
            updateUI(trips)
        }
    }

    private fun updateUI(trips: List<FinishedTrip>) {
        adapter.submitList(trips)

        if (trips.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvTripHistory.visibility = View.GONE
            binding.btnClearAllHistory.visibility = View.GONE
            binding.tvTotalTripsCount.text = "0"
            binding.tvTotalDistance.text = "0.0 km"
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvTripHistory.visibility = View.VISIBLE
            binding.btnClearAllHistory.visibility = View.VISIBLE

            val totalCount = trips.size
            val totalDistanceKm = trips.sumOf { it.distanceKm }

            binding.tvTotalTripsCount.text = totalCount.toString()
            binding.tvTotalDistance.text = String.format(Locale.getDefault(), "%.1f km", totalDistanceKm)
        }
    }

    private fun showDeleteSingleDialog(trip: FinishedTrip) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Viagem")
            .setMessage("Deseja realmente remover esta viagem do histórico?")
            .setPositiveButton("EXCLUIR") { _, _ ->
                lifecycleScope.launch {
                    TripManager.deleteTripFromHistory(trip.id)
                    loadHistoryData()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Limpar Histórico")
            .setMessage("Deseja apagar todas as viagens gravadas no histórico? Esta ação não pode ser desfeita.")
            .setPositiveButton("LIMPAR TUDO") { _, _ ->
                lifecycleScope.launch {
                    TripManager.clearTripHistory()
                    loadHistoryData()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            ThemeRepository.themeState.collectLatest { theme ->
                val colors = if (theme == SpeedometerTheme.DARK) DarkSpeedometerColors else LightSpeedometerColors
                binding.containerTripHistory.setBackgroundColor(colors.background)
                binding.topBar.setBackgroundColor(if (theme == SpeedometerTheme.DARK) 0xFF12161D.toInt() else 0xFFEAEAEA.toInt())
                binding.tvHeaderTitle.setTextColor(colors.primaryText)
                binding.btnBack.setColorFilter(colors.primaryText)

                val cardBg = if (theme == SpeedometerTheme.DARK) 0xFF161B22.toInt() else 0xFFFFFFFF.toInt()
                binding.cardSummary.setCardBackgroundColor(cardBg)
            }
        }
    }
}

