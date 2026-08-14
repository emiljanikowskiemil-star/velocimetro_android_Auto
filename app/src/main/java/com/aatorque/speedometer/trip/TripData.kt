package com.aatorque.speedometer.trip

data class TripData(
    val state: TripState = TripState.IDLE,
    val distanceMeters: Double = 0.0,
    val maxSpeedKmh: Float = 0f,
    val averageSpeedKmh: Float = 0f,
    val totalAverageSpeedKmh: Float = 0f,
    val elapsedTimeMillis: Long = 0L,
    val movingTimeMillis: Long = 0L,
    val stoppedTimeMillis: Long = 0L,
    val hasUnfinishedTrip: Boolean = false
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0
}
