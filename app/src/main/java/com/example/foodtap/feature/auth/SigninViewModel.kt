package com.example.foodtap.feature.auth

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import java.util.Locale

class SigninViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "signin") {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }
}