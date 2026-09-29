package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
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

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
    }

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
            try {
                val repo = SettingsRepository(context)
                val settings = repo.getSettingsOnce()

                val isAzanAllowed = settings.isAzanEnabledFor(prayerType)
                val isFajr = (prayerType == PrayerType.FAJR)
                val willPlayAudio = isAzanAllowed && settings.azanTone != "none"

                if (willPlayAudio) {
                    // Show notification with Stop Azan action
                    NotificationHelper.showPrayerNotification(
                        context = context,
                        prayerType = prayerType,
                        timeString = timeString,
                        isPlayingAzan = true
                    )

                    // Start background Azan playback service to play selected or bundled Azan audio
                    val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
                        action = AzanPlaybackService.ACTION_START_AZAN
                        putExtra(AzanPlaybackService.EXTRA_PRAYER_NAME, prayerType.englishName)
                        putExtra(AzanPlaybackService.EXTRA_PRAYER_ARABIC, prayerType.arabicName)
                        putExtra(AzanPlaybackService.EXTRA_IS_FAJR, isFajr)
                        putExtra(AzanPlaybackService.EXTRA_CUSTOM_URI, settings.customAzanUri)
                    }
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start AzanPlaybackService", e)
                    }
                } else {
                    // Audio muted or disabled: show notification only
                    NotificationHelper.showPrayerNotification(
                        context = context,
                        prayerType = prayerType,
                        timeString = timeString,
                        isPlayingAzan = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling prayer alarm", e)
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
        // Pre-prayer reminder plays gentle 2-second chime
        AzanPlayer.playChime(context)
    }

    private fun handleTestAlarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val settings = repo.getSettingsOnce()

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
                    putExtra(AzanPlaybackService.EXTRA_CUSTOM_URI, settings.customAzanUri)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start test AzanPlaybackService", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling test alarm", e)
            }
        }
    }
}
