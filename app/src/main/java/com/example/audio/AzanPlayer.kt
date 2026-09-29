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
        return try {
            val afd = context.resources.openRawResourceFd(R.raw.azan)
            val exists = afd != null
            afd?.close()
            exists
        } catch (_: Exception) {
            true
        }
    }

    fun hasSelectedAudio(context: Context, customUri: String?): Boolean {
        if (!customUri.isNullOrBlank() && customUri != "bundled") return true
        val localFile = getLocalAzanFile(context)
        return localFile.exists() && localFile.length() > 0
    }

    /**
     * Reliable central audio playback method for both Test Azan and scheduled prayer playback.
     *
     * - If [customUri] points to a user-selected audio file (content:// or file://), it plays that file
     *   via ContentResolver / FileDescriptor, with cached local file backup.
     * - If [customUri] is null, blank, or "bundled", it plays bundled azan.mp3 (R.raw.azan).
     * - Under NO circumstance does it silently substitute reminder_chime.wav.
     */
    @Synchronized
    fun playAzan(
        context: Context,
        customUri: String? = null,
        isFajr: Boolean = false,
        onCompletion: (() -> Unit)? = null
    ): AzanPlayResult {
        // 1. Stop any current playback cleanly
        stop()

        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager = audioMgr

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        // 2. Request Audio Focus
        requestAudioFocus(audioMgr, audioAttributes)

        var player: MediaPlayer? = null
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                isLooping = false
            }

            var dataSourceSet = false
            var customLoadError: Exception? = null

            // Check if user selected a custom file (not null, not empty, not "bundled")
            val isCustomSelected = !customUri.isNullOrBlank() && customUri != "bundled"

            if (isCustomSelected) {
                val uriString = customUri!!
                try {
                    val uri = Uri.parse(uriString)
                    // Attempt opening file descriptor via ContentResolver (supports persisted SAF URIs)
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        try {
                            player.setDataSource(pfd.fileDescriptor)
                            player.prepare()
                            dataSourceSet = true
                            Log.d(TAG, "Successfully loaded custom Azan via ContentResolver FD: $uriString")
                        } finally {
                            try { pfd.close() } catch (_: Exception) {}
                        }
                    } else {
                        // Direct Context URI fallback
                        player.setDataSource(context, uri)
                        player.prepare()
                        dataSourceSet = true
                        Log.d(TAG, "Successfully loaded custom Azan via context URI: $uriString")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed loading custom Azan from ContentResolver URI: $uriString", e)
                    customLoadError = e
                    // Try cached internal storage copy as fallback
                    val localFile = getLocalAzanFile(context)
                    if (localFile.exists() && localFile.length() > 0) {
                        try {
                            player.reset()
                            player.setAudioAttributes(audioAttributes)
                            val fis = FileInputStream(localFile)
                            try {
                                player.setDataSource(fis.fd)
                                player.prepare()
                                dataSourceSet = true
                                Log.d(TAG, "Successfully loaded custom Azan from cached local file (${localFile.length()} bytes)")
                            } finally {
                                try { fis.close() } catch (_: Exception) {}
                            }
                        } catch (cacheEx: Exception) {
                            Log.e(TAG, "Failed loading custom Azan from cached copy as well", cacheEx)
                        }
                    }
                }

                if (!dataSourceSet) {
                    // Do NOT silently fall back to chime or bundled audio when custom file fails.
                    // Return explicit diagnostic error so the user can fix the selected file.
                    abandonFocus()
                    player.release()
                    _isPlaying.value = false
                    val errorMsg = "Failed to play selected Azan audio: ${customLoadError?.localizedMessage ?: "File inaccessible"}"
                    Log.e(TAG, errorMsg)
                    return AzanPlayResult.Error(errorMsg)
                }
            } else {
                // Bundled Azan: Play R.raw.azan (app/src/main/res/raw/azan.mp3)
                try {
                    val afd = context.resources.openRawResourceFd(R.raw.azan)
                    if (afd != null) {
                        try {
                            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            player.prepare()
                            dataSourceSet = true
                            Log.d(TAG, "Successfully loaded bundled Azan (R.raw.azan / azan.mp3)")
                        } finally {
                            try { afd.close() } catch (_: Exception) {}
                        }
                    } else {
                        val resUri = Uri.parse("android.resource://${context.packageName}/${R.raw.azan}")
                        player.setDataSource(context, resUri)
                        player.prepare()
                        dataSourceSet = true
                        Log.d(TAG, "Successfully loaded bundled Azan via android.resource URI")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load bundled Azan (R.raw.azan)", e)
                    abandonFocus()
                    player.release()
                    _isPlaying.value = false
                    return AzanPlayResult.Error("Failed to load bundled azan.mp3: ${e.localizedMessage}")
                }
            }

            // 3. Configure listeners
            player.setOnCompletionListener {
                Log.d(TAG, "Azan playback completed naturally")
                _isPlaying.value = false
                stop()
                onCompletion?.invoke()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer playback error: what=$what, extra=$extra")
                _isPlaying.value = false
                stop()
                true
            }

            // 4. Start playback
            player.start()
            mediaPlayer = player
            _isPlaying.value = true
            Log.d(TAG, "Azan playback started successfully")
            return AzanPlayResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error playing Azan", e)
            try { player?.release() } catch (_: Exception) {}
            abandonFocus()
            _isPlaying.value = false
            return AzanPlayResult.Error("Audio error: ${e.localizedMessage ?: "Unknown error"}")
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
