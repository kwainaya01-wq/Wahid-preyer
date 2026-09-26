package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.PrayerAlarmScheduler
import com.example.audio.AzanPlayer
import com.example.audio.NotificationHelper
import com.example.calculation.CalculationMethod
import com.example.calculation.HighLatitudeRule
import com.example.calculation.HijriCalendarHelper
import com.example.calculation.HijriDate
import com.example.calculation.JuristicMethod
import com.example.calculation.NextPrayerInfo
import com.example.calculation.PrayerCalculator
import com.example.calculation.PrayerTimes
import com.example.calculation.PrayerType
import com.example.calculation.QiblaCalculator
import com.example.data.AppSettings
import com.example.data.SettingsRepository
import com.example.location.AppLocationManager
import com.example.location.PresetCity
import com.example.location.UserLocation
import com.example.qibla.CompassManager
import com.example.qibla.CompassState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class MainUiState(
    val isLoadingLocation: Boolean = false,
    val locationErrorMessage: String? = null,
    val settings: AppSettings = AppSettings(),
    val todayPrayerTimes: PrayerTimes? = null,
    val tomorrowPrayerTimes: PrayerTimes? = null,
    val currentPrayer: PrayerType = PrayerType.FAJR,
    val nextPrayerInfo: NextPrayerInfo? = null,
    val currentTimeString: String = "",
    val hijriDate: HijriDate? = null,
    val isRamadan: Boolean = false,
    val timeToIftar: String? = null,
    val timeToSuhoor: String? = null,
    // Calendar screen state
    val selectedCalendarDate: LocalDate = LocalDate.now(),
    val calendarPrayerTimes: PrayerTimes? = null,
    val calendarHijriDate: HijriDate? = null,
    // Qibla screen state
    val qiblaBearing: Double = 0.0,
    val distanceToKaabaKm: Double = 0.0,
    val isAzanAudioPlaying: Boolean = false,
    val testAlarmScheduled: Boolean = false,
    val audioErrorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repo = SettingsRepository(context)
    val locationManager = AppLocationManager(context)
    val compassManager = CompassManager(context)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val compassState: StateFlow<CompassState> = compassManager.compassState

    private var clockJob: Job? = null

    init {
        NotificationHelper.createNotificationChannels(context)

        // Observe settings changes
        viewModelScope.launch {
            repo.settingsFlow.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
                recalculateAll(settings)
                PrayerAlarmScheduler.scheduleWithSettings(context, settings)
            }
        }

        // Observe audio player state
        viewModelScope.launch {
            AzanPlayer.isPlaying.collectLatest { playing ->
                _uiState.value = _uiState.value.copy(isAzanAudioPlaying = playing)
            }
        }

        // Start 1-second ticking clock
        startClock()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                updateTimeTick()
                delay(1000L)
            }
        }
    }

    private fun updateTimeTick() {
        val settings = _uiState.value.settings
        val zoneId = settings.userLocation.zoneId
        val now = ZonedDateTime.now(zoneId)
        val todayTimes = _uiState.value.todayPrayerTimes
        val tomorrowTimes = _uiState.value.tomorrowPrayerTimes

        val pattern = if (settings.is24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
        val timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern(pattern))

        if (todayTimes != null) {
            val next = todayTimes.getNextPrayer(now, tomorrowTimes)
            val current = todayTimes.getCurrentPrayer(now)

            // Ramadan Suhoor / Iftar countdowns
            var suhoorCountdown: String? = null
            var iftarCountdown: String? = null

            val fajrZdt = todayTimes.getZonedDateTime(PrayerType.FAJR)
            val maghribZdt = todayTimes.getZonedDateTime(PrayerType.MAGHRIB)

            if (now.isBefore(fajrZdt)) {
                val dur = Duration.between(now, fajrZdt).seconds.coerceAtLeast(0)
                suhoorCountdown = formatDuration(dur)
            }
            if (now.isBefore(maghribZdt)) {
                val dur = Duration.between(now, maghribZdt).seconds.coerceAtLeast(0)
                iftarCountdown = formatDuration(dur)
            }

            _uiState.value = _uiState.value.copy(
                currentTimeString = timeStr,
                nextPrayerInfo = next,
                currentPrayer = current,
                timeToSuhoor = suhoorCountdown,
                timeToIftar = iftarCountdown
            )
        } else {
            _uiState.value = _uiState.value.copy(currentTimeString = timeStr)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    fun recalculateAll(settings: AppSettings) {
        val loc = settings.userLocation
        val zoneId = loc.zoneId
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        val todayTimes = PrayerCalculator.calculate(
            latitude = loc.latitude,
            longitude = loc.longitude,
            date = today,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )

        val tomorrowTimes = PrayerCalculator.calculate(
            latitude = loc.latitude,
            longitude = loc.longitude,
            date = tomorrow,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )

        val hijri = HijriCalendarHelper.getHijriDate(today, settings.hijriAdjustment)

        val qibla = QiblaCalculator.calculateQiblaDirection(loc.latitude, loc.longitude)
        val distKaaba = QiblaCalculator.calculateDistanceToKaabaKm(loc.latitude, loc.longitude)
        compassManager.setQiblaBearing(qibla)

        // Also calculate for selected calendar date
        val calDate = _uiState.value.selectedCalendarDate
        val calTimes = PrayerCalculator.calculate(
            latitude = loc.latitude,
            longitude = loc.longitude,
            date = calDate,
            zoneId = zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )
        val calHijri = HijriCalendarHelper.getHijriDate(calDate, settings.hijriAdjustment)

        _uiState.value = _uiState.value.copy(
            todayPrayerTimes = todayTimes,
            tomorrowPrayerTimes = tomorrowTimes,
            hijriDate = hijri,
            isRamadan = hijri.isRamadan,
            qiblaBearing = qibla,
            distanceToKaabaKm = distKaaba,
            calendarPrayerTimes = calTimes,
            calendarHijriDate = calHijri
        )

        updateTimeTick()
    }

    fun refreshLocationWithGps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLocation = true, locationErrorMessage = null)
            val result = locationManager.getCurrentLocation()
            result.onSuccess { location ->
                repo.updateLocation(location)
                _uiState.value = _uiState.value.copy(isLoadingLocation = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    locationErrorMessage = err.message ?: "Failed to get GPS location. Please check permissions."
                )
            }
        }
    }

    fun selectPresetCity(preset: PresetCity) {
        viewModelScope.launch {
            val userLoc = preset.toUserLocation()
            repo.updateLocation(userLoc)
            repo.updateCalculationMethod(preset.suggestedMethod)
        }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            repo.updateCalculationMethod(method)
        }
    }

    fun setJuristicMethod(method: JuristicMethod) {
        viewModelScope.launch {
            repo.updateJuristicMethod(method)
        }
    }

    fun setHighLatitudeRule(rule: HighLatitudeRule) {
        viewModelScope.launch {
            repo.updateHighLatitudeRule(rule)
        }
    }

    fun toggleMasterAzan(enabled: Boolean) {
        viewModelScope.launch {
            repo.setMasterAzan(enabled)
        }
    }

    fun togglePrayerAzan(prayer: PrayerType, enabled: Boolean) {
        viewModelScope.launch {
            repo.setPrayerAzan(prayer, enabled)
        }
    }

    fun setAzanTone(tone: String, customUri: String? = null) {
        viewModelScope.launch {
            repo.setAzanTone(tone, customUri)
        }
    }

    fun setPreReminder(enabled: Boolean, minutes: Int? = null) {
        viewModelScope.launch {
            repo.setPreReminder(enabled, minutes)
        }
    }

    fun setHijriAdjustment(adjustmentDays: Int) {
        viewModelScope.launch {
            repo.setHijriAdjustment(adjustmentDays)
        }
    }

    fun setThemeMode(theme: String) {
        viewModelScope.launch {
            repo.setThemeMode(theme)
        }
    }

    fun setCalendarDate(date: LocalDate) {
        val settings = _uiState.value.settings
        val calTimes = PrayerCalculator.calculate(
            latitude = settings.userLocation.latitude,
            longitude = settings.userLocation.longitude,
            date = date,
            zoneId = settings.userLocation.zoneId,
            method = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            adjustment = settings.prayerAdjustment
        )
        val calHijri = HijriCalendarHelper.getHijriDate(date, settings.hijriAdjustment)

        _uiState.value = _uiState.value.copy(
            selectedCalendarDate = date,
            calendarPrayerTimes = calTimes,
            calendarHijriDate = calHijri
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repo.setOnboardingCompleted(true)
        }
    }

    fun testPlayAzan() {
        if (_uiState.value.isAzanAudioPlaying) {
            AzanPlayer.stop()
        } else {
            val settings = _uiState.value.settings
            if (!AzanPlayer.hasSelectedAudio(context, settings.customAzanUri)) {
                _uiState.value = _uiState.value.copy(
                    audioErrorMessage = "No Azan audio file selected. Please select an Azan audio file using the 'Select Azan Audio' button."
                )
                return
            }
            val result = AzanPlayer.playAzan(
                context = context,
                customUri = settings.customAzanUri,
                allowBundledFallback = false
            )
            if (result is com.example.audio.AzanPlayResult.Error) {
                _uiState.value = _uiState.value.copy(audioErrorMessage = result.message)
            } else {
                _uiState.value = _uiState.value.copy(audioErrorMessage = null)
            }
        }
    }

    fun onAudioFileSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Take persistable URI permissions
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Could not take persistable URI permission", e)
                }

                // 2. Query display name
                var displayName = "SelectedAzan.mp3"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1) {
                                val name = cursor.getString(nameIdx)
                                if (!name.isNullOrBlank()) {
                                    displayName = name
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Could not query display name", e)
                }

                // 3. Cache copy into internal storage for 100% reliable background/offline alarms
                val localAzanFile = AzanPlayer.getLocalAzanFile(context)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(localAzanFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Could not copy audio file locally", e)
                }

                // 4. Save to DataStore
                repo.setSelectedCustomAzan(uri.toString(), displayName)
                _uiState.value = _uiState.value.copy(audioErrorMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(audioErrorMessage = "Failed to load selected audio: ${e.message}")
            }
        }
    }

    fun useBundledAzan() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localAzanFile = AzanPlayer.getLocalAzanFile(context)
                val azanResId = context.resources.getIdentifier("azan", "raw", context.packageName)
                if (azanResId != 0) {
                    context.resources.openRawResource(azanResId).use { input ->
                        FileOutputStream(localAzanFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    repo.setSelectedCustomAzan("bundled", "azan.mp3 (Bundled)")
                    _uiState.value = _uiState.value.copy(audioErrorMessage = null)
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Failed to set bundled azan", e)
            }
        }
    }

    fun stopAzan() {
        AzanPlayer.stop()
    }

    fun dismissAudioError() {
        _uiState.value = _uiState.value.copy(audioErrorMessage = null)
    }

    fun testReminderChime() {
        AzanPlayer.playChime(context)
    }

    fun triggerTestNotification() {
        val settings = _uiState.value.settings
        NotificationHelper.showPrayerNotification(
            context = context,
            prayerType = PrayerType.MAGHRIB,
            timeString = _uiState.value.todayPrayerTimes?.formattedTime(PrayerType.MAGHRIB, settings.is24HourFormat) ?: "18:30",
            isPlayingAzan = false
        )
    }

    fun scheduleDevTestAlarm() {
        PrayerAlarmScheduler.scheduleTestAlarm(context, delayMinutes = 1)
        _uiState.value = _uiState.value.copy(testAlarmScheduled = true)
    }

    fun cancelDevTestAlarm() {
        PrayerAlarmScheduler.cancelTestAlarm(context)
        _uiState.value = _uiState.value.copy(testAlarmScheduled = false)
    }

    fun setPrayerOffsets(adj: PrayerCalculator.PrayerAdjustment) {
        viewModelScope.launch {
            repo.setPrayerOffsets(adj)
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        compassManager.stopListening()
        AzanPlayer.stop()
    }
}
