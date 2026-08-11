package com.aatorque.speedometer.gps

data class SpeedState(
    val rawSpeedKmh: Double = 0.0,
    val filteredSpeedKmh: Double = 0.0,
    val displaySpeed: Int = 0,
    val maxSpeedLimit: Int = 220,
    val unit: String = "km/h",
    val accuracyMeters: Float = 0.0f,
    val satelliteCount: Int = 0,
    val altitudeMeters: Double = 0.0,
    val tripDistanceKm: Double = 0.0,
    val isGpsActive: Boolean = false,
    val smoothingLevel: String = "MEDIUM",
    val themeMode: String = "AUTO",
    val keepScreenOn: Boolean = true,
    val showAccuracy: Boolean = true,
    val showSatellites: Boolean = true,
    val showAltitude: Boolean = true,
    val showTrip: Boolean = true
)
