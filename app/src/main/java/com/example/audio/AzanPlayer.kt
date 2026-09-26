package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AzanPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Synchronized
    fun playAzan(
        context: Context,
        isFajr: Boolean = false,
        customUri: String? = null,
        onCompletion: (() -> Unit)? = null
    ) {
        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )

                if (!customUri.isNullOrBlank()) {
                    try {
                        setDataSource(context, Uri.parse(customUri))
                    } catch (_: Exception) {
                        // Fallback to bundled resource
                        val resId = if (isFajr) R.raw.azan_fajr else R.raw.azan_default
                        val afd = context.resources.openRawResourceFd(resId)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                } else {
                    val resId = if (isFajr) R.raw.azan_fajr else R.raw.azan_default
                    val afd = context.resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }

                prepare()
                start()
            }

            player.setOnCompletionListener {
                _isPlaying.value = false
                stop()
                onCompletion?.invoke()
            }

            player.setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                stop()
                true
            }

            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

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
            e.printStackTrace()
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
        }
    }
}
