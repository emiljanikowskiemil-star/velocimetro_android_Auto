package com.aatorque.speedometer.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aatorque.speedometer.gps.SpeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object ThemeRepository {

    private val THEME_KEY = stringPreferencesKey("speedometer_theme_key")

    private val _themeState = MutableStateFlow(SpeedometerTheme.DARK)
    val themeState: StateFlow<SpeedometerTheme> = _themeState.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        repositoryScope.launch {
            try {
                val savedThemeName = context.themeDataStore.data.map { preferences ->
                    preferences[THEME_KEY] ?: SpeedometerTheme.DARK.name
                }.first()

                val theme = runCatching { SpeedometerTheme.valueOf(savedThemeName) }.getOrDefault(SpeedometerTheme.DARK)
                _themeState.value = theme
                SpeedRepository.updateSettings(themeMode = theme.name)
                Timber.tag("SpeedometerTheme").i("Tema inicializado do DataStore: %s", theme)
            } catch (e: Exception) {
                Timber.tag("SpeedometerTheme").e(e, "Erro ao carregar tema do DataStore")
            }
        }
    }

    fun toggleTheme(context: Context) {
        val nextTheme = if (_themeState.value == SpeedometerTheme.DARK) {
            SpeedometerTheme.LIGHT
        } else {
            SpeedometerTheme.DARK
        }
        setTheme(context, nextTheme)
    }

    fun setTheme(context: Context, theme: SpeedometerTheme) {
        _themeState.value = theme
        SpeedRepository.updateSettings(themeMode = theme.name)
        repositoryScope.launch {
            try {
                context.themeDataStore.edit { preferences ->
                    preferences[THEME_KEY] = theme.name
                }
                Timber.tag("SpeedometerTheme").i("Tema salvo no DataStore: %s", theme.name)
            } catch (e: Exception) {
                Timber.tag("SpeedometerTheme").e(e, "Erro ao salvar tema no DataStore")
            }
        }
    }
}
