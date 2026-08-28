package com.aatorque.speedometer.trip

import android.content.Context
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

object TripManager {

    private const val MIN_SPEED_THRESHOLD_KMH = 2.0
    private const val MAX_ACCURACY_METERS = 30.0f
    private const val MAX_REASONABLE_SPEED_KMH = 220.0
    private const val MIN_DISTANCE_DELTA_METERS = 2.0
    private const val SAVE_INTERVAL_MILLIS = 5000L

    private var tripStorage: TripStorage? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _tripData = MutableStateFlow(TripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()

    private var currentSession: TripSession? = null
    private var lastLocation: Location? = null
    private var lastSaveTimeMillis = 0L
    private var timerJob: Job? = null
    private var latestSpeedKmh: Double = 0.0

    fun init(context: Context) {
        if (tripStorage != null) return
        tripStorage = TripStorage(context.applicationContext)
        Timber.tag("TripManager").i("TripManager inicializado")

        scope.launch {
            val pendingSession = tripStorage?.loadSession()
            if (pendingSession != null && pendingSession.state != TripState.IDLE) {
                currentSession = pendingSession
                _tripData.update {
                    TripData(
                        state = pendingSession.state,
                        distanceMeters = pendingSession.distanceMeters,
                        maxSpeedKmh = pendingSession.maxSpeedKmh,
                        averageSpeedKmh = calculateMovingAverage(pendingSession.distanceMeters, pendingSession.movingTimeMillis, pendingSession.maxSpeedKmh),
                        totalAverageSpeedKmh = calculateTotalAverage(pendingSession.distanceMeters, pendingSession.movingTimeMillis + pendingSession.stoppedTimeMillis, pendingSession.maxSpeedKmh),
                        elapsedTimeMillis = pendingSession.movingTimeMillis + pendingSession.stoppedTimeMillis,
                        movingTimeMillis = pendingSession.movingTimeMillis,
                        stoppedTimeMillis = pendingSession.stoppedTimeMillis,
                        hasUnfinishedTrip = true
                    )
                }
                Timber.tag("TripManager").i("Viagem não finalizada encontrada no DataStore: %s", pendingSession.id)
            }
        }
    }

    fun startTrip() {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = TripSession(
            id = newId,
            startTime = now,
            state = TripState.RUNNING,
            distanceMeters = 0.0,
            maxSpeedKmh = 0f,
            movingTimeMillis = 0L,
            stoppedTimeMillis = 0L,
            lastUpdateTime = now
        )

        currentSession = session
        lastLocation = null
        latestSpeedKmh = 0.0

        _tripData.update {
            TripData(
                state = TripState.RUNNING,
                distanceMeters = 0.0,
                maxSpeedKmh = 0f,
                averageSpeedKmh = 0f,
                totalAverageSpeedKmh = 0f,
                elapsedTimeMillis = 0L,
                movingTimeMillis = 0L,
                stoppedTimeMillis = 0L,
                hasUnfinishedTrip = false
            )
        }

        startTimer()
        saveSessionAsync(session)
        Timber.tag("TripManager").i("Nova viagem iniciada: id=%s", newId)
    }

    fun pauseTrip() {
        val session = currentSession ?: return
        if (session.state != TripState.RUNNING) return

        stopTimer()
        lastLocation = null

        val updated = session.copy(
            state = TripState.PAUSED,
            lastUpdateTime = System.currentTimeMillis()
        )
        currentSession = updated

        _tripData.update { it.copy(state = TripState.PAUSED) }
        saveSessionAsync(updated)
        Timber.tag("TripManager").i("Viagem pausada: id=%s", session.id)
    }

    fun resumeTrip() {
        val session = currentSession ?: return
        if (session.state != TripState.PAUSED && session.state != TripState.FINISHED) return

        lastLocation = null

        val updated = session.copy(
            state = TripState.RUNNING,
            lastUpdateTime = System.currentTimeMillis()
        )
        currentSession = updated

        _tripData.update {
            it.copy(
                state = TripState.RUNNING,
                hasUnfinishedTrip = false
            )
        }

        startTimer()
        saveSessionAsync(updated)
        Timber.tag("TripManager").i("Viagem retomada: id=%s", session.id)
    }

    fun finishTrip() {
        val session = currentSession ?: return
        if (session.state == TripState.FINISHED || session.state == TripState.IDLE) return

        stopTimer()
        lastLocation = null

        val updated = session.copy(
            state = TripState.FINISHED,
            lastUpdateTime = System.currentTimeMillis()
        )
        currentSession = updated

        _tripData.update { it.copy(state = TripState.FINISHED) }

        scope.launch {
            tripStorage?.saveFinishedTripToHistory(updated)
            tripStorage?.saveSession(updated)
        }
        Timber.tag("TripManager").i("Viagem finalizada: id=%s dist=%.2fm", session.id, session.distanceMeters)
    }

    fun newTrip() {
        finishTrip()
        startTrip()
    }

    fun discardUnfinishedTrip() {
        stopTimer()
        currentSession = null
        lastLocation = null

        _tripData.update { TripData(state = TripState.IDLE, hasUnfinishedTrip = false) }

        scope.launch {
            tripStorage?.clearSession()
        }
        Timber.tag("TripManager").i("Viagem pendente descartada pelo usuário")
    }

    fun restoreUnfinishedTrip() {
        if (currentSession != null) {
            resumeTrip()
        }
    }

    fun processLocation(location: Location, filteredSpeedKmh: Double, accuracyMeters: Float) {
        latestSpeedKmh = filteredSpeedKmh
        val session = currentSession ?: return

        if (session.state != TripState.RUNNING) return

        // 1. Filter out poor accuracy
        if (accuracyMeters > MAX_ACCURACY_METERS) {
            Timber.tag("TripManager").d("Ignorando localização por precisão baixa (%.1fm > %.1fm)", accuracyMeters, MAX_ACCURACY_METERS)
            return
        }

        // 2. Clear location reference rule on start/resume
        val prev = lastLocation
        if (prev == null) {
            lastLocation = location
            Timber.tag("TripManager").d("Primeira localização válida pós-início/retomada registrada como ponto inicial")
            return
        }

        // 3. Time difference sanity check
        val timeDiffSec = (location.time - prev.time) / 1000.0
        if (timeDiffSec <= 0 || timeDiffSec > 60.0) {
            lastLocation = location
            Timber.tag("TripManager").d("Salto de tempo incomum (%.1fs). Reiniciando ponto de referência.", timeDiffSec)
            return
        }

        // 4. Distance delta
        val distDelta = location.distanceTo(prev).toDouble()

        // Filter micro-drift when stopped
        if (distDelta < MIN_DISTANCE_DELTA_METERS) {
            lastLocation = location
            return
        }

        // 5. Speed sanity check to prevent impossible GPS leaps
        val calcSpeedKmh = (distDelta / timeDiffSec) * 3.6
        if (calcSpeedKmh > MAX_REASONABLE_SPEED_KMH) {
            Timber.tag("TripManager").w("Ignorando salto de GPS impossível: %.1f km/h (dist=%.1fm, tempo=%.1fs)", calcSpeedKmh, distDelta, timeDiffSec)
            lastLocation = location
            return
        }

        // 6. Accumulate valid distance
        val newDistanceMeters = session.distanceMeters + distDelta
        lastLocation = location

        // 7. Max speed update (with protection against impossible spikes)
        val validCurrentSpeed = if (filteredSpeedKmh <= MAX_REASONABLE_SPEED_KMH) filteredSpeedKmh.toFloat() else session.maxSpeedKmh
        val newMaxSpeed = maxOf(session.maxSpeedKmh, validCurrentSpeed)

        val updated = session.copy(
            distanceMeters = newDistanceMeters,
            maxSpeedKmh = newMaxSpeed,
            lastValidLatitude = location.latitude,
            lastValidLongitude = location.longitude,
            lastUpdateTime = System.currentTimeMillis()
        )
        currentSession = updated

        val movingAverage = calculateMovingAverage(newDistanceMeters, updated.movingTimeMillis, updated.maxSpeedKmh)
        val totalAverage = calculateTotalAverage(newDistanceMeters, updated.movingTimeMillis + updated.stoppedTimeMillis, updated.maxSpeedKmh)

        _tripData.update { current ->
            current.copy(
                distanceMeters = newDistanceMeters,
                maxSpeedKmh = newMaxSpeed,
                averageSpeedKmh = movingAverage,
                totalAverageSpeedKmh = totalAverage
            )
        }

        // Periodic DataStore saving
        val now = System.currentTimeMillis()
        if (now - lastSaveTimeMillis > SAVE_INTERVAL_MILLIS) {
            lastSaveTimeMillis = now
            saveSessionAsync(updated)
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val session = currentSession ?: break
                if (session.state != TripState.RUNNING) break

                val isMoving = latestSpeedKmh >= MIN_SPEED_THRESHOLD_KMH
                val newMovingTime = if (isMoving) session.movingTimeMillis + 1000L else session.movingTimeMillis
                val newStoppedTime = if (!isMoving) session.stoppedTimeMillis + 1000L else session.stoppedTimeMillis
                val totalElapsed = newMovingTime + newStoppedTime

                val updatedSession = session.copy(
                    movingTimeMillis = newMovingTime,
                    stoppedTimeMillis = newStoppedTime,
                    lastUpdateTime = System.currentTimeMillis()
                )
                currentSession = updatedSession

                val movingAverage = calculateMovingAverage(updatedSession.distanceMeters, newMovingTime, updatedSession.maxSpeedKmh)
                val totalAverage = calculateTotalAverage(updatedSession.distanceMeters, totalElapsed, updatedSession.maxSpeedKmh)

                _tripData.update { current ->
                    current.copy(
                        movingTimeMillis = newMovingTime,
                        stoppedTimeMillis = newStoppedTime,
                        elapsedTimeMillis = totalElapsed,
                        averageSpeedKmh = movingAverage,
                        totalAverageSpeedKmh = totalAverage
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun saveSessionAsync(session: TripSession) {
        scope.launch {
            tripStorage?.saveSession(session)
        }
    }

    private fun calculateMovingAverage(distanceMeters: Double, movingTimeMillis: Long, maxSpeedKmh: Float = 0f): Float {
        if (movingTimeMillis <= 0) return 0f
        val movingTimeHours = movingTimeMillis / 3_600_000.0
        val distanceKm = distanceMeters / 1000.0
        val calculatedAvg = (distanceKm / movingTimeHours).toFloat().coerceAtLeast(0f)
        return if (maxSpeedKmh > 0f) minOf(calculatedAvg, maxSpeedKmh) else calculatedAvg
    }

    private fun calculateTotalAverage(distanceMeters: Double, totalTimeMillis: Long, maxSpeedKmh: Float = 0f): Float {
        if (totalTimeMillis <= 0) return 0f
        val totalTimeHours = totalTimeMillis / 3_600_000.0
        val distanceKm = distanceMeters / 1000.0
        val calculatedAvg = (distanceKm / totalTimeHours).toFloat().coerceAtLeast(0f)
        return if (maxSpeedKmh > 0f) minOf(calculatedAvg, maxSpeedKmh) else calculatedAvg
    }

    suspend fun loadTripHistory(): List<FinishedTrip> {
        return tripStorage?.loadAllFinishedTrips() ?: emptyList()
    }

    suspend fun deleteTripFromHistory(tripId: String) {
        tripStorage?.deleteFinishedTrip(tripId)
    }

    suspend fun clearTripHistory() {
        tripStorage?.clearTripHistory()
    }
}

