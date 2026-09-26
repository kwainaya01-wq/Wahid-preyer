package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calculation.CalculationMethod
import com.example.calculation.HighLatitudeRule
import com.example.calculation.JuristicMethod
import com.example.calculation.PrayerCalculator
import com.example.calculation.PrayerType
import com.example.location.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wahid_prayer_settings")

data class AppSettings(
    val userLocation: UserLocation = UserLocation.DEFAULT_MAKKAH,
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val juristicMethod: JuristicMethod = JuristicMethod.SHAFI,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDNIGHT,
    val masterAzanEnabled: Boolean = true,
    val fajrAzanEnabled: Boolean = true,
    val dhuhrAzanEnabled: Boolean = true,
    val asrAzanEnabled: Boolean = true,
    val maghribAzanEnabled: Boolean = true,
    val ishaAzanEnabled: Boolean = true,
    val azanTone: String = "custom", // "custom", "none"
    val customAzanUri: String? = null,
    val customAzanFileName: String? = null,
    val preReminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 10,
    val hijriAdjustment: Int = 0,
    val themeMode: String = "system", // "system", "light", "dark"
    val hasCompletedOnboarding: Boolean = false,
    val is24HourFormat: Boolean = false,
    val prayerAdjustment: PrayerCalculator.PrayerAdjustment = PrayerCalculator.PrayerAdjustment()
) {
    fun isAzanEnabledFor(prayer: PrayerType): Boolean {
        if (!masterAzanEnabled) return false
        return when (prayer) {
            PrayerType.FAJR -> fajrAzanEnabled
            PrayerType.DHUHR -> dhuhrAzanEnabled
            PrayerType.ASR -> asrAzanEnabled
            PrayerType.MAGHRIB -> maghribAzanEnabled
            PrayerType.ISHA -> ishaAzanEnabled
            else -> false
        }
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val COUNTRY_NAME = stringPreferencesKey("country_name")
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val IS_GPS = booleanPreferencesKey("is_gps")

        val CALC_METHOD = stringPreferencesKey("calc_method")
        val JURISTIC_METHOD = stringPreferencesKey("juristic_method")
        val HIGH_LAT_RULE = stringPreferencesKey("high_lat_rule")

        val MASTER_AZAN = booleanPreferencesKey("master_azan")
        val FAJR_AZAN = booleanPreferencesKey("fajr_azan")
        val DHUHR_AZAN = booleanPreferencesKey("dhuhr_azan")
        val ASR_AZAN = booleanPreferencesKey("asr_azan")
        val MAGHRIB_AZAN = booleanPreferencesKey("maghrib_azan")
        val ISHA_AZAN = booleanPreferencesKey("isha_azan")

        val AZAN_TONE = stringPreferencesKey("azan_tone")
        val CUSTOM_AZAN_URI = stringPreferencesKey("custom_azan_uri")
        val CUSTOM_AZAN_FILE_NAME = stringPreferencesKey("custom_azan_file_name")

        val PRE_REMINDER = booleanPreferencesKey("pre_reminder")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")

        val HIJRI_ADJUSTMENT = intPreferencesKey("hijri_adjustment")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")

        val FAJR_OFFSET = intPreferencesKey("fajr_offset")
        val SUNRISE_OFFSET = intPreferencesKey("sunrise_offset")
        val DHUHR_OFFSET = intPreferencesKey("dhuhr_offset")
        val ASR_OFFSET = intPreferencesKey("asr_offset")
        val SUNSET_OFFSET = intPreferencesKey("sunset_offset")
        val MAGHRIB_OFFSET = intPreferencesKey("maghrib_offset")
        val ISHA_OFFSET = intPreferencesKey("isha_offset")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { pref ->
        val lat = pref[Keys.LATITUDE] ?: UserLocation.DEFAULT_MAKKAH.latitude
        val lon = pref[Keys.LONGITUDE] ?: UserLocation.DEFAULT_MAKKAH.longitude
        val city = pref[Keys.CITY_NAME] ?: UserLocation.DEFAULT_MAKKAH.cityName
        val country = pref[Keys.COUNTRY_NAME] ?: UserLocation.DEFAULT_MAKKAH.countryName
        val tz = pref[Keys.TIMEZONE_ID] ?: UserLocation.DEFAULT_MAKKAH.timezoneId
        val isGps = pref[Keys.IS_GPS] ?: false

        val methodStr = pref[Keys.CALC_METHOD] ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.name
        val method = try {
            CalculationMethod.valueOf(methodStr)
        } catch (_: Exception) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE
        }

        val juristicStr = pref[Keys.JURISTIC_METHOD] ?: JuristicMethod.SHAFI.name
        val juristic = try {
            JuristicMethod.valueOf(juristicStr)
        } catch (_: Exception) {
            JuristicMethod.SHAFI
        }

        val highLatStr = pref[Keys.HIGH_LAT_RULE] ?: HighLatitudeRule.MIDNIGHT.name
        val highLat = try {
            HighLatitudeRule.valueOf(highLatStr)
        } catch (_: Exception) {
            HighLatitudeRule.MIDNIGHT
        }

        AppSettings(
            userLocation = UserLocation(lat, lon, city, country, tz, isGps),
            calculationMethod = method,
            juristicMethod = juristic,
            highLatitudeRule = highLat,
            masterAzanEnabled = pref[Keys.MASTER_AZAN] ?: true,
            fajrAzanEnabled = pref[Keys.FAJR_AZAN] ?: true,
            dhuhrAzanEnabled = pref[Keys.DHUHR_AZAN] ?: true,
            asrAzanEnabled = pref[Keys.ASR_AZAN] ?: true,
            maghribAzanEnabled = pref[Keys.MAGHRIB_AZAN] ?: true,
            ishaAzanEnabled = pref[Keys.ISHA_AZAN] ?: true,
            azanTone = pref[Keys.AZAN_TONE] ?: "custom",
            customAzanUri = pref[Keys.CUSTOM_AZAN_URI],
            customAzanFileName = pref[Keys.CUSTOM_AZAN_FILE_NAME],
            preReminderEnabled = pref[Keys.PRE_REMINDER] ?: true,
            reminderMinutesBefore = pref[Keys.REMINDER_MINUTES] ?: 10,
            hijriAdjustment = pref[Keys.HIJRI_ADJUSTMENT] ?: 0,
            themeMode = pref[Keys.THEME_MODE] ?: "system",
            hasCompletedOnboarding = pref[Keys.ONBOARDING_COMPLETED] ?: false,
            is24HourFormat = pref[Keys.IS_24_HOUR] ?: false,
            prayerAdjustment = PrayerCalculator.PrayerAdjustment(
                fajrMinutes = pref[Keys.FAJR_OFFSET] ?: 0,
                sunriseMinutes = pref[Keys.SUNRISE_OFFSET] ?: 0,
                dhuhrMinutes = pref[Keys.DHUHR_OFFSET] ?: 0,
                asrMinutes = pref[Keys.ASR_OFFSET] ?: 0,
                sunsetMinutes = pref[Keys.SUNSET_OFFSET] ?: 0,
                maghribMinutes = pref[Keys.MAGHRIB_OFFSET] ?: 0,
                ishaMinutes = pref[Keys.ISHA_OFFSET] ?: 0
            )
        )
    }

    suspend fun getSettingsOnce(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun updateLocation(location: UserLocation) {
        context.dataStore.edit { pref ->
            pref[Keys.LATITUDE] = location.latitude
            pref[Keys.LONGITUDE] = location.longitude
            pref[Keys.CITY_NAME] = location.cityName
            pref[Keys.COUNTRY_NAME] = location.countryName
            pref[Keys.TIMEZONE_ID] = location.timezoneId
            pref[Keys.IS_GPS] = location.isGps
        }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.name }
    }

    suspend fun updateJuristicMethod(method: JuristicMethod) {
        context.dataStore.edit { it[Keys.JURISTIC_METHOD] = method.name }
    }

    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        context.dataStore.edit { it[Keys.HIGH_LAT_RULE] = rule.name }
    }

    suspend fun setMasterAzan(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_AZAN] = enabled }
    }

    suspend fun setPrayerAzan(prayer: PrayerType, enabled: Boolean) {
        context.dataStore.edit { pref ->
            when (prayer) {
                PrayerType.FAJR -> pref[Keys.FAJR_AZAN] = enabled
                PrayerType.DHUHR -> pref[Keys.DHUHR_AZAN] = enabled
                PrayerType.ASR -> pref[Keys.ASR_AZAN] = enabled
                PrayerType.MAGHRIB -> pref[Keys.MAGHRIB_AZAN] = enabled
                PrayerType.ISHA -> pref[Keys.ISHA_AZAN] = enabled
                else -> Unit
            }
        }
    }

    suspend fun setAzanTone(tone: String, customUri: String? = null) {
        context.dataStore.edit { pref ->
            pref[Keys.AZAN_TONE] = tone
            if (customUri != null) {
                pref[Keys.CUSTOM_AZAN_URI] = customUri
            }
        }
    }

    suspend fun setSelectedCustomAzan(uri: String, fileName: String) {
        context.dataStore.edit { pref ->
            pref[Keys.AZAN_TONE] = "custom"
            pref[Keys.CUSTOM_AZAN_URI] = uri
            pref[Keys.CUSTOM_AZAN_FILE_NAME] = fileName
        }
    }

    suspend fun clearSelectedCustomAzan() {
        context.dataStore.edit { pref ->
            pref.remove(Keys.CUSTOM_AZAN_URI)
            pref.remove(Keys.CUSTOM_AZAN_FILE_NAME)
        }
    }

    suspend fun setPreReminder(enabled: Boolean, minutesBefore: Int? = null) {
        context.dataStore.edit { pref ->
            pref[Keys.PRE_REMINDER] = enabled
            if (minutesBefore != null) {
                pref[Keys.REMINDER_MINUTES] = minutesBefore
            }
        }
    }

    suspend fun setHijriAdjustment(days: Int) {
        context.dataStore.edit { it[Keys.HIJRI_ADJUSTMENT] = days }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = theme }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setIs24HourFormat(is24Hour: Boolean) {
        context.dataStore.edit { it[Keys.IS_24_HOUR] = is24Hour }
    }

    suspend fun setPrayerOffsets(adj: PrayerCalculator.PrayerAdjustment) {
        context.dataStore.edit { pref ->
            pref[Keys.FAJR_OFFSET] = adj.fajrMinutes
            pref[Keys.SUNRISE_OFFSET] = adj.sunriseMinutes
            pref[Keys.DHUHR_OFFSET] = adj.dhuhrMinutes
            pref[Keys.ASR_OFFSET] = adj.asrMinutes
            pref[Keys.SUNSET_OFFSET] = adj.sunsetMinutes
            pref[Keys.MAGHRIB_OFFSET] = adj.maghribMinutes
            pref[Keys.ISHA_OFFSET] = adj.ishaMinutes
        }
    }
}
