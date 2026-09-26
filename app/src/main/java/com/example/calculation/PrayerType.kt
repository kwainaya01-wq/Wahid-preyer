package com.example.calculation

enum class PrayerType(
    val englishName: String,
    val arabicName: String,
    val hasAzan: Boolean,
    val iconEmoji: String
) {
    FAJR("Fajr", "الفجر", true, "🌅"),
    SUNRISE("Sunrise", "الشروق", false, "☀️"),
    DHUHR("Dhuhr", "الظهر", true, "☀️"),
    ASR("Asr", "العصر", true, "🌤️"),
    SUNSET("Sunset", "الغروب", false, "🌇"),
    MAGHRIB("Maghrib", "المغرب", true, "🕌"),
    ISHA("Isha", "العشاء", true, "🌙");

    companion object {
        val azanPrayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
        val mainSchedule = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA)
    }
}
