package com.example.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.calculation.PrayerType

object NotificationHelper {

    const val CHANNEL_PRAYER_TIMES = "wahid_prayer_times_channel"
    const val CHANNEL_REMINDERS = "wahid_prayer_reminders_channel"
    const val NOTIFICATION_ID_PRAYER = 1001
    const val NOTIFICATION_ID_REMINDER = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prayerSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.azan_default}")
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. Prayer Times Channel (High Importance)
            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER_TIMES,
                "Prayer Times & Azan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications for Islamic prayer times (Salah)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(prayerSoundUri, audioAttributes)
            }

            // 2. Reminder Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Pre-Prayer Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle advance reminders before prayer time"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(prayerChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showPrayerNotification(
        context: Context,
        prayerType: PrayerType,
        timeString: String,
        isPlayingAzan: Boolean = false
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PRAYER_TIMES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${prayerType.iconEmoji} ${prayerType.englishName} Salah (${prayerType.arabicName})")
            .setContentText("It is time for ${prayerType.englishName} Salah ($timeString).")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "It is time for ${prayerType.englishName} Salah ($timeString).\n\"Verily, Salah has been enjoined on the believers at fixed times.\""
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)

        if (isPlayingAzan) {
            val stopAzanIntent = Intent(context, AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_STOP_AZAN
            }
            val pendingStop = PendingIntent.getService(
                context,
                1,
                stopAzanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Stop Azan", pendingStop)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PRAYER, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission not yet granted
        }
    }

    fun showReminderNotification(
        context: Context,
        prayerType: PrayerType,
        minutesRemaining: Int
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ ${prayerType.englishName} Prayer in $minutesRemaining minutes")
            .setContentText("Prepare for ${prayerType.englishName} Salah (${prayerType.arabicName}).")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission not yet granted
        }
    }
}
