package com.example.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R

class AzanPlaybackService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "AzanPlaybackService"
        const val ACTION_START_AZAN = "com.example.wahidprayer.ACTION_START_AZAN"
        const val ACTION_STOP_AZAN = "com.example.wahidprayer.ACTION_STOP_AZAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_ARABIC = "extra_prayer_arabic"
        const val EXTRA_IS_FAJR = "extra_is_fajr"
        const val EXTRA_CUSTOM_URI = "extra_custom_uri"
        const val FOREGROUND_NOTIFICATION_ID = 2001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WahidPrayer:AzanPlaybackWakeLock"
        ).apply {
            acquire(5 * 60 * 1000L) // 5 minutes max timeout
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_AZAN) {
            stopAzanAndService()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerArabic = intent?.getStringExtra(EXTRA_PRAYER_ARABIC) ?: ""
        val isFajr = intent?.getBooleanExtra(EXTRA_IS_FAJR, false) ?: false
        val customUri = intent?.getStringExtra(EXTRA_CUSTOM_URI)

        startForegroundNotification(prayerName, prayerArabic)

        // Play Azan using the central AzanPlayer method
        val result = AzanPlayer.playAzan(
            context = this,
            customUri = customUri,
            isFajr = isFajr,
            onCompletion = {
                stopAzanAndService()
            }
        )

        if (result is AzanPlayResult.Error) {
            Log.e(TAG, "Scheduled Azan playback error: ${result.message}")
            showMissingAudioNotification(prayerName, result.message)
            stopAzanAndService()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundNotification(prayerName: String, prayerArabic: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AzanPlaybackService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        val pendingStop = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_PRAYER_TIMES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🕌 Azan: $prayerName Salah ($prayerArabic)")
            .setContentText("Playing Azan. Tap to open or stop below.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_media_pause, "Stop Azan", pendingStop)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun showMissingAudioNotification(prayerName: String, errorMessage: String) {
        NotificationHelper.showMissingAudioNotification(this, prayerName, errorMessage)
    }

    private fun stopAzanAndService() {
        AzanPlayer.stop()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        AzanPlayer.stop()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
