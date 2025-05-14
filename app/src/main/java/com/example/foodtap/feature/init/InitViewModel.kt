package com.example.foodtap.feature.init

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class InitViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _rawSttText = MutableStateFlow("")
    val rawSttText: StateFlow<String> = _rawSttText

    private val _allergySttText = MutableStateFlow("")
    val allergySttText: StateFlow<String> = _allergySttText

    private val _registeredAllergyText = MutableStateFlow("")
    val registeredAllergyText: StateFlow<String> = _registeredAllergyText

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(application.applicationContext)

    private val recognizerIntent: Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "init") {
        if (isTtsInitialized) {
            tts.stop()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            speak("화면을 탭하여 알레르기 성분을 음성으로 등록하세요", "starttap")
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val result =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?: ""
                _rawSttText.value = result
                _allergySttText.value = result
                _isListening.value = false
                _showDialog.value = true
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _showDialog.value = true
            }

            override fun onEndOfSpeech() {
                _isListening.value = false
                _showDialog.value = true
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        _isListening.value = true
        _rawSttText.value = ""
        _allergySttText.value = ""
        startSpeechRecognition()
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            speak("음성 인식 중입니다.", "startListening")
        }
    }

    private fun startSpeechRecognition() {
        speechRecognizer.startListening(recognizerIntent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
        _showDialog.value = true
        _isListening.value = false
    }

    fun tapShowDialog(value: Boolean) {
        _showDialog.value = value
    }

    fun confirmResult() {
        _registeredAllergyText.value = _allergySttText.value
        _showDialog.value = false
    }

    fun resetResult() {
        _rawSttText.value = ""
        _allergySttText.value = ""
        _showDialog.value = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            speak("화면을 탭하여 알레르기 성분을 음성으로 등록하세요", "starttap")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}