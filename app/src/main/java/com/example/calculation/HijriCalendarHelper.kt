package com.example.calculation

import java.time.LocalDate
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameEnglish: String,
    val monthNameArabic: String,
    val isRamadan: Boolean
) {
    fun formattedEnglish(): String {
        return "$day $monthNameEnglish $year AH"
    }

    fun formattedArabic(): String {
        return "$day $monthNameArabic $year هـ"
    }
}

object HijriCalendarHelper {

    private val MONTH_NAMES_ENGLISH = arrayOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    private val MONTH_NAMES_ARABIC = arrayOf(
        "محرّم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوّال",
        "ذو القعدة",
        "ذو الحجة"
    )

    /**
     * Converts a Gregorian LocalDate to an Islamic Hijri date with configurable user adjustment.
     */
    fun getHijriDate(date: LocalDate, adjustmentDays: Int = 0): HijriDate {
        val adjustedDate = date.plusDays(adjustmentDays.toLong())
        val jd = gregorianToJulianDay(adjustedDate.year, adjustedDate.monthValue, adjustedDate.dayOfMonth)
        return julianDayToHijri(jd)
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Double {
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

    private fun julianDayToHijri(jd: Double): HijriDate {
        val jdFloor = floor(jd) + 0.5
        val z = jdFloor - 1948440.0 // Epoch of Islamic calendar (July 16, 622 CE)
        val cyc = floor(z / 10631.0)
        val zCyc = z - cyc * 10631.0
        val j = floor((zCyc - 0.1335) / 354.366)
        val zYear = zCyc - floor(j * 354.366 + 0.1335)
        val m = floor((zYear + 0.85) / 29.5)
        val d = floor(zYear - floor(m * 29.5) + 1.0).toInt()

        val hijriYear = (cyc * 30.0 + j + 1.0).toInt()
        val hijriMonth = (m + 1.0).toInt().coerceIn(1, 12)
        val hijriDay = d.coerceIn(1, 30)

        val monthIdx = hijriMonth - 1

        return HijriDate(
            day = hijriDay,
            month = hijriMonth,
            year = hijriYear,
            monthNameEnglish = MONTH_NAMES_ENGLISH[monthIdx],
            monthNameArabic = MONTH_NAMES_ARABIC[monthIdx],
            isRamadan = (hijriMonth == 9)
        )
    }
}
