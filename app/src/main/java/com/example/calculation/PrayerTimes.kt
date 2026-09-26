package com.example.calculation

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class PrayerTimes(
    val date: LocalDate,
    val zoneId: ZoneId,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val sunset: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime
) {
    fun getTime(prayerType: PrayerType): LocalTime {
        return when (prayerType) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> sunrise
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.SUNSET -> sunset
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
    }

    fun getZonedDateTime(prayerType: PrayerType, targetDate: LocalDate = date): ZonedDateTime {
        return ZonedDateTime.of(targetDate, getTime(prayerType), zoneId)
    }

    /**
     * Determines which prayer is the next upcoming one, or tomorrow's Fajr if Isha has passed.
     */
    fun getNextPrayer(now: ZonedDateTime, tomorrowTimes: PrayerTimes? = null): NextPrayerInfo {
        val nowLocalTime = now.withZoneSameInstant(zoneId).toLocalTime()
        val nowDate = now.withZoneSameInstant(zoneId).toLocalDate()

        if (nowDate.isBefore(date)) {
            // Looking at a future day
            return NextPrayerInfo(
                prayerType = PrayerType.FAJR,
                dateTime = getZonedDateTime(PrayerType.FAJR),
                durationRemaining = Duration.between(now, getZonedDateTime(PrayerType.FAJR))
            )
        }

        // Ordered prayer check
        val ordered = listOf(
            PrayerType.FAJR to fajr,
            PrayerType.SUNRISE to sunrise,
            PrayerType.DHUHR to dhuhr,
            PrayerType.ASR to asr,
            PrayerType.MAGHRIB to maghrib,
            PrayerType.ISHA to isha
        )

        for ((type, time) in ordered) {
            if (nowLocalTime.isBefore(time)) {
                val prayerDateTime = ZonedDateTime.of(date, time, zoneId)
                return NextPrayerInfo(
                    prayerType = type,
                    dateTime = prayerDateTime,
                    durationRemaining = Duration.between(now, prayerDateTime)
                )
            }
        }

        // Past Isha: Next prayer is tomorrow's Fajr
        val tomorrowFajr = tomorrowTimes?.fajr ?: fajr
        val tomorrowFajrDateTime = ZonedDateTime.of(date.plusDays(1), tomorrowFajr, zoneId)
        return NextPrayerInfo(
            prayerType = PrayerType.FAJR,
            dateTime = tomorrowFajrDateTime,
            durationRemaining = Duration.between(now, tomorrowFajrDateTime),
            isTomorrow = true
        )
    }

    /**
     * Determines current active prayer period.
     */
    fun getCurrentPrayer(now: ZonedDateTime): PrayerType {
        val nowLocalTime = now.withZoneSameInstant(zoneId).toLocalTime()
        return when {
            nowLocalTime.isBefore(fajr) -> PrayerType.ISHA // Before Fajr is night/Isha
            nowLocalTime.isBefore(sunrise) -> PrayerType.FAJR
            nowLocalTime.isBefore(dhuhr) -> PrayerType.SUNRISE // Duha period
            nowLocalTime.isBefore(asr) -> PrayerType.DHUHR
            nowLocalTime.isBefore(maghrib) -> PrayerType.ASR
            nowLocalTime.isBefore(isha) -> PrayerType.MAGHRIB
            else -> PrayerType.ISHA
        }
    }

    fun formattedTime(prayerType: PrayerType, is24Hour: Boolean = false): String {
        val time = getTime(prayerType)
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }
}

data class NextPrayerInfo(
    val prayerType: PrayerType,
    val dateTime: ZonedDateTime,
    val durationRemaining: Duration,
    val isTomorrow: Boolean = false
) {
    fun formattedCountdown(): String {
        val seconds = durationRemaining.seconds.coerceAtLeast(0)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}
