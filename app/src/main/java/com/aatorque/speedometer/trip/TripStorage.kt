package com.aatorque.speedometer.trip

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.tripDataStore: DataStore<Preferences> by preferencesDataStore(name = "trip_computer_prefs")

class TripStorage(private val context: Context) {

    companion object {
        private val KEY_ID = stringPreferencesKey("trip_id")
        private val KEY_START_TIME = longPreferencesKey("trip_start_time")
        private val KEY_STATE = stringPreferencesKey("trip_state")
        private val KEY_DISTANCE = doublePreferencesKey("trip_distance")
        private val KEY_MAX_SPEED = floatPreferencesKey("trip_max_speed")
        private val KEY_MOVING_TIME = longPreferencesKey("trip_moving_time")
        private val KEY_STOPPED_TIME = longPreferencesKey("trip_stopped_time")
        private val KEY_LAST_LAT = doublePreferencesKey("trip_last_lat")
        private val KEY_LAST_LNG = doublePreferencesKey("trip_last_lng")
        private val KEY_LAST_UPDATE = longPreferencesKey("trip_last_update")
        private val KEY_HISTORY_COUNT = longPreferencesKey("trip_history_count")
    }

    suspend fun saveSession(session: TripSession) {
        try {
            context.tripDataStore.edit { prefs ->
                prefs[KEY_ID] = session.id
                prefs[KEY_START_TIME] = session.startTime
                prefs[KEY_STATE] = session.state.name
                prefs[KEY_DISTANCE] = session.distanceMeters
                prefs[KEY_MAX_SPEED] = session.maxSpeedKmh
                prefs[KEY_MOVING_TIME] = session.movingTimeMillis
                prefs[KEY_STOPPED_TIME] = session.stoppedTimeMillis
                if (session.lastValidLatitude != null) {
                    prefs[KEY_LAST_LAT] = session.lastValidLatitude
                } else {
                    prefs.remove(KEY_LAST_LAT)
                }
                if (session.lastValidLongitude != null) {
                    prefs[KEY_LAST_LNG] = session.lastValidLongitude
                } else {
                    prefs.remove(KEY_LAST_LNG)
                }
                prefs[KEY_LAST_UPDATE] = session.lastUpdateTime
            }
            Timber.tag("TripStorage").d("Sessão de viagem salva: id=%s state=%s dist=%.2fm", session.id, session.state, session.distanceMeters)
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao salvar sessão de viagem no DataStore")
        }
    }

    suspend fun loadSession(): TripSession? {
        return try {
            val prefs = context.tripDataStore.data.first()
            val id = prefs[KEY_ID] ?: return null
            val stateName = prefs[KEY_STATE] ?: TripState.IDLE.name
            val state = try {
                TripState.valueOf(stateName)
            } catch (e: Exception) {
                TripState.IDLE
            }

            if (state == TripState.IDLE) return null

            TripSession(
                id = id,
                startTime = prefs[KEY_START_TIME] ?: 0L,
                state = state,
                distanceMeters = prefs[KEY_DISTANCE] ?: 0.0,
                maxSpeedKmh = prefs[KEY_MAX_SPEED] ?: 0f,
                movingTimeMillis = prefs[KEY_MOVING_TIME] ?: 0L,
                stoppedTimeMillis = prefs[KEY_STOPPED_TIME] ?: 0L,
                lastValidLatitude = prefs[KEY_LAST_LAT],
                lastValidLongitude = prefs[KEY_LAST_LNG],
                lastUpdateTime = prefs[KEY_LAST_UPDATE] ?: 0L
            )
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao carregar sessão de viagem do DataStore")
            null
        }
    }

    suspend fun clearSession() {
        try {
            context.tripDataStore.edit { prefs ->
                prefs.remove(KEY_ID)
                prefs.remove(KEY_START_TIME)
                prefs[KEY_STATE] = TripState.IDLE.name
                prefs.remove(KEY_DISTANCE)
                prefs.remove(KEY_MAX_SPEED)
                prefs.remove(KEY_MOVING_TIME)
                prefs.remove(KEY_STOPPED_TIME)
                prefs.remove(KEY_LAST_LAT)
                prefs.remove(KEY_LAST_LNG)
                prefs.remove(KEY_LAST_UPDATE)
            }
            Timber.tag("TripStorage").i("Sessão de viagem limpa do DataStore")
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao limpar sessão de viagem no DataStore")
        }
    }

