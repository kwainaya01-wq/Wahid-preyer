package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calculation.CalculationMethod
import com.example.calculation.HijriCalendarHelper
import com.example.calculation.JuristicMethod
import com.example.calculation.PrayerCalculator
import com.example.calculation.PrayerType
import com.example.calculation.QiblaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Wahid Prayer", appName)
    }

    @Test
    fun `verify prayer calculation for Makkah`() {
        val date = LocalDate.of(2026, 9, 26)
        val zoneId = ZoneId.of("Asia/Riyadh")

        val times = PrayerCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            date = date,
            zoneId = zoneId,
            method = CalculationMethod.UMM_AL_QURA,
            juristicMethod = JuristicMethod.SHAFI
        )

        assertNotNull(times.fajr)
        assertNotNull(times.sunrise)
        assertNotNull(times.dhuhr)
        assertNotNull(times.asr)
        assertNotNull(times.maghrib)
        assertNotNull(times.isha)

        // Verify logical progression of prayer times
        assertTrue(times.fajr.isBefore(times.sunrise))
        assertTrue(times.sunrise.isBefore(times.dhuhr))
        assertTrue(times.dhuhr.isBefore(times.asr))
        assertTrue(times.asr.isBefore(times.maghrib))
        assertTrue(times.maghrib.isBefore(times.isha))
    }

    @Test
    fun `verify Qibla direction from Cairo`() {
        // Cairo coordinates: 30.0444, 31.2357
        val qibla = QiblaCalculator.calculateQiblaDirection(30.0444, 31.2357)
        // From Cairo, Kaaba is approximately South-East (~135° to 137°)
        assertTrue("Qibla from Cairo should be between 130 and 140 degrees", qibla in 130.0..140.0)
    }

    @Test
    fun `verify Hijri conversion`() {
        val date = LocalDate.of(2026, 9, 26)
        val hijri = HijriCalendarHelper.getHijriDate(date)
        assertTrue("Hijri year should be roughly 1448", hijri.year in 1447..1449)
        assertTrue("Hijri month must be between 1 and 12", hijri.month in 1..12)
        assertTrue("Hijri day must be between 1 and 30", hijri.day in 1..30)
    }

    @Test
    fun `verify azan mp3 audio exists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hasAzan = com.example.audio.AzanPlayer.hasDefaultAzanAudio(context)
        assertTrue("azan.mp3 must be present in raw resources", hasAzan)
    }

    @Test
    fun `verify playAzan with null customUri plays bundled azan`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.shadows.ShadowMediaPlayer.setMediaInfoProvider { _ ->
            org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(1000, 0)
        }

        val result = com.example.audio.AzanPlayer.playAzan(context, customUri = null)
        val errorMsg = if (result is com.example.audio.AzanPlayResult.Error) result.message else ""
        assertTrue("When no custom audio is selected, should play bundled azan.mp3. Error: $errorMsg", result is com.example.audio.AzanPlayResult.Success)
        com.example.audio.AzanPlayer.stop()
    }

    @Test
    fun `verify playAzan with invalid customUri returns error and does not play chime`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val localFile = com.example.audio.AzanPlayer.getLocalAzanFile(context)
        if (localFile.exists()) localFile.delete()

        val result = com.example.audio.AzanPlayer.playAzan(context, customUri = "content://invalid/missing.mp3")
        assertTrue("When invalid custom URI is provided, should return Error without playing chime", result is com.example.audio.AzanPlayResult.Error)
    }
}
