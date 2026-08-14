@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")
package com.aatorque.speedometer.androidauto

import android.os.Bundle
import android.view.View
import com.aatorque.speedometer.R
import com.aatorque.speedometer.databinding.ActivityAutoSpeedometerBinding
import com.aatorque.speedometer.gps.GpsForegroundService
import com.aatorque.speedometer.gps.SpeedRepository
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme
import com.aatorque.speedometer.theme.ThemeRepository
import com.aatorque.speedometer.trip.TripManager
import com.aatorque.speedometer.trip.TripState
import com.google.android.apps.auto.sdk.CarActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class AutoSpeedometerActivity : CarActivity() {

    private lateinit var binding: ActivityAutoSpeedometerBinding
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        Timber.tag("SpeedometerAuto").i("AutoSpeedometerActivity criada no Android Auto")

        setContentView(R.layout.activity_auto_speedometer)
        binding = ActivityAutoSpeedometerBinding.bind(findViewById(R.id.containerAutoSpeedometer))

        ThemeRepository.init(applicationContext)
        TripManager.init(applicationContext)
        GpsForegroundService.start(applicationContext)

        binding.btnThemeToggle.setOnClickListener {
            ThemeRepository.toggleTheme(applicationContext)
        }

        // Set Android Auto Status Bar Title
        try {
            carUiController.statusBarController.setTitle(getString(R.string.auto_app_name))
        } catch (e: Exception) {
            Timber.tag("SpeedometerAuto").w(e, "Não foi possível definir título na barra de status do carUiController")
        }

        setIgnoreConfigChanges(0xFFFF)

        setupTripControlButtons()
        observeSpeedState()
        observeTripState()
        observeThemeState()
    }

    private fun setupTripControlButtons() {
        binding.btnAutoTripStartResume.setOnClickListener {
            val currentState = TripManager.tripData.value.state
            if (currentState == TripState.PAUSED) {
                TripManager.resumeTrip()
            } else {
                TripManager.startTrip()
            }
        }

        binding.btnAutoTripPause.setOnClickListener {
            TripManager.pauseTrip()
        }

        binding.btnAutoTripFinish.setOnClickListener {
            TripManager.finishTrip()
        }

        binding.btnAutoTripNew.setOnClickListener {
            TripManager.newTrip()
        }
    }

    private fun observeSpeedState() {
        activityScope.launch {
            SpeedRepository.speedState.collectLatest { state ->
                binding.autoSpeedometerView.updateState(state)
            }
        }
    }

    private fun observeTripState() {
        activityScope.launch {
            TripManager.tripData.collectLatest { data ->
                binding.autoSpeedometerView.updateTripData(data)
                updateTripButtonsUI(data.state)
            }
        }
    }

    private fun updateTripButtonsUI(state: TripState) {
        when (state) {
            TripState.IDLE -> {
                binding.btnAutoTripStartResume.visibility = View.VISIBLE
                binding.btnAutoTripStartResume.text = "▶ INICIAR VIAGEM"
                binding.btnAutoTripPause.visibility = View.GONE
                binding.btnAutoTripFinish.visibility = View.GONE
                binding.btnAutoTripNew.visibility = View.GONE
            }
            TripState.RUNNING -> {
                binding.btnAutoTripStartResume.visibility = View.GONE
                binding.btnAutoTripPause.visibility = View.VISIBLE
                binding.btnAutoTripPause.text = "⏸ PAUSAR"
                binding.btnAutoTripFinish.visibility = View.VISIBLE
                binding.btnAutoTripNew.visibility = View.GONE
            }
            TripState.PAUSED -> {
                binding.btnAutoTripStartResume.visibility = View.VISIBLE
                binding.btnAutoTripStartResume.text = "▶ RETOMAR"
                binding.btnAutoTripPause.visibility = View.GONE
                binding.btnAutoTripFinish.visibility = View.VISIBLE
                binding.btnAutoTripNew.visibility = View.GONE
            }
            TripState.FINISHED -> {
                binding.btnAutoTripStartResume.visibility = View.GONE
                binding.btnAutoTripPause.visibility = View.GONE
                binding.btnAutoTripFinish.visibility = View.GONE
                binding.btnAutoTripNew.visibility = View.VISIBLE
            }
        }
    }

    private fun observeThemeState() {
        activityScope.launch {
            ThemeRepository.themeState.collectLatest { theme ->
                val colors = if (theme == SpeedometerTheme.DARK) DarkSpeedometerColors else LightSpeedometerColors
                binding.containerAutoSpeedometer.setBackgroundColor(colors.background)
                binding.autoSpeedometerView.setTheme(theme)

                if (theme == SpeedometerTheme.DARK) {
                    binding.btnThemeToggle.setImageResource(R.drawable.ic_light_mode)
                } else {
                    binding.btnThemeToggle.setImageResource(R.drawable.ic_dark_mode)
                }
                binding.btnThemeToggle.setColorFilter(colors.themeButtonIconColor)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.tag("SpeedometerAuto").i("AutoSpeedometerActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("SpeedometerAuto").i("AutoSpeedometerActivity onResume")
    }

    override fun onPause() {
        Timber.tag("SpeedometerAuto").i("AutoSpeedometerActivity onPause")
        super.onPause()
    }

    override fun onStop() {
        Timber.tag("SpeedometerAuto").i("AutoSpeedometerActivity onStop")
        super.onStop()
    }
}
