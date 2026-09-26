package com.example.calculation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerCalculator {

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    data class PrayerAdjustment(
        val fajrMinutes: Int = 0,
        val sunriseMinutes: Int = 0,
        val dhuhrMinutes: Int = 0,
        val asrMinutes: Int = 0,
        val sunsetMinutes: Int = 0,
        val maghribMinutes: Int = 0,
        val ishaMinutes: Int = 0
    )

    /**
     * Calculates prayer times dynamically for a given location, date, timezone, and methodology.
     * All calculations are 100% offline using astronomical equations.
     */
    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zoneId: ZoneId,
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        juristicMethod: JuristicMethod = JuristicMethod.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDNIGHT,
        adjustment: PrayerAdjustment = PrayerAdjustment()
    ): PrayerTimes {
        val timezoneOffsetHours = zoneId.rules.getOffset(date.atStartOfDay()).totalSeconds / 3600.0

        // 1. Julian Day at 0h UT
        val jd = julianDay(date.year, date.monthValue, date.dayOfMonth)

        // 2. Solar coordinates at midday
        val solar = calculateSunCoordinates(jd, longitude, timezoneOffsetHours)

        // 3. Solar Transit (Dhuhr)
        val dhuhrRaw = 12.0 + timezoneOffsetHours - (longitude / 15.0) - (solar.equationOfTime / 60.0)

        // 4. Sunrise and Sunset (altitude = -0.8333°)
        val sunriseAngle = -0.8333
        val sunriseHalfDay = hourAngle(sunriseAngle, latitude, solar.declination)
        val sunriseRaw = if (sunriseHalfDay != null) dhuhrRaw - (sunriseHalfDay / 15.0) else dhuhrRaw - 6.0
        val sunsetRaw = if (sunriseHalfDay != null) dhuhrRaw + (sunriseHalfDay / 15.0) else dhuhrRaw + 6.0

        // 5. Asr calculation
        val asrAltitude = calculateAsrAltitude(latitude, solar.declination, juristicMethod.shadowRatio)
        val asrHalfDay = hourAngle(asrAltitude, latitude, solar.declination)
        val asrRaw = if (asrHalfDay != null) dhuhrRaw + (asrHalfDay / 15.0) else dhuhrRaw + 3.2

        // 6. Fajr calculation
        var fajrHalfDay = hourAngle(-method.fajrAngle, latitude, solar.declination)
        var fajrRaw = if (fajrHalfDay != null) {
            dhuhrRaw - (fajrHalfDay / 15.0)
        } else {
            // Extreme latitude twilight adjustment
            adjustTwilight(sunriseRaw, sunsetRaw, method.fajrAngle, highLatitudeRule, isMorning = true)
        }

        // Apply high latitude rule check if twilight night interval is abnormal
        val nightDuration = (24.0 - sunsetRaw + sunriseRaw).let { if (it < 0) it + 24.0 else it }
        if (highLatitudeRule != HighLatitudeRule.NONE && fajrHalfDay != null) {
            val fajrToSunrise = sunriseRaw - fajrRaw
            val maxTwilight = when (highLatitudeRule) {
                HighLatitudeRule.MIDNIGHT -> nightDuration / 2.0
                HighLatitudeRule.ONE_SEVENTH -> nightDuration / 7.0
                HighLatitudeRule.ANGLE_BASED -> nightDuration * (method.fajrAngle / 60.0)
                HighLatitudeRule.NONE -> nightDuration
            }
            if (fajrToSunrise > maxTwilight) {
                fajrRaw = sunriseRaw - maxTwilight
            }
        }

        // 7. Maghrib calculation
        val maghribRaw = if (method.maghribAngle != null) {
            val maghribHalfDay = hourAngle(-method.maghribAngle, latitude, solar.declination)
            if (maghribHalfDay != null) dhuhrRaw + (maghribHalfDay / 15.0) else sunsetRaw
        } else {
            sunsetRaw
        }

        // 8. Isha calculation
        var ishaRaw: Double
        if (method.ishaIntervalMinutes != null) {
            ishaRaw = maghribRaw + (method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaHalfDay = hourAngle(-method.ishaAngle, latitude, solar.declination)
            ishaRaw = if (ishaHalfDay != null) {
                dhuhrRaw + (ishaHalfDay / 15.0)
            } else {
                adjustTwilight(sunriseRaw, sunsetRaw, method.ishaAngle, highLatitudeRule, isMorning = false)
            }

            if (highLatitudeRule != HighLatitudeRule.NONE && ishaHalfDay != null) {
                val sunsetToIsha = ishaRaw - sunsetRaw
                val maxTwilight = when (highLatitudeRule) {
                    HighLatitudeRule.MIDNIGHT -> nightDuration / 2.0
                    HighLatitudeRule.ONE_SEVENTH -> nightDuration / 7.0
                    HighLatitudeRule.ANGLE_BASED -> nightDuration * (method.ishaAngle / 60.0)
                    HighLatitudeRule.NONE -> nightDuration
                }
                if (sunsetToIsha > maxTwilight) {
                    ishaRaw = sunsetRaw + maxTwilight
                }
            }
        }

        return PrayerTimes(
            date = date,
            zoneId = zoneId,
            fajr = doubleToLocalTime(fajrRaw, adjustment.fajrMinutes),
            sunrise = doubleToLocalTime(sunriseRaw, adjustment.sunriseMinutes),
            dhuhr = doubleToLocalTime(dhuhrRaw, adjustment.dhuhrMinutes),
            asr = doubleToLocalTime(asrRaw, adjustment.asrMinutes),
            sunset = doubleToLocalTime(sunsetRaw, adjustment.sunsetMinutes),
            maghrib = doubleToLocalTime(maghribRaw, adjustment.maghribMinutes),
            isha = doubleToLocalTime(ishaRaw, adjustment.ishaMinutes)
        )
    }

    private data class SolarCoordinates(
        val declination: Double,
        val equationOfTime: Double
    )

    private fun calculateSunCoordinates(jd: Double, longitude: Double, timezone: Double): SolarCoordinates {
        val d = jd - 2451545.0 + (12.0 - longitude / 15.0) / 24.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g * DEG_TO_RAD) + 0.020 * sin(2.0 * g * DEG_TO_RAD))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(atan2(cos(e * DEG_TO_RAD) * sin(l * DEG_TO_RAD), cos(l * DEG_TO_RAD)) * RAD_TO_DEG) / 15.0
        val declination = asin(sin(e * DEG_TO_RAD) * sin(l * DEG_TO_RAD)) * RAD_TO_DEG
        val equationOfTime = (q / 15.0 - fixHour(ra)) * 60.0

        return SolarCoordinates(declination, equationOfTime)
    }

    private fun hourAngle(altitudeAngle: Double, latitude: Double, declination: Double): Double? {
        val latRad = latitude * DEG_TO_RAD
        val decRad = declination * DEG_TO_RAD
        val altRad = altitudeAngle * DEG_TO_RAD

        val cosOmega = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        return if (cosOmega in -1.0..1.0) {
            acos(cosOmega) * RAD_TO_DEG
        } else {
            null
        }
    }

    private fun calculateAsrAltitude(latitude: Double, declination: Double, shadowRatio: Double): Double {
        val latDecDiff = abs(latitude - declination)
        val tanAlt = 1.0 / (shadowRatio + tan(latDecDiff * DEG_TO_RAD))
        return atan(tanAlt) * RAD_TO_DEG
    }

    private fun adjustTwilight(
        sunriseRaw: Double,
        sunsetRaw: Double,
        angle: Double,
        rule: HighLatitudeRule,
        isMorning: Boolean
    ): Double {
        val nightDuration = (24.0 - sunsetRaw + sunriseRaw).let { if (it < 0) it + 24.0 else it }
        val portion = when (rule) {
            HighLatitudeRule.MIDNIGHT -> nightDuration / 2.0
            HighLatitudeRule.ONE_SEVENTH -> nightDuration / 7.0
            HighLatitudeRule.ANGLE_BASED -> nightDuration * (angle / 60.0)
            HighLatitudeRule.NONE -> nightDuration / 6.0
        }
        return if (isMorning) sunriseRaw - portion else sunsetRaw + portion
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun doubleToLocalTime(decimalHours: Double, adjustmentMinutes: Int): LocalTime {
        var totalMinutes = (decimalHours * 60.0).toInt() + adjustmentMinutes
        totalMinutes = (totalMinutes % (24 * 60) + 24 * 60) % (24 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return LocalTime.of(hours, minutes, 0)
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }
}
