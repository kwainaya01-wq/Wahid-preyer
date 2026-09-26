package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calculation.CalculationMethod
import com.example.calculation.HighLatitudeRule
import com.example.calculation.JuristicMethod
import com.example.calculation.PrayerCalculator
import com.example.calculation.PrayerType
import com.example.location.PresetCities
import com.example.location.PresetCity
import com.example.ui.theme.IslamicEmerald
import com.example.ui.theme.IslamicGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings = uiState.settings

    var showCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showJuristicDialog by remember { mutableStateOf(false) }
    var showHighLatDialog by remember { mutableStateOf(false) }
    var showOffsetsDialog by remember { mutableStateOf(false) }
    var showAzanToneDialog by remember { mutableStateOf(false) }
    var showReminderMinutesDialog by remember { mutableStateOf(false) }
    var showHijriAdjustmentDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Audio file picker launcher (Storage Access Framework)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAudioFileSelected(uri)
        }
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocationWithGps()
        }
    }

    // Permission launcher for Notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. LOCATION SECTION
        item {
            SectionHeader(title = "LOCATION", icon = Icons.Default.LocationOn)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.userLocation.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lat: ${String.format("%.4f", settings.userLocation.latitude)}, Lon: ${String.format("%.4f", settings.userLocation.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Timezone: ${settings.userLocation.timezoneId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (settings.userLocation.isGps) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (settings.userLocation.isGps) "GPS" else "Manual",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.userLocation.isGps) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.locationManager.hasLocationPermission()) {
                                    viewModel.refreshLocationWithGps()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("use_gps_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use GPS")
                        }

                        OutlinedButton(
                            onClick = { showCityDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("choose_city_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Select City")
                        }
                    }
                }
            }
        }

        // 2. PRAYER CALCULATION SECTION
        item {
            SectionHeader(title = "PRAYER CALCULATION", icon = Icons.Default.DateRange)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingClickableItem(
                        title = "Calculation Method",
                        subtitle = settings.calculationMethod.title,
                        onClick = { showMethodDialog = true },
                        testTag = "setting_calc_method"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingClickableItem(
                        title = "Asr Juristic Method",
                        subtitle = settings.juristicMethod.title,
                        onClick = { showJuristicDialog = true },
                        testTag = "setting_juristic_method"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingClickableItem(
                        title = "High Latitude Rule",
                        subtitle = settings.highLatitudeRule.title,
                        onClick = { showHighLatDialog = true },
                        testTag = "setting_high_lat_rule"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingClickableItem(
                        title = "Manual Minute Adjustments",
                        subtitle = "Fine tune minutes for individual prayers",
                        onClick = { showOffsetsDialog = true },
                        testTag = "setting_prayer_offsets"
                    )
                }
            }
        }

        // 3. AZAN & AUDIO SECTION
        item {
            SectionHeader(title = "AZAN & AUDIO", icon = Icons.Default.VolumeUp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Azan [ ON/OFF ]
                    SettingSwitchItem(
                        title = "Azan",
                        subtitle = if (settings.masterAzanEnabled) "Azan is enabled" else "Azan is muted",
                        checked = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.toggleMasterAzan(it) },
                        testTag = "setting_master_azan_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Fajr [ON/OFF]
                    SettingSwitchItem(
                        title = "Fajr",
                        subtitle = "${PrayerType.FAJR.arabicName} Salah",
                        checked = settings.isAzanEnabledFor(PrayerType.FAJR),
                        enabled = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.togglePrayerAzan(PrayerType.FAJR, it) },
                        testTag = "setting_azan_fajr"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Dhuhr [ON/OFF]
                    SettingSwitchItem(
                        title = "Dhuhr",
                        subtitle = "${PrayerType.DHUHR.arabicName} Salah",
                        checked = settings.isAzanEnabledFor(PrayerType.DHUHR),
                        enabled = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.togglePrayerAzan(PrayerType.DHUHR, it) },
                        testTag = "setting_azan_dhuhr"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Asr [ON/OFF]
                    SettingSwitchItem(
                        title = "Asr",
                        subtitle = "${PrayerType.ASR.arabicName} Salah",
                        checked = settings.isAzanEnabledFor(PrayerType.ASR),
                        enabled = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.togglePrayerAzan(PrayerType.ASR, it) },
                        testTag = "setting_azan_asr"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Maghrib [ON/OFF]
                    SettingSwitchItem(
                        title = "Maghrib",
                        subtitle = "${PrayerType.MAGHRIB.arabicName} Salah",
                        checked = settings.isAzanEnabledFor(PrayerType.MAGHRIB),
                        enabled = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.togglePrayerAzan(PrayerType.MAGHRIB, it) },
                        testTag = "setting_azan_maghrib"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Isha [ON/OFF]
                    SettingSwitchItem(
                        title = "Isha",
                        subtitle = "${PrayerType.ISHA.arabicName} Salah",
                        checked = settings.isAzanEnabledFor(PrayerType.ISHA),
                        enabled = settings.masterAzanEnabled,
                        onCheckedChange = { viewModel.togglePrayerAzan(PrayerType.ISHA, it) },
                        testTag = "setting_azan_isha"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Azan Audio Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Azan Audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val selectedName = settings.customAzanFileName ?: "None (Please select an audio file)"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (settings.customAzanFileName != null) IslamicGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Selected: $selectedName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (settings.customAzanFileName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // [ Select Azan Audio ] (or [ Change Audio ])
                        Button(
                            onClick = {
                                audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_azan_audio_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (settings.customAzanFileName != null) "Change Audio" else "Select Azan Audio")
                        }

                        // [ Test Azan ] and [ Stop Azan ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.testPlayAzan() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_azan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Azan")
                            }

                            FilledTonalButton(
                                onClick = { viewModel.stopAzan() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (uiState.isAzanAudioPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (uiState.isAzanAudioPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stop_azan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Azan")
                            }
                        }

                        // Option to easily use bundled azan.mp3
                        OutlinedButton(
                            onClick = { viewModel.useBundledAzan() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Use Bundled Azan (azan.mp3)")
                        }
                    }
                }
            }
        }

        // 4. NOTIFICATIONS SECTION
        item {
            SectionHeader(title = "NOTIFICATIONS", icon = Icons.Default.Notifications)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Permission",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasNotificationPermission) "Granted (Notifications active)" else "Denied (Azan notifications may fail)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasNotificationPermission) IslamicEmerald else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Allow")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    SettingSwitchItem(
                        title = "Pre-Prayer Reminder",
                        subtitle = "Advance gentle chime reminder before prayer",
                        checked = settings.preReminderEnabled,
                        onCheckedChange = { viewModel.setPreReminder(it) },
                        testTag = "setting_pre_reminder_switch"
                    )

                    if (settings.preReminderEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        SettingClickableItem(
                            title = "Reminder Timing",
                            subtitle = "${settings.reminderMinutesBefore} minutes before prayer",
                            onClick = { showReminderMinutesDialog = true },
                            testTag = "setting_reminder_duration"
                        )
                    }
                }
            }
        }

        // 5. APPEARANCE & CALENDAR SECTION
        item {
            SectionHeader(title = "APPEARANCE & CALENDAR", icon = Icons.Default.ColorLens)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingClickableItem(
                        title = "App Theme",
                        subtitle = settings.themeMode.replaceFirstChar { it.uppercase() },
                        onClick = { showThemeDialog = true },
                        testTag = "setting_theme_mode"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingClickableItem(
                        title = "Hijri Date Adjustment",
                        subtitle = if (settings.hijriAdjustment == 0) "Standard (0 days)" else "${if (settings.hijriAdjustment > 0) "+" else ""}${settings.hijriAdjustment} days",
                        onClick = { showHijriAdjustmentDialog = true },
                        testTag = "setting_hijri_adjustment"
                    )
                }
            }
        }

        // 6. ANDROID BACKGROUND RELIABILITY GUIDE
        item {
            SectionHeader(title = "BACKGROUND RELIABILITY", icon = Icons.Default.BatteryAlert)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Why Do Alarms Sometimes Get Delayed on Android?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Device manufacturers (such as Samsung, Xiaomi, Huawei, OnePlus, Oppo) have aggressive battery saver policies that kill background services or defer AlarmManager tasks.\n\nTo ensure Azan always rings reliably:\n1. Set Battery Usage for Wahid Prayer to 'Unrestricted'.\n2. Allow 'Autostart' or disable 'App Kill' in system settings.\n3. Keep Exact Alarm permission enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(appIntent)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Battery Optimization Settings")
                    }
                }
            }
        }

        // 7. DEVELOPER & TEST UTILITY SECTION
        item {
            SectionHeader(title = "DEVELOPER / TEST UTILITIES", icon = Icons.Default.Alarm)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Use these tools to verify alarm and notification behavior without waiting for actual prayer hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerTestNotification() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_test_notification_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Preview Notification", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.testReminderChime() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_test_chime_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Test Chime", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.scheduleDevTestAlarm() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_schedule_alarm_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Schedule +1m Alarm", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelDevTestAlarm() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_cancel_alarm_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel Alarm", fontSize = 12.sp)
                        }
                    }

                    if (uiState.testAlarmScheduled) {
                        Text(
                            text = "✅ Test alarm scheduled for 1 minute from now.",
                            color = IslamicEmerald,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // DIALOGS:
    // 1. Choose City Dialog
    if (showCityDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = remember(searchQuery) {
            if (searchQuery.isBlank()) PresetCities.CITIES
            else PresetCities.CITIES.filter {
                it.city.contains(searchQuery, ignoreCase = true) || it.country.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Select City") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search City or Country") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filtered) { city ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectPresetCity(city)
                                        showCityDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = city.city, fontWeight = FontWeight.Bold)
                                    Text(text = city.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(
                                    text = city.suggestedMethod.name.split("_").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Calculation Method Dialog
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("Calculation Method") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(CalculationMethod.values()) { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCalculationMethod(method)
                                    showMethodDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (method == settings.calculationMethod),
                                onClick = {
                                    viewModel.setCalculationMethod(method)
                                    showMethodDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = method.title, fontWeight = FontWeight.SemiBold)
                                Text(text = method.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Juristic Method Dialog (Asr)
    if (showJuristicDialog) {
        AlertDialog(
            onDismissRequest = { showJuristicDialog = false },
            title = { Text("Asr Juristic Method") },
            text = {
                Column {
                    JuristicMethod.values().forEach { juristic ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setJuristicMethod(juristic)
                                    showJuristicDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (juristic == settings.juristicMethod),
                                onClick = {
                                    viewModel.setJuristicMethod(juristic)
                                    showJuristicDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = juristic.title, fontWeight = FontWeight.SemiBold)
                                Text(text = juristic.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJuristicDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. High Latitude Rule Dialog
    if (showHighLatDialog) {
        AlertDialog(
            onDismissRequest = { showHighLatDialog = false },
            title = { Text("High Latitude Rule") },
            text = {
                Column {
                    HighLatitudeRule.values().forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setHighLatitudeRule(rule)
                                    showHighLatDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (rule == settings.highLatitudeRule),
                                onClick = {
                                    viewModel.setHighLatitudeRule(rule)
                                    showHighLatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = rule.title, fontWeight = FontWeight.SemiBold)
                                Text(text = rule.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHighLatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Select Azan Audio Tone Dialog
    if (showAzanToneDialog) {
        AlertDialog(
            onDismissRequest = { showAzanToneDialog = false },
            title = { Text("Select Azan Audio") },
            text = {
                Column {
                    val tones = listOf(
                        "default" to "Default Azan (azan.mp3)",
                        "custom" to "Custom Audio File",
                        "none" to "No Azan / Notification Only"
                    )
                    tones.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (key == "custom") {
                                        showAzanToneDialog = false
                                        audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                                    } else {
                                        viewModel.setAzanTone(key)
                                        showAzanToneDialog = false
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.azanTone == key),
                                onClick = {
                                    if (key == "custom") {
                                        showAzanToneDialog = false
                                        audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                                    } else {
                                        viewModel.setAzanTone(key)
                                        showAzanToneDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            showAzanToneDialog = false
                            audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Custom Audio File from Storage...")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAzanToneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Missing Audio Notice Dialog
    if (uiState.audioErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAudioError() },
            title = { Text("Azan Audio Notice") },
            text = { Text(uiState.audioErrorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAudioError() }) {
                    Text("OK")
                }
            }
        )
    }

    // 6. Pre-Reminder Minutes Dialog
    if (showReminderMinutesDialog) {
        AlertDialog(
            onDismissRequest = { showReminderMinutesDialog = false },
            title = { Text("Pre-Prayer Reminder Duration") },
            text = {
                Column {
                    listOf(5, 10, 15, 20).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPreReminder(true, mins)
                                    showReminderMinutesDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.reminderMinutesBefore == mins),
                                onClick = {
                                    viewModel.setPreReminder(true, mins)
                                    showReminderMinutesDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$mins minutes before prayer", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderMinutesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Hijri Adjustment Dialog
    if (showHijriAdjustmentDialog) {
        AlertDialog(
            onDismissRequest = { showHijriAdjustmentDialog = false },
            title = { Text("Islamic Hijri Date Adjustment") },
            text = {
                Column {
                    listOf(-2, -1, 0, 1, 2).forEach { adj ->
                        val label = when (adj) {
                            0 -> "Standard (0 days)"
                            else -> "${if (adj > 0) "+" else ""}$adj day${if (adj != 1 && adj != -1) "s" else ""}"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setHijriAdjustment(adj)
                                    showHijriAdjustmentDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.hijriAdjustment == adj),
                                onClick = {
                                    viewModel.setHijriAdjustment(adj)
                                    showHijriAdjustmentDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHijriAdjustmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance Theme") },
            text = {
                Column {
                    listOf("system" to "System Default", "light" to "Light Mode", "dark" to "Dark Mode").forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.themeMode == key),
                                onClick = {
                                    viewModel.setThemeMode(key)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 9. Manual Minute Adjustments Dialog
    if (showOffsetsDialog) {
        var fajrOff by remember { mutableIntStateOf(settings.prayerAdjustment.fajrMinutes) }
        var dhuhrOff by remember { mutableIntStateOf(settings.prayerAdjustment.dhuhrMinutes) }
        var asrOff by remember { mutableIntStateOf(settings.prayerAdjustment.asrMinutes) }
        var maghribOff by remember { mutableIntStateOf(settings.prayerAdjustment.maghribMinutes) }
        var ishaOff by remember { mutableIntStateOf(settings.prayerAdjustment.ishaMinutes) }

        AlertDialog(
            onDismissRequest = { showOffsetsDialog = false },
            title = { Text("Minute Adjustments (+/-)") },
            text = {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    OffsetRow(name = "Fajr", value = fajrOff, onChange = { fajrOff = it })
                    OffsetRow(name = "Dhuhr", value = dhuhrOff, onChange = { dhuhrOff = it })
                    OffsetRow(name = "Asr", value = asrOff, onChange = { asrOff = it })
                    OffsetRow(name = "Maghrib", value = maghribOff, onChange = { maghribOff = it })
                    OffsetRow(name = "Isha", value = ishaOff, onChange = { ishaOff = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setPrayerOffsets(
                        PrayerCalculator.PrayerAdjustment(
                            fajrMinutes = fajrOff,
                            dhuhrMinutes = dhuhrOff,
                            asrMinutes = asrOff,
                            maghribMinutes = maghribOff,
                            ishaMinutes = ishaOff
                        )
                    )
                    showOffsetsDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOffsetsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OffsetRow(name: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > -30) onChange(value - 1) }, modifier = Modifier.size(32.dp)) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${if (value > 0) "+" else ""}$value m",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { if (value < 30) onChange(value + 1) }, modifier = Modifier.size(32.dp)) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = IslamicGold,
                checkedTrackColor = IslamicEmerald
            )
        )
    }
}
