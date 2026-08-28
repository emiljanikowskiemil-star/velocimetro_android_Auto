package com.aatorque.speedometer.trip

data class FinishedTrip(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val maxSpeedKmh: Float,
    val movingTimeMillis: Long,
    val stoppedTimeMillis: Long
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val averageSpeedKmh: Float
        get() {
            if (movingTimeMillis <= 0) return 0f
            val movingHours = movingTimeMillis / 3_600_000.0
            val calculatedAvg = (distanceKm / movingHours).toFloat().coerceAtLeast(0f)
            return if (maxSpeedKmh > 0f) minOf(calculatedAvg, maxSpeedKmh) else calculatedAvg
        }

    val totalDurationMillis: Long
        get() = movingTimeMillis + stoppedTimeMillis
}
