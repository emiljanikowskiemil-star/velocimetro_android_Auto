package com.aatorque.speedometer.speedometer

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.aatorque.speedometer.R
import com.aatorque.speedometer.gps.SpeedRepository

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.title_activity_settings)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val resetTripPref = findPreference<Preference>("pref_reset_trip")
            resetTripPref?.setOnPreferenceClickListener {
                SpeedRepository.resetTrip()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null || key == null) return

            when (key) {
                "pref_max_speed" -> {
                    val maxSpeed = sharedPreferences.getString(key, "220")?.toIntOrNull() ?: 220
                    SpeedRepository.updateSettings(maxSpeedLimit = maxSpeed)
                }
                "pref_smoothing" -> {
                    val smoothing = sharedPreferences.getString(key, "MEDIUM") ?: "MEDIUM"
                    SpeedRepository.updateSettings(smoothingLevel = smoothing)
                }
                "pref_theme" -> {
                    val themeStr = sharedPreferences.getString(key, "DARK") ?: "DARK"
                    val speedometerTheme = if (themeStr.equals("LIGHT", ignoreCase = true)) com.aatorque.speedometer.theme.SpeedometerTheme.LIGHT else com.aatorque.speedometer.theme.SpeedometerTheme.DARK
                    com.aatorque.speedometer.theme.ThemeRepository.setTheme(requireContext().applicationContext, speedometerTheme)
                }
                "pref_unit" -> {
                    val unit = sharedPreferences.getString(key, "km/h") ?: "km/h"
                    SpeedRepository.updateSettings(unit = unit)
                }
                "pref_keep_screen_on" -> {
                    val keepOn = sharedPreferences.getBoolean(key, true)
                    SpeedRepository.updateSettings(keepScreenOn = keepOn)
                }
                "pref_show_accuracy" -> {
                    val showAcc = sharedPreferences.getBoolean(key, true)
                    SpeedRepository.updateSettings(showAccuracy = showAcc)
                }
                "pref_show_satellites" -> {
                    val showSat = sharedPreferences.getBoolean(key, true)
                    SpeedRepository.updateSettings(showSatellites = showSat)
                }
                "pref_show_altitude" -> {
                    val showAlt = sharedPreferences.getBoolean(key, true)
                    SpeedRepository.updateSettings(showAltitude = showAlt)
                }
                "pref_show_trip" -> {
                    val showTrip = sharedPreferences.getBoolean(key, true)
                    SpeedRepository.updateSettings(showTrip = showTrip)
                }
            }
        }
    }
}
