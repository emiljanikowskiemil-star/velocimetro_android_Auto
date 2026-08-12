@file:Suppress("MISSING_DEPENDENCY_CLASS", "MISSING_DEPENDENCY_SUPERCLASS")
package com.aatorque.speedometer.androidauto

import android.os.Bundle
import com.aatorque.speedometer.R
import com.aatorque.speedometer.databinding.ActivityAutoSpeedometerBinding
import com.aatorque.speedometer.gps.SpeedRepository
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme
import com.aatorque.speedometer.theme.ThemeRepository
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

        observeSpeedState()
        observeThemeState()
    }

    private fun observeSpeedState() {
        activityScope.launch {
            SpeedRepository.speedState.collectLatest { state ->
                binding.autoSpeedometerView.updateState(state)
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
