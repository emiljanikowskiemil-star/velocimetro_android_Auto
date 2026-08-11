package com.aatorque.speedometer.gps

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aatorque.speedometer.App
import com.aatorque.speedometer.R
import com.aatorque.speedometer.speedometer.MainActivity
import timber.log.Timber

class GpsForegroundService : Service() {

    private lateinit var gpsSpeedManager: GpsSpeedManager

    override fun onCreate() {
        super.onCreate()
        Timber.tag("SpeedometerGPS").i("GpsForegroundService criado")
        gpsSpeedManager = GpsSpeedManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("SpeedometerGPS").i("GpsForegroundService iniciado (onStartCommand)")
        startForeground(App.NOTIFICATION_ID, buildNotification())
        gpsSpeedManager.startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {

        Timber.tag("SpeedometerGPS").i("GpsForegroundService destruído")
        gpsSpeedManager.stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Velocímetro GPS Ativo")
            .setContentText("Calculando velocidade em tempo real para Android e Android Auto")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, GpsForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GpsForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
