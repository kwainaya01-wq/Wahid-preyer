package com.example.calculation

enum class CalculationMethod(
    val title: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null,
    val maghribAngle: Double? = null
) {
    MUSLIM_WORLD_LEAGUE(
        title = "Muslim World League",
        description = "Standard method used widely in Europe, Far East, and parts of the US. Fajr: 18°, Isha: 17°.",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    EGYPTIAN(
        title = "Egyptian General Authority of Survey",
        description = "Widely used in Egypt, Africa, Syria, Lebanon, and parts of the Middle East. Fajr: 19.5°, Isha: 17.5°.",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    KARACHI(
        title = "University of Islamic Sciences, Karachi",
        description = "Used in Pakistan, Bangladesh, India, Afghanistan, and parts of Europe. Fajr: 18°, Isha: 18°.",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    UMM_AL_QURA(
        title = "Umm Al-Qura University, Makkah",
        description = "Official calendar of Saudi Arabia and Arabian Peninsula. Fajr: 18.5°, Isha: 90 min after Maghrib.",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    DUBAI(
        title = "Dubai (UAE Islamic Affairs)",
        description = "Used in the United Arab Emirates. Fajr: 18.2°, Isha: 18.2°.",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    MOONSIGHTING_COMMITTEE(
        title = "Moonsighting Committee Worldwide",
        description = "Fajr: 18°, Isha: 18°, with latitude-based twilight adjustments.",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    NORTH_AMERICA(
        title = "Islamic Society of North America (ISNA)",
        description = "Commonly used in USA and Canada. Fajr: 15°, Isha: 15°.",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    KUWAIT(
        title = "Kuwait",
        description = "Used in Kuwait. Fajr: 18°, Isha: 17.5°.",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    QATAR(
        title = "Qatar",
        description = "Used in Qatar. Fajr: 18°, Isha: 90 min after Maghrib.",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    SINGAPORE(
        title = "MUIS (Singapore)",
        description = "Majlis Ugama Islam Singapura. Fajr: 20°, Isha: 18°.",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),
    TEHRAN(
        title = "Institute of Geophysics, University of Tehran",
        description = "Fajr: 17.7°, Maghrib: 4.5°, Isha: 14°.",
        fajrAngle = 17.7,
        ishaAngle = 14.0,
        maghribAngle = 4.5
    );

    companion object {
        val defaultMethod = MUSLIM_WORLD_LEAGUE
    }
}
