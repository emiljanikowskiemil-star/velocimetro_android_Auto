package com.aatorque.speedometer.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class FinishedTripTest {

    @Test
    fun testDistanceKmCalculation() {
        val trip = FinishedTrip(
            id = "test-1",
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 15500.0,
            maxSpeedKmh = 120f,
            movingTimeMillis = 1_800_000L, // 30 mins
            stoppedTimeMillis = 300_000L
        )

        assertEquals(15.5, trip.distanceKm, 0.001)
    }

    @Test
    fun testAverageSpeedCalculation() {
        val trip = FinishedTrip(
            id = "test-2",
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 30000.0, // 30 km
            maxSpeedKmh = 100f,
            movingTimeMillis = 1_800_000L, // 0.5 hours
            stoppedTimeMillis = 0L
        )

        // 30 km / 0.5 hours = 60 km/h
        assertEquals(60.0f, trip.averageSpeedKmh, 0.01f)
    }

    @Test
    fun testTotalDurationCalculation() {
        val trip = FinishedTrip(
            id = "test-3",
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 5000.0,
            maxSpeedKmh = 50f,
            movingTimeMillis = 600_000L, // 10 mins
            stoppedTimeMillis = 300_000L // 5 mins
        )

        assertEquals(900_000L, trip.totalDurationMillis)
    }
}
