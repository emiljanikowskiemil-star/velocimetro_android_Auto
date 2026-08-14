package com.aatorque.speedometer.speedometer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aatorque.speedometer.R
import com.aatorque.speedometer.databinding.ActivityGpsSpeedometerBinding
import com.aatorque.speedometer.gps.GpsForegroundService
import com.aatorque.speedometer.gps.SpeedRepository
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme
import com.aatorque.speedometer.theme.ThemeRepository
import com.aatorque.speedometer.trip.TripData
import com.aatorque.speedometer.trip.TripDetailsBottomSheet
import com.aatorque.speedometer.trip.TripManager
import com.aatorque.speedometer.trip.TripState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

import android.content.Intent
import com.aatorque.speedometer.trip.TripHistoryActivity

class GpsSpeedometerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGpsSpeedometerBinding
    private var unfinishedDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsSpeedometerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.tag("SpeedometerGPS").i("GpsSpeedometerActivity iniciada no smartphone")

        ThemeRepository.init(applicationContext)
        GpsForegroundService.start(applicationContext)

        binding.btnThemeToggle.setOnClickListener {
            ThemeRepository.toggleTheme(applicationContext)
        }

        binding.btnTripHistory.setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }

        binding.speedometerView.onTripPanelClickListener = {
            TripDetailsBottomSheet.newInstance().show(supportFragmentManager, TripDetailsBottomSheet.TAG)
        }


        setupTripControlButtons()
        observeSpeedState()
        observeTripState()
        observeTheme()
    }

    private fun setupTripControlButtons() {
        binding.btnTripStartResume.setOnClickListener {
            val currentState = TripManager.tripData.value.state
            if (currentState == TripState.PAUSED) {
                TripManager.resumeTrip()
            } else {
                TripManager.startTrip()
            }
        }

        binding.btnTripPause.setOnClickListener {
            TripManager.pauseTrip()
        }

        binding.btnTripFinish.setOnClickListener {
            TripManager.finishTrip()
        }

        binding.btnTripNew.setOnClickListener {
            TripManager.newTrip()
        }
    }

    private fun observeSpeedState() {
        lifecycleScope.launch {
            SpeedRepository.speedState.collectLatest { state ->
                binding.speedometerView.updateState(state)

                if (state.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    private fun observeTripState() {
        lifecycleScope.launch {
            TripManager.tripData.collectLatest { data ->
                binding.speedometerView.updateTripData(data)
                updateTripButtonsUI(data.state)

                if (data.hasUnfinishedTrip && !unfinishedDialogShown) {
                    unfinishedDialogShown = true
                    showUnfinishedTripDialog(data)
                }
            }
        }
    }

    private fun updateTripButtonsUI(state: TripState) {
        when (state) {
            TripState.IDLE -> {
                binding.btnTripStartResume.visibility = View.VISIBLE
                binding.btnTripStartResume.text = "▶ INICIAR VIAGEM"
                binding.btnTripPause.visibility = View.GONE
                binding.btnTripFinish.visibility = View.GONE
                binding.btnTripNew.visibility = View.GONE
            }
            TripState.RUNNING -> {
                binding.btnTripStartResume.visibility = View.GONE
                binding.btnTripPause.visibility = View.VISIBLE
                binding.btnTripPause.text = "⏸ PAUSAR"
                binding.btnTripFinish.visibility = View.VISIBLE
                binding.btnTripNew.visibility = View.GONE
            }
            TripState.PAUSED -> {
                binding.btnTripStartResume.visibility = View.VISIBLE
                binding.btnTripStartResume.text = "▶ RETOMAR"
                binding.btnTripPause.visibility = View.GONE
                binding.btnTripFinish.visibility = View.VISIBLE
                binding.btnTripNew.visibility = View.GONE
            }
            TripState.FINISHED -> {
                binding.btnTripStartResume.visibility = View.GONE
                binding.btnTripPause.visibility = View.GONE
                binding.btnTripFinish.visibility = View.GONE
                binding.btnTripNew.visibility = View.VISIBLE
            }
        }
    }

    private fun showUnfinishedTripDialog(data: TripData) {
        val movingMin = data.movingTimeMillis / 60_000
        val message = "Existe uma viagem não finalizada.\n\n" +
                "Distância: %.1f km\n".format(data.distanceKm) +
                "Tempo em movimento: %d min\n".format(movingMin) +
                "Top Speed: %.0f km/h".format(data.maxSpeedKmh)

        AlertDialog.Builder(this)
            .setTitle("Viagem Não Finalizada")
            .setMessage(message)
            .setPositiveButton("RETOMAR VIAGEM") { _, _ ->
                TripManager.restoreUnfinishedTrip()
            }
            .setNeutralButton("FINALIZAR") { _, _ ->
                TripManager.finishTrip()
            }
            .setNegativeButton("DESCARTAR") { _, _ ->
                TripManager.discardUnfinishedTrip()
            }
            .setCancelable(false)
            .show()
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            ThemeRepository.themeState.collectLatest { theme ->
                val colors = if (theme == SpeedometerTheme.DARK) DarkSpeedometerColors else LightSpeedometerColors
                binding.containerSpeedometer.setBackgroundColor(colors.background)
                binding.speedometerView.setTheme(theme)

                if (theme == SpeedometerTheme.DARK) {
                    binding.btnThemeToggle.setImageResource(R.drawable.ic_light_mode)
                } else {
                    binding.btnThemeToggle.setImageResource(R.drawable.ic_dark_mode)
                }
                binding.btnThemeToggle.setColorFilter(colors.themeButtonIconColor)
                binding.btnTripHistory.setColorFilter(colors.themeButtonIconColor)
            }
        }
    }
}

