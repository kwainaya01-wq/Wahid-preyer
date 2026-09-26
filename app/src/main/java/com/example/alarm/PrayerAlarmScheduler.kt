package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.calculation.PrayerCalculator
import com.example.calculation.PrayerTimes
import com.example.calculation.PrayerType
import com.example.data.AppSettings
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

object PrayerAlarmScheduler {

    private const val TAG = "PrayerAlarmScheduler"

    const val ACTION_PRAYER_ALARM = "com.example.wahidprayer.ACTION_PRAYER_ALARM"
    const val ACTION_PRE_REMINDER = "com.example.wahidprayer.ACTION_PRE_REMINDER"
    const val ACTION_MIDNIGHT_UPDATE = "com.example.wahidprayer.ACTION_MIDNIGHT_UPDATE"
    const val ACTION_TEST_ALARM = "com.example.wahidprayer.ACTION_TEST_ALARM"

    const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
    const val EXTRA_PRAYER_TIME_STR = "extra_prayer_time_str"

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val settings = repo.getSettingsOnce()
                scheduleWithSettings(context, settings)
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarms", e)
            }
        }
    }

    fun scheduleWithSettings(context: Context, settings: AppSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val zoneId = settings.userLocation.zoneId
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        // Cancel previous alarms to prevent duplicates
        cancelAll(context)

        // Calculate today and tomorrow
        val todayTimes = PrayerCalculator.calculate(
            latitude = settings.userLocation.latitude,
            longitude = settings.userLocation.longitude,
            date = today,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )

        val tomorrowTimes = PrayerCalculator.calculate(
            latitude = settings.userLocation.latitude,
            longitude = settings.userLocation.longitude,
            date = tomorrow,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )

        // Schedule today's upcoming prayers
        scheduleDayPrayers(context, alarmManager, todayTimes, today, now, settings, dayOffset = 0)

        // Schedule tomorrow's prayers (ensuring 24h continuity)
        scheduleDayPrayers(context, alarmManager, tomorrowTimes, tomorrow, now, settings, dayOffset = 1)

        // Schedule midnight refresh alarm
        scheduleMidnightAlarm(context, alarmManager, today, zoneId)
    }

    private fun scheduleDayPrayers(
        context: Context,
        alarmManager: AlarmManager,
        times: PrayerTimes,
        date: LocalDate,
        now: ZonedDateTime,
        settings: AppSettings,
        dayOffset: Int
    ) {
        for (prayer in PrayerType.azanPrayers) {
            val prayerTime = times.getZonedDateTime(prayer, date)
            val timeMillis = prayerTime.toInstant().toEpochMilli()
            val requestCode = 1000 * dayOffset + getPrayerRequestCode(prayer)

            // 1. Prayer Salah Time Alarm
            if (prayerTime.isAfter(now)) {
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_ALARM
                    putExtra(EXTRA_PRAYER_TYPE, prayer.name)
                    putExtra(EXTRA_PRAYER_TIME_STR, times.formattedTime(prayer, settings.is24HourFormat))
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setAlarmSafely(context, alarmManager, timeMillis, pendingIntent)
            }

            // 2. Pre-Prayer Reminder Alarm
            if (settings.preReminderEnabled && settings.reminderMinutesBefore > 0) {
                val reminderTime = prayerTime.minusMinutes(settings.reminderMinutesBefore.toLong())
                if (reminderTime.isAfter(now)) {
                    val reminderMillis = reminderTime.toInstant().toEpochMilli()
                    val reminderRequestCode = 2000 * (dayOffset + 1) + getPrayerRequestCode(prayer)

                    val reminderIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = ACTION_PRE_REMINDER
                        putExtra(EXTRA_PRAYER_TYPE, prayer.name)
                        putExtra("reminder_minutes", settings.reminderMinutesBefore)
                    }
                    val reminderPendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminderRequestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setAlarmSafely(context, alarmManager, reminderMillis, reminderPendingIntent)
                }
            }
        }
    }

    private fun scheduleMidnightAlarm(
        context: Context,
        alarmManager: AlarmManager,
        today: LocalDate,
        zoneId: java.time.ZoneId
    ) {
        val midnight = today.plusDays(1).atStartOfDay(zoneId).plusMinutes(1)
        val midnightMillis = midnight.toInstant().toEpochMilli()

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(context, alarmManager, midnightMillis, pendingIntent)
    }

    fun scheduleTestAlarm(context: Context, delayMinutes: Int = 1) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TEST_ALARM
            putExtra(EXTRA_PRAYER_TYPE, PrayerType.MAGHRIB.name)
            putExtra(EXTRA_PRAYER_TIME_STR, "Test Alarm")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(context, alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelTestAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TEST_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (dayOffset in 0..1) {
            for (prayer in PrayerType.azanPrayers) {
                val prayerCode = 1000 * dayOffset + getPrayerRequestCode(prayer)
                val prayerIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRAYER_ALARM
                }
                alarmManager.cancel(
                    PendingIntent.getBroadcast(
                        context,
                        prayerCode,
                        prayerIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                val reminderCode = 2000 * (dayOffset + 1) + getPrayerRequestCode(prayer)
                val reminderIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_PRE_REMINDER
                }
                alarmManager.cancel(
                    PendingIntent.getBroadcast(
                        context,
                        reminderCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }

    private fun setAlarmSafely(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            val showIntent = Intent(context, MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                0,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // AlarmClockInfo gives highest priority wakeups on modern Android
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (_: SecurityException) {
            // If exact alarm permission not allowed, use setAndAllowWhileIdle
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm clock", e)
        }
    }

    private fun getPrayerRequestCode(prayer: PrayerType): Int {
        return when (prayer) {
            PrayerType.FAJR -> 1
            PrayerType.SUNRISE -> 2
            PrayerType.DHUHR -> 3
            PrayerType.ASR -> 4
            PrayerType.SUNSET -> 5
            PrayerType.MAGHRIB -> 6
            PrayerType.ISHA -> 7
        }
    }
}
