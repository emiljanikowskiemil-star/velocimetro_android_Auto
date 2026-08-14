package com.aatorque.speedometer.speedometer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aatorque.speedometer.databinding.ActivityMainBinding
import com.aatorque.speedometer.gps.GpsForegroundService
import com.aatorque.speedometer.gps.SpeedRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

import com.aatorque.speedometer.trip.TripHistoryActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            Timber.tag("SpeedometerGPS").i("Permissões de localização concedidas")
            startGpsService()
        } else {
            Toast.makeText(
                this,
                "Permissão de localização é necessária para medir a velocidade.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()

        binding.cardSpeedometer.setOnClickListener {
            startActivity(Intent(this, GpsSpeedometerActivity::class.java))
        }

        binding.btnOpenSpeedometer.setOnClickListener {
            startActivity(Intent(this, GpsSpeedometerActivity::class.java))
        }

        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        observeSpeedState()
    }


    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            locationPermissionLauncher.launch(missing.toTypedArray())
        } else {
            startGpsService()
        }
    }

    private fun startGpsService() {
        GpsForegroundService.start(this)
    }

    private fun observeSpeedState() {
        lifecycleScope.launch {
            SpeedRepository.speedState.collectLatest { state ->
                binding.tvCardSpeedPreview.text = "🏍  ${state.displaySpeed} ${state.unit}"
            }
        }
    }
}
