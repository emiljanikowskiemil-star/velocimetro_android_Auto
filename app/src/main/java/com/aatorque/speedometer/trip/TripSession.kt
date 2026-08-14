package com.aatorque.speedometer.trip

data class TripSession(
    val id: String = "",
    val startTime: Long = 0L,
    val state: TripState = TripState.IDLE,
    val distanceMeters: Double = 0.0,
    val maxSpeedKmh: Float = 0f,
    val movingTimeMillis: Long = 0L,
    val stoppedTimeMillis: Long = 0L,
    val lastValidLatitude: Double? = null,
    val lastValidLongitude: Double? = null,
    val lastUpdateTime: Long = 0L
)
