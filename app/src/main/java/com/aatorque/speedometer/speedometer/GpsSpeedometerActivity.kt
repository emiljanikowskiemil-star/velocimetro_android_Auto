package com.aatorque.speedometer.speedometer

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aatorque.speedometer.databinding.ActivityGpsSpeedometerBinding
import com.aatorque.speedometer.gps.SpeedRepository
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

        observeState()
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
}
