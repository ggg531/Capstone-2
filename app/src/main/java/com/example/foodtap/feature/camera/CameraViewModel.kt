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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _ocrTextList = mutableListOf<String>()

    private val _identifiedAllergy = MutableStateFlow<List<String>>(emptyList())
    val identifiedAllergy: StateFlow<List<String>> = _identifiedAllergy

    private val _identifiedDesc = MutableStateFlow("")
    val identifiedDesc: StateFlow<String> = _identifiedDesc

    private val _dDayDesc = MutableStateFlow<Int?>(null)
    val dDayDesc: StateFlow<Int?> = _dDayDesc

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



    fun descDday(): Int? {
        if (identifiedDesc.value.isBlank()) return null

        return try {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val endDate = dateFormat.parse(identifiedDesc.value)
            val startDate = java.util.Date()

            val dDay = ((endDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt()
            _dDayDesc.value = dDay
            dDay
        } catch (e: Exception) {
            Log.e("descDday", "날짜 파싱 실패: ${e.message}")
            null
        }
    }

    fun descFiltering(): Boolean { // userDesc
        if (identifiedDesc.value.isBlank()) return true

        val dDay = descDday()
        return dDay != null && dDay > userDesc
    }


    fun allergyFiltering(): Boolean { // userAllergy
        if (identifiedAllergy.value.isEmpty()) return true

        return identifiedAllergy.value.none { userAllergy.contains(it) }
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