package com.aatorque.speedometer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aatorque.speedometer.theme.ThemeRepository
import com.aatorque.speedometer.trip.TripManager
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        ThemeRepository.init(this)
        TripManager.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Velocímetro GPS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a leitura do GPS ativa para o Velocímetro Android e Android Auto"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "gps_speedometer_service"
        const val NOTIFICATION_ID = 1001
    }
}
