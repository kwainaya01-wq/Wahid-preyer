package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

sealed class AzanPlayResult {
    object Success : AzanPlayResult()
    data class Error(val message: String) : AzanPlayResult()
}

object AzanPlayer {

    private const val TAG = "AzanPlayer"
    const val MISSING_AZAN_MESSAGE = "Azan audio is not selected. Please select an Azan audio file in Azan & Audio settings."
    const val LOCAL_AZAN_FILE_NAME = "selected_azan_audio"

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun getLocalAzanFile(context: Context): File {
        return File(context.filesDir, LOCAL_AZAN_FILE_NAME)
    }

    fun hasDefaultAzanAudio(context: Context): Boolean {
        val resId = context.resources.getIdentifier("azan", "raw", context.packageName)
        return resId != 0
    }

    fun hasSelectedAudio(context: Context, customUri: String?): Boolean {
        val localFile = getLocalAzanFile(context)
        if (localFile.exists() && localFile.length() > 0) return true
        if (!customUri.isNullOrBlank()) return true
        return false
    }

    @Synchronized
    fun playAzan(
        context: Context,
        isFajr: Boolean = false,
        customUri: String? = null,
        allowBundledFallback: Boolean = false,
        onCompletion: (() -> Unit)? = null
    ): AzanPlayResult {
        stop()

        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager = audioMgr

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        // 1. Request Audio Focus
        requestAudioFocus(audioMgr, audioAttributes)

        // 2. Determine and open data source
        var openedPlayer: MediaPlayer? = null
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
            }

            var dataSourceSet = false

            // Priority 1: Check internally persisted/cached user audio file
            val localFile = getLocalAzanFile(context)
            if (localFile.exists() && localFile.length() > 0) {
                try {
                    val fis = FileInputStream(localFile)
                    player.setDataSource(fis.fd)
                    fis.close()
                    dataSourceSet = true
                    Log.d(TAG, "Playing Azan from persistent local file: ${localFile.length()} bytes")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load persistent local audio file", e)
                }
            }

            // Priority 2: Try custom URI if provided
            if (!dataSourceSet && !customUri.isNullOrBlank()) {
                if (customUri == "bundled") {
                    val azanResId = context.resources.getIdentifier("azan", "raw", context.packageName)
                    if (azanResId != 0) {
                        val afd = context.resources.openRawResourceFd(azanResId)
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        dataSourceSet = true
                    }
                } else {
                    try {
                        player.setDataSource(context, Uri.parse(customUri))
                        dataSourceSet = true
                        Log.d(TAG, "Playing Azan from SAF URI: $customUri")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load custom audio URI: $customUri", e)
                    }
                }
            }

            // Priority 3: Bundled fallback ONLY if explicitly allowed (e.g. testing)
            if (!dataSourceSet && allowBundledFallback) {
                val azanResId = context.resources.getIdentifier("azan", "raw", context.packageName)
                if (azanResId != 0) {
                    val afd = context.resources.openRawResourceFd(azanResId)
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    dataSourceSet = true
                }
            }

            // If no user audio selected and no source could be set:
            if (!dataSourceSet) {
                abandonFocus()
                player.release()
                Log.e(TAG, MISSING_AZAN_MESSAGE)
                return AzanPlayResult.Error(MISSING_AZAN_MESSAGE)
            }

            player.prepare()
            player.start()

            player.setOnCompletionListener {
                _isPlaying.value = false
                stop()
                onCompletion?.invoke()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                _isPlaying.value = false
                stop()
                true
            }

            openedPlayer = player
            mediaPlayer = player
            _isPlaying.value = true
            return AzanPlayResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error playing Azan", e)
            openedPlayer?.release()
            abandonFocus()
            _isPlaying.value = false
            return AzanPlayResult.Error(e.message ?: MISSING_AZAN_MESSAGE)
        }
    }

    /**
     * Plays gentle reminder chime ONLY for pre-prayer reminders. Never used as Azan.
     */
    @Synchronized
    fun playChime(context: Context, onCompletion: (() -> Unit)? = null) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(R.raw.reminder_chime)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
            }
            player.setOnCompletionListener {
                stop()
                onCompletion?.invoke()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing reminder chime", e)
        }
    }

    @Synchronized
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            abandonFocus()
        }
    }

    private fun requestAudioFocus(audioMgr: AudioManager?, audioAttributes: AudioAttributes) {
        if (audioMgr == null) return

        val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            ) {
                stop()
            }
        }
        audioFocusChangeListener = listener

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(listener)
                .build()
            audioFocusRequest = req
            audioMgr.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioMgr.requestAudioFocus(
                listener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonFocus() {
        val mgr = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { mgr.abandonAudioFocusRequest(it) }
            } else {
                audioFocusChangeListener?.let {
                    @Suppress("DEPRECATION")
                    mgr.abandonAudioFocus(it)
                }
            }
        } catch (_: Exception) {}
        audioFocusRequest = null
        audioFocusChangeListener = null
    }
}
