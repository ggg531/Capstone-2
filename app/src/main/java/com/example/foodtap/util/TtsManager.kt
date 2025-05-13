package com.example.foodtap.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsManager : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun initialize(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "") {
        if (isTtsInitialized) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            tts?.stop()
        }
    }
}