package com.aatorque.speedometer.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class TripDataTest {

    @Test
    fun testDistanceKmCalculation() {
        val tripData = TripData(distanceMeters = 55000.0)
        assertEquals(55.0, tripData.distanceKm, 0.001)
    }

    @Test
    fun testDefaultStateIsIdle() {
        val tripData = TripData()
        assertEquals(TripState.IDLE, tripData.state)
        assertEquals(0.0, tripData.distanceMeters, 0.0)
        assertEquals(0f, tripData.maxSpeedKmh, 0f)
    }
}
