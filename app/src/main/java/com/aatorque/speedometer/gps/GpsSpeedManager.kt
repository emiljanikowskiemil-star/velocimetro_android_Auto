package com.aatorque.speedometer.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import timber.log.Timber

class GpsSpeedManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var locationCallback: LocationCallback? = null
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var legacyLocationListener: LocationListener? = null
    private var lastLocation: Location? = null
    private var isStarted = false

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isStarted) return
        isStarted = true
        Timber.tag("SpeedometerLocation").i("Iniciando atualizações de localização GPS GNSS")

        setupFusedLocation()
        setupFallbackLocationManager()
        setupGnssStatusCallback()
        SpeedRepository.updateGpsStatus(true)
    }

    @SuppressLint("MissingPermission")
    private fun setupFusedLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 150L)
            .setMinUpdateIntervalMillis(100L)
            .setMaxUpdateDelayMillis(200L)
            .setMinUpdateDistanceMeters(0.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Timber.tag("SpeedometerLocation").e(e, "Erro ao solicitar localização via FusedLocationProviderClient")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupFallbackLocationManager() {
        legacyLocationListener = LocationListener { location ->
            processLocation(location)
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    100L,
                    0f,
                    legacyLocationListener!!,
                    Looper.getMainLooper()
                )
                Timber.tag("SpeedometerLocation").i("LocationManager GPS_PROVIDER registrado com sucesso")
            }
        } catch (e: Exception) {
            Timber.tag("SpeedometerLocation").e(e, "Erro ao registrar LocationManager GPS_PROVIDER")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGnssStatusCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val totalCount = status.satelliteCount
                    var usedCount = 0
                    for (i in 0 until totalCount) {
                        if (status.usedInFix(i)) {
                            usedCount++
                        }
                    }
                    SpeedRepository.updateSatelliteCount(usedCount)
                }
            }
            try {
                locationManager.registerGnssStatusCallback(gnssStatusCallback!!, null)
            } catch (e: Exception) {
                Timber.tag("SpeedometerLocation").w(e, "Erro ao registrar GnssStatusCallback")
            }
        }
    }

    private fun processLocation(location: Location) {
        val prev = lastLocation
        if (prev != null && location.time <= prev.time && location.latitude == prev.latitude && location.longitude == prev.longitude) {
            return
        }

        val speedMps = if (location.hasSpeed()) {
            location.speed
        } else if (prev != null && location.time > prev.time) {
            val dist = location.distanceTo(prev)
            val timeSec = (location.time - prev.time) / 1000.0
            if (timeSec > 0) (dist / timeSec).toFloat() else 0f
        } else {
            0f
        }

        val distanceDelta = if (prev != null) {
            location.distanceTo(prev).toDouble()
        } else 0.0

        lastLocation = location

        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        val altitude = if (location.hasAltitude()) location.altitude else 0.0

        SpeedRepository.updateRawSpeed(
            location = location,
            rawSpeedMetersPerSec = speedMps,
            accuracyMeters = accuracy,
            altitudeMeters = altitude,
            tripDeltaDistanceMeters = distanceDelta
        )
    }

    fun stopLocationUpdates() {
        if (!isStarted) return
        isStarted = false
        Timber.tag("SpeedometerLocation").i("Parando atualizações de localização GPS GNSS")

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        legacyLocationListener?.let { locationManager.removeUpdates(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback != null) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback!!)
        }

        SpeedRepository.updateGpsStatus(false)
    }
}