    suspend fun saveFinishedTripToHistory(session: TripSession) {
        try {
            context.tripDataStore.edit { prefs ->
                val count = (prefs[KEY_HISTORY_COUNT] ?: 0L) + 1
                prefs[KEY_HISTORY_COUNT] = count
                // Save key metrics of finished trip under historical keys
                prefs[stringPreferencesKey("history_${session.id}_id")] = session.id
                prefs[longPreferencesKey("history_${session.id}_start")] = session.startTime
                prefs[longPreferencesKey("history_${session.id}_end")] = System.currentTimeMillis()
                prefs[doublePreferencesKey("history_${session.id}_dist")] = session.distanceMeters
                prefs[floatPreferencesKey("history_${session.id}_maxspeed")] = session.maxSpeedKmh
                prefs[longPreferencesKey("history_${session.id}_moving_time")] = session.movingTimeMillis
                prefs[longPreferencesKey("history_${session.id}_stopped_time")] = session.stoppedTimeMillis
            }
            Timber.tag("TripStorage").i("Viagem finalizada salva no histórico: %s", session.id)
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao salvar viagem finalizada no histórico")
        }
    }

    suspend fun loadAllFinishedTrips(): List<FinishedTrip> {
        return try {
            val prefs = context.tripDataStore.data.first()
            val allEntries = prefs.asMap()
            val tripIds = allEntries.keys
                .map { it.name }
                .filter { it.startsWith("history_") && it.endsWith("_id") }
                .map { it.removePrefix("history_").removeSuffix("_id") }
                .distinct()

            tripIds.mapNotNull { id ->
                val idVal = prefs[stringPreferencesKey("history_${id}_id")] ?: return@mapNotNull null
                val startTime = prefs[longPreferencesKey("history_${id}_start")] ?: 0L
                val endTime = prefs[longPreferencesKey("history_${id}_end")] ?: startTime
                val dist = prefs[doublePreferencesKey("history_${id}_dist")] ?: 0.0
                val maxSpeed = prefs[floatPreferencesKey("history_${id}_maxspeed")] ?: 0f
                val movingTime = prefs[longPreferencesKey("history_${id}_moving_time")] ?: 0L
                val stoppedTime = prefs[longPreferencesKey("history_${id}_stopped_time")] ?: 0L

                FinishedTrip(
                    id = idVal,
                    startTime = startTime,
                    endTime = endTime,
                    distanceMeters = dist,
                    maxSpeedKmh = maxSpeed,
                    movingTimeMillis = movingTime,
                    stoppedTimeMillis = stoppedTime
                )
            }.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao carregar histórico de viagens")
            emptyList()
        }
    }

    suspend fun deleteFinishedTrip(tripId: String) {
        try {
            context.tripDataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey("history_${tripId}_id"))
                prefs.remove(longPreferencesKey("history_${tripId}_start"))
                prefs.remove(longPreferencesKey("history_${tripId}_end"))
                prefs.remove(doublePreferencesKey("history_${tripId}_dist"))
                prefs.remove(floatPreferencesKey("history_${tripId}_maxspeed"))
                prefs.remove(longPreferencesKey("history_${tripId}_moving_time"))
                prefs.remove(longPreferencesKey("history_${tripId}_stopped_time"))
            }
            Timber.tag("TripStorage").i("Viagem %s removida do histórico", tripId)
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao deletar viagem do histórico")
        }
    }

    suspend fun clearTripHistory() {
        try {
            context.tripDataStore.edit { prefs ->
                val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith("history_") }
                keysToRemove.forEach { key ->
                    prefs.remove(key)
                }
                prefs.remove(KEY_HISTORY_COUNT)
            }
            Timber.tag("TripStorage").i("Histórico de viagens limpo com sucesso")
        } catch (e: Exception) {
            Timber.tag("TripStorage").e(e, "Erro ao limpar histórico de viagens")
        }
    }
}

