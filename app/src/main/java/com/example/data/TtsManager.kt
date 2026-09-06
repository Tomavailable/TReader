package com.example.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

data class TtsVoiceItem(
    val id: String,
    val name: String,
    val locale: Locale,
    val isNetworkConnectionRequired: Boolean = false,
    val latency: Int = Voice.LATENCY_NORMAL,
    val quality: Int = Voice.QUALITY_NORMAL
)

class TtsManager(private val context: Context) {

    private val TAG = "TtsManager"
    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceItem>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceItem>> = _availableVoices.asStateFlow()

    var onUtteranceDone: ((utteranceId: String) -> Unit)? = null
    var onUtteranceError: ((utteranceId: String) -> Unit)? = null

    private var activeSynthesizeDeferred: CompletableDeferred<Boolean>? = null
    private var activeSynthesizeUtteranceId: String? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupVoices()
                setupProgressListener()
                _isInitialized.value = true
                Log.d(TAG, "TTS initialized successfully with ${_availableVoices.value.size} voices")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Utterance done: $utteranceId")
                if (utteranceId != null && utteranceId == activeSynthesizeUtteranceId) {
                    activeSynthesizeDeferred?.complete(true)
                }
                if (utteranceId != null) {
                    onUtteranceDone?.invoke(utteranceId)
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Utterance error: $utteranceId")
                if (utteranceId != null && utteranceId == activeSynthesizeUtteranceId) {
                    activeSynthesizeDeferred?.complete(false)
                }
                if (utteranceId != null) {
                    onUtteranceError?.invoke(utteranceId)
                }
            }
        })
    }

    private fun setupVoices() {
        try {
            val systemVoices = tts?.voices ?: emptySet()
            val list = systemVoices.map { voice ->
                TtsVoiceItem(
                    id = voice.name,
                    name = formatVoiceDisplayName(voice),
                    locale = voice.locale,
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired,
                    latency = voice.latency,
                    quality = voice.quality
                )
            }.sortedWith(compareBy({ it.locale.displayName }, { it.name }))

            _availableVoices.value = list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query system voices: ${e.message}")
        }
    }

    private fun formatVoiceDisplayName(voice: Voice): String {
        val langName = voice.locale.displayLanguage
        val countryName = voice.locale.displayCountry
        val simpleName = voice.name.substringAfterLast("#").substringAfterLast("-")
        return if (countryName.isNotBlank()) {
            "$langName ($countryName) - $simpleName"
        } else {
            "$langName - $simpleName"
        }
    }

    /**
     * Finds a Voice object matching the given ID.
     */
    fun findVoiceById(voiceId: String?): Voice? {
        if (voiceId.isNullOrBlank()) return null
        return try {
            tts?.voices?.firstOrNull { it.name == voiceId }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Speaks the given text with specified voice, rate, and pitch.
     */
    fun speak(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        utteranceId: String
    ) {
        val engine = tts ?: return
        try {
            val voice = findVoiceById(voiceId)
            if (voice != null) {
                engine.voice = voice
                engine.language = voice.locale
            }

            engine.setSpeechRate(rate.coerceIn(0.2f, 3.0f))
            engine.setPitch(pitch.coerceIn(0.3f, 2.0f))

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Speak failed: ${e.message}")
        }
    }

    /**
     * Synthesizes text to a WAV audio file asynchronously.
     */
    suspend fun synthesizeToFile(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        outputFile: File,
        utteranceId: String
    ): Boolean {
        val engine = tts ?: return false
        val deferred = CompletableDeferred<Boolean>()
        activeSynthesizeDeferred = deferred
        activeSynthesizeUtteranceId = utteranceId

        try {
            val voice = findVoiceById(voiceId)
            if (voice != null) {
                engine.voice = voice
                engine.language = voice.locale
            }
            engine.setSpeechRate(rate.coerceIn(0.2f, 3.0f))
            engine.setPitch(pitch.coerceIn(0.3f, 2.0f))

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val result = engine.synthesizeToFile(text, params, outputFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                return false
            }
            return deferred.await()
        } catch (e: Exception) {
            Log.e(TAG, "SynthesizeToFile failed: ${e.message}")
            return false
        } finally {
            if (activeSynthesizeUtteranceId == utteranceId) {
                activeSynthesizeDeferred = null
                activeSynthesizeUtteranceId = null
            }
        }
    }

    /**
     * Stops any currently playing speech.
     */
    fun speakWordWithAccent(
        word: String,
        accentLocale: Locale = Locale.UK,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        val engine = tts ?: return
        try {
            val matchingVoice = engine.voices?.firstOrNull { 
                it.locale.country == accentLocale.country && it.locale.language == accentLocale.language 
            } ?: engine.voices?.firstOrNull { it.locale.language == accentLocale.language }

            if (matchingVoice != null) {
                engine.voice = matchingVoice
            }
            engine.language = accentLocale
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)

            val utteranceId = "WORD_PRONUNCIATION_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            engine.speak(word, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "speakWordWithAccent failed: ${e.message}")
        }
    }

    /**
     * Stops any currently playing speech.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Stop failed: ${e.message}")
        }
    }

    /**
     * Releases TTS resources.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Shutdown failed: ${e.message}")
        }
    }
}
