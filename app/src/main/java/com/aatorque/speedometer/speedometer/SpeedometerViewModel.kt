package com.aatorque.speedometer.speedometer

import androidx.lifecycle.ViewModel
import com.aatorque.speedometer.gps.SpeedRepository
import com.aatorque.speedometer.gps.SpeedState
import kotlinx.coroutines.flow.StateFlow

class SpeedometerViewModel : ViewModel() {

    val speedState: StateFlow<SpeedState> = SpeedRepository.speedState

    fun resetTrip() {
        SpeedRepository.resetTrip()
    }
}
