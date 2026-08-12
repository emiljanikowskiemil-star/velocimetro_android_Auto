package com.aatorque.speedometer.speedometer

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aatorque.speedometer.R
import com.aatorque.speedometer.databinding.ActivityGpsSpeedometerBinding
import com.aatorque.speedometer.gps.SpeedRepository
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme
import com.aatorque.speedometer.theme.ThemeRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class GpsSpeedometerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGpsSpeedometerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsSpeedometerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.tag("SpeedometerGPS").i("GpsSpeedometerActivity iniciada no smartphone")

        ThemeRepository.init(applicationContext)

        binding.btnThemeToggle.setOnClickListener {
            ThemeRepository.toggleTheme(applicationContext)
        }

        observeState()
        observeTheme()
    }

    private fun observeState() {
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
            }
        }
    }
}
