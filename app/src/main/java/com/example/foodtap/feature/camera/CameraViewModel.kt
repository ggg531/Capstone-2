package com.example.foodtap.feature.camera

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse
import com.example.foodtap.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _ocrTextList = mutableListOf<String>()

    private val _identifiedAllergy = MutableStateFlow<List<String>>(emptyList())
    val identifiedAllergy: StateFlow<List<String>> = _identifiedAllergy

    private val _identifiedDesc = MutableStateFlow("")
    val identifiedDesc: StateFlow<String> = _identifiedDesc

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

    fun processResults() {
        val distinctText = _ocrTextList.distinct()
        val nutrition = distinctText.joinToString("\n")
        val expiry = distinctText.filter { containsDate(it) }.joinToString("\n")

        if (nutrition.isNotBlank() || expiry.isNotBlank()) {
            val fullText = "$nutrition\n$expiry"

            // 외부 API를 호출하여 OCR 결과를 전송하고 허가 여부 결정
            val request = OcrRequest(ocr = fullText)


            RetrofitClient.get_approval.getApproval(request).enqueue(object :
                Callback<OcrResponse> {
                override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                    if (response.isSuccessful) {
                        val approval = response.body()?.approval == true
                        val allergy = response.body()?.allergy
                        val desc = response.body()?.desc

                        Log.d("API", "approval: $approval")
                        Log.d("API", "allergy: $allergy")
                        Log.d("API", "desc: $desc")

                        if (approval && (allergy != null && desc != null)) {
                            _identifiedAllergy.value = allergy
                            _identifiedDesc.value = desc
                            _showDialog.value = true
                            _isScanning.value = false
                        }

                    } else {
                        Log.e("API", "서버 응답 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                    Log.e("API", "요청 실패: ${t.message}")
                }
            })
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