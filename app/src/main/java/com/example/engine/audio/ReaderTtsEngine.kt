package com.example.engine.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 0 KB Offline Read-Aloud engine backed by Android's built-in TextToSpeech.
 */
class ReaderTtsEngine(
    context: Context,
    private val onPageFinishedSpeaking: () -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null
    private var pendingRate: Float = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == UTTERANCE_PAGE) {
                            onPageFinishedSpeaking()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = Unit
                })
                pendingText?.let { text ->
                    pendingText = null
                    speak(text, pendingRate)
                }
            }
        }
    }

    fun speak(text: String, speed: Float) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (!isReady) {
            pendingText = clean
            pendingRate = speed
            return
        }
        runCatching {
            tts?.setSpeechRate(speed.coerceIn(0.75f, 2.0f))
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_PAGE)
        }
    }

    fun setSpeed(speed: Float) {
        pendingRate = speed.coerceIn(0.75f, 2.0f)
        if (isReady) {
            runCatching { tts?.setSpeechRate(pendingRate) }
        }
    }

    fun stop() {
        pendingText = null
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        pendingText = null
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isReady = false
    }

    companion object {
        private const val UTTERANCE_PAGE = "folio_page_read"
    }
}
