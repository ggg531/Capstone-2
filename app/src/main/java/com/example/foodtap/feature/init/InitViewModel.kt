package com.example.foodtap.feature.init

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.allergy.TextRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.util.Log

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

    fun speak(text: String, utteranceId: String) {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            speak("화면을 탭하여 보유 알레르기 성분을 음성으로 등록하세요", "starttap")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}

            override fun onResults(results: Bundle?) {
                val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _rawSttText.value = result
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitClient.allergy_keyword_instance.extractKeywords(TextRequest(result))
                        withContext(Dispatchers.Main) {
                            if (response.keywords.isNotEmpty()) {
                                _allergySttText.value = response.keywords.joinToString(", ")
                            } else {
                                _allergySttText.value = ""
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RetrofitError", "서버 연결 실패: ${e.message}")
                        withContext(Dispatchers.Main) {
                            _allergySttText.value = ""
                        }
                    }
                }
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _showDialog.value = true
            }

            override fun onEndOfSpeech() {
                _isListening.value = false
                _showDialog.value = true
            }

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
            speak("화면을 탭하여 보유 알레르기 성분을 음성으로 등록하세요", "starttap")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}