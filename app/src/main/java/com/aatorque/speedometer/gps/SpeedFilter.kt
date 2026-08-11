package com.aatorque.speedometer.gps

import kotlin.math.abs

class SpeedFilter {

    private var currentFilteredSpeed: Double = 0.0
    private var lastTimestamp: Long = 0L

    // Minimum speed threshold below which speed is forced to 0.0 km/h
    private val zeroThresholdKmh: Double = 2.0

    /**
     * Filters raw speed input (in km/h).
     * @param rawSpeedKmh Raw speed in km/h received from GNSS
     * @param smoothingLevel "LOW", "MEDIUM", or "HIGH"
     * @return Filtered speed in km/h
     */
    fun filter(rawSpeedKmh: Double, smoothingLevel: String): Double {
        val now = System.currentTimeMillis()
        if (lastTimestamp == 0L) {
            currentFilteredSpeed = rawSpeedKmh
            lastTimestamp = now
            return if (currentFilteredSpeed < zeroThresholdKmh) 0.0 else currentFilteredSpeed
        }

        // Base alpha depending on requested smoothing level
        val baseAlpha = when (smoothingLevel) {
            "LOW" -> 0.60
            "HIGH" -> 0.20
            else -> 0.40 // MEDIUM
        }

        // Dynamic adjustment: if there's a big delta, increase alpha to eliminate perceived delay
        val delta = abs(rawSpeedKmh - currentFilteredSpeed)
        val alpha = if (delta > 15.0) {
            (baseAlpha * 1.8).coerceAtMost(0.95)
        } else if (delta > 7.0) {
            (baseAlpha * 1.4).coerceAtMost(0.85)
        } else {
            baseAlpha
        }

        currentFilteredSpeed = alpha * rawSpeedKmh + (1.0 - alpha) * currentFilteredSpeed
        lastTimestamp = now

        // Cut-off threshold when stopped
        if (currentFilteredSpeed < zeroThresholdKmh) {
            currentFilteredSpeed = 0.0
        }

        return currentFilteredSpeed
    }

    fun reset() {
        currentFilteredSpeed = 0.0
        lastTimestamp = 0L
    }
}
