package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.audio.AzanPlaybackService
import com.example.audio.AzanPlayer
import com.example.audio.NotificationHelper
import com.example.calculation.PrayerType
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WahidPrayer:PrayerAlarmWakeLock"
        ).apply {
            acquire(60 * 1000L) // 1 minute safety hold
        }

        try {
            when (intent?.action) {
                PrayerAlarmScheduler.ACTION_PRAYER_ALARM -> {
                    handlePrayerAlarm(context, intent)
                }
                PrayerAlarmScheduler.ACTION_PRE_REMINDER -> {
                    handlePreReminder(context, intent)
                }
                PrayerAlarmScheduler.ACTION_MIDNIGHT_UPDATE -> {
                    PrayerAlarmScheduler.scheduleAll(context)
                }
                PrayerAlarmScheduler.ACTION_TEST_ALARM -> {
                    handleTestAlarm(context)
                }
            }
        } finally {
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    private fun handlePrayerAlarm(context: Context, intent: Intent) {
        val prayerTypeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) ?: return
        val timeString = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME_STR) ?: ""
        val prayerType = try {
            PrayerType.valueOf(prayerTypeName)
        } catch (_: Exception) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val repo = SettingsRepository(context)
            val settings = repo.getSettingsOnce()

            val isAzanAllowed = settings.isAzanEnabledFor(prayerType)
            val isFajr = (prayerType == PrayerType.FAJR)

            val willPlayAudio = isAzanAllowed && settings.azanTone != "none"

            // Show notification
            NotificationHelper.showPrayerNotification(
                context = context,
                prayerType = prayerType,
                timeString = timeString,
                isPlayingAzan = willPlayAudio
            )

            // Start Azan playback service if audio enabled
            if (willPlayAudio) {
                val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
                    action = AzanPlaybackService.ACTION_START_AZAN
                    putExtra(AzanPlaybackService.EXTRA_PRAYER_NAME, prayerType.englishName)
                    putExtra(AzanPlaybackService.EXTRA_PRAYER_ARABIC, prayerType.arabicName)
                    putExtra(AzanPlaybackService.EXTRA_IS_FAJR, isFajr)
                    if (settings.azanTone == "custom") {
                        putExtra(AzanPlaybackService.EXTRA_CUSTOM_URI, settings.customAzanUri)
                    }
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    private fun handlePreReminder(context: Context, intent: Intent) {
        val prayerTypeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) ?: return
        val minutesRemaining = intent.getIntExtra("reminder_minutes", 10)
        val prayerType = try {
            PrayerType.valueOf(prayerTypeName)
        } catch (_: Exception) {
            return
        }

        NotificationHelper.showReminderNotification(context, prayerType, minutesRemaining)
        AzanPlayer.playChime(context)
    }

    private fun handleTestAlarm(context: Context) {
        NotificationHelper.showPrayerNotification(
            context = context,
            prayerType = PrayerType.MAGHRIB,
            timeString = "Test Prayer",
            isPlayingAzan = true
        )

        val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_START_AZAN
            putExtra(AzanPlaybackService.EXTRA_PRAYER_NAME, "Maghrib (Test)")
            putExtra(AzanPlaybackService.EXTRA_PRAYER_ARABIC, "المغرب")
            putExtra(AzanPlaybackService.EXTRA_IS_FAJR, false)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
