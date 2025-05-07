package com.example.foodtap.feature.camera

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _ocrTextList = mutableListOf<String>()

    private val _nutritionText = MutableStateFlow("")
    val nutritionText: StateFlow<String> = _nutritionText

    private val _expiryText = MutableStateFlow("")
    val expiryText: StateFlow<String> = _expiryText

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "camera") {
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

    fun addOcrResult(result: String) {
        if (result.isNotBlank() && _isScanning.value) {
            val lines = result.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            _ocrTextList.addAll(lines)
        }
    }

    fun processResults() { // 텍스트 결과 처리 (필터링)
        val distinctText = _ocrTextList.distinct()
        val nutrition = distinctText.joinToString("\n")
        val expiry = distinctText.filter { containsDate(it) }.joinToString("\n")

        if (nutrition.isNotBlank() || expiry.isNotBlank()) {
            _isScanning.value = false
            _nutritionText.value = nutrition
            _expiryText.value = expiry
            _showDialog.value = true
        }
        _ocrTextList.clear()
    }

    fun resetScan() {
        _isScanning.value = true
        _showDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }

    private fun containsDate(text: String): Boolean {
        val datePatterns = listOf(
            "\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}",
            "\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}"
        )
        return datePatterns.any { Regex(it).containsMatchIn(text) }
    }
}