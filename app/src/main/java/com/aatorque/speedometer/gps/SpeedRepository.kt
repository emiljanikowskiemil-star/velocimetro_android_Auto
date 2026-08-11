package com.aatorque.speedometer.gps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

object SpeedRepository {

    private val _speedState = MutableStateFlow(SpeedState())
    val speedState: StateFlow<SpeedState> = _speedState.asStateFlow()

    private val speedFilter = SpeedFilter()

    fun updateRawSpeed(
        rawSpeedMetersPerSec: Float,
        accuracyMeters: Float,
        altitudeMeters: Double,
        tripDeltaDistanceMeters: Double = 0.0
    ) {
        val rawKmh = (rawSpeedMetersPerSec * 3.6).coerceAtLeast(0.0)
        val smoothing = _speedState.value.smoothingLevel
        val filteredKmh = speedFilter.filter(rawKmh, smoothing)
        val unit = _speedState.value.unit

        val speedInSelectedUnit = if (unit.equals("mph", ignoreCase = true)) {
            filteredKmh * 0.621371
        } else {
            filteredKmh
        }

        val displaySpeed = Math.round(speedInSelectedUnit).toInt()
        val deltaKm = tripDeltaDistanceMeters / 1000.0

        _speedState.update { current ->
            current.copy(
                rawSpeedKmh = rawKmh,
                filteredSpeedKmh = filteredKmh,
                displaySpeed = displaySpeed,
                accuracyMeters = accuracyMeters,
                altitudeMeters = altitudeMeters,
                tripDistanceKm = current.tripDistanceKm + deltaKm,
                isGpsActive = true
            )
        }

        Timber.tag("SpeedometerGPS").d(
            "GPS Update: Raw=%.1f km/h, Filtered=%.1f km/h, Satellites=%d, Acc=%.1fm",
            rawKmh, filteredKmh, _speedState.value.satelliteCount, accuracyMeters
        )
    }

    fun updateSatelliteCount(count: Int) {
        _speedState.update { it.copy(satelliteCount = count) }
        Timber.tag("SpeedometerGPS").d("Satellites count updated: %d", count)
    }

    fun updateGpsStatus(active: Boolean) {
        _speedState.update { it.copy(isGpsActive = active) }
        if (!active) {
            speedFilter.reset()
        }
        Timber.tag("SpeedometerGPS").d("GPS Active state changed: %b", active)
    }

    fun updateSettings(
        maxSpeedLimit: Int? = null,
        smoothingLevel: String? = null,
        themeMode: String? = null,
        unit: String? = null,
        keepScreenOn: Boolean? = null,
        showAccuracy: Boolean? = null,
        showSatellites: Boolean? = null,
        showAltitude: Boolean? = null,
        showTrip: Boolean? = null
    ) {
        _speedState.update { current ->
            current.copy(
                maxSpeedLimit = maxSpeedLimit ?: current.maxSpeedLimit,
                smoothingLevel = smoothingLevel ?: current.smoothingLevel,
                themeMode = themeMode ?: current.themeMode,
                unit = unit ?: current.unit,
                keepScreenOn = keepScreenOn ?: current.keepScreenOn,
                showAccuracy = showAccuracy ?: current.showAccuracy,
                showSatellites = showSatellites ?: current.showSatellites,
                showAltitude = showAltitude ?: current.showAltitude,
                showTrip = showTrip ?: current.showTrip
            )
        }
    }

    fun resetTrip() {
        _speedState.update { it.copy(tripDistanceKm = 0.0) }
        Timber.tag("SpeedometerGPS").i("Trip distance reset to 0.0 km")
    }
}
