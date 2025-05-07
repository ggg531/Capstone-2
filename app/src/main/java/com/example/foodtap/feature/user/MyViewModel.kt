package com.example.foodtap.feature.user

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "my") {
        if (isTtsInitialized) {
            tts.stop()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            tts.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }
}