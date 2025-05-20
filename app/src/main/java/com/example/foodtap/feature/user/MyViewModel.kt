package com.example.foodtap.feature.user

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val _userExp = MutableStateFlow(5)
    val userExp: StateFlow<Int> = _userExp

    private val _userAllergy = MutableStateFlow<List<String>>(emptyList())
    val userAllergy: StateFlow<List<String>> = _userAllergy

    init {
        val context = getApplication<Application>()
        loadUserPreferences(context)
    }

    private fun loadUserPreferences(context: Context) {
        viewModelScope.launch {
            val expFile = File(context.filesDir, "user_exp.txt")
            val allergyFile = File(context.filesDir, "user_allergy.txt")

            if (expFile.exists()) {
                _userExp.value = expFile.readText().toIntOrNull() ?: 5
            }

            if (allergyFile.exists()) {
                _userAllergy.value = allergyFile.readText()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        }
    }

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