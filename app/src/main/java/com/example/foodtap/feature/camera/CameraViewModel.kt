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
import java.util.Locale

class CameraViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _ocrTextList = mutableListOf<String>()

    private val _nutritionText = MutableStateFlow("")
    val nutritionText: StateFlow<String> = _nutritionText

    private val _expiryText = MutableStateFlow("")
    val expiryText: StateFlow<String> = _expiryText

    private val _identifiedAllergy = MutableStateFlow<List<String>>(emptyList())
    val identifiedAllergy: StateFlow<List<String>> = _identifiedAllergy

    private val _identifiedDesc = MutableStateFlow("")
    val identifiedDesc: StateFlow<String> = _identifiedDesc

    private val _identifiedExpiration = MutableStateFlow("")
    val identifiedExpiration: StateFlow<String> = _identifiedExpiration

    private val _dDayExp = MutableStateFlow<Int?>(null)
    val dDayExp: StateFlow<Int?> = _dDayExp

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

    fun clovaAnalyze(inferTextLines: String) {
        if (_isScanning.value) {  // 스캔 중일 때만 처리
            // 받은 텍스트 라인들을 CameraScreen의 상태에 반영
            _ocrTextList.clear()
            _ocrTextList.addAll(listOf(inferTextLines))

            if (_ocrTextList.isNotEmpty()) {

                Log.d("CameraScreen", "Triggering external approval API call with: $_ocrTextList")

                // 외부 API 호출 및 결과 처리
                val request = OcrRequest(ocr = _ocrTextList.toString()) // OcrRequest 데이터 클래스 필요
                RetrofitClient.get_approval.getApproval(request).enqueue(object : Callback<OcrResponse> {
                    override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                        if (response.isSuccessful) {
                            val approval = response.body()?.approval == true
                            val allergy = response.body()?.allergy
                            val desc = response.body()?.desc
                            val expiration = response.body()?.expiration

                            Log.d("API", "approval: $approval, allergy: $allergy, desc: $desc, expiration: $expiration")

                            // 외부 API 응답 결과에 따라 UI 상태 업데이트
                            if (approval &&  desc != null) {
                                if (!allergy.isNullOrEmpty() && !_identifiedAllergy.value.containsAll(allergy)) {
                                    _identifiedDesc.value += desc
                                }

                                if (allergy != null) {
                                    _identifiedAllergy.value = _identifiedAllergy.value.union(allergy).toList()
                                }
                                if (!expiration.isNullOrEmpty()) {
                                    _identifiedExpiration.value = expiration
                                }

                                _showDialog.value = true // 다이얼로그 표시
                                _isScanning.value = false // 스캔 중지
                            } else {
                                // 승인되지 않거나 결과가 불완전한 경우 처리
                                Log.d("API", "Approval denied or incomplete result.")
                                // 필요에 따라 사용자에게 알리거나 스캔을 계속할 수 있습니다.
                            }

                        } else {
                            Log.e("API", "서버 응답 오류: ${response.code()}")
                            // 오류 처리 (사용자에게 알림 등)
                        }
                        // API 호출 완료 후 필요한 상태 정리 (예: ocrTextList.clear() 등)
                        // ocrTextList.clear() // 다음 프레임의 결과를 받기 위해 클리어
                    }

                    override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                        Log.e("API", "요청 실패: ${t.message}", t)
                        // 오류 처리 (사용자에게 알림 등)
                        // API 호출 완료 후 필요한 상태 정리
                        // ocrTextList.clear()
                    }
                })

                // 주의: API 호출 후 ocrTextList를 바로 clear하면 다음 프레임 결과와 섞이지 않지만,
                // 여러 프레임에 걸친 텍스트를 모아서 분석해야 한다면 clear 시점을 조절해야 합니다.

            } else {
                // 추출된 영양 정보나 유통기한이 없는 경우
                Log.d("CameraScreen", "No significant text found for processing.")
                // 필요에 따라 사용자에게 알리거나 스캔을 계속합니다.
                // ocrTextList.clear() // 다음 프레임 결과를 위해 클리어
            }
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
                            _nutritionText.value = nutrition
                            _expiryText.value = expiry
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

    private val UserExp = 5
    private val userAllergy = listOf("우유", "대두")

    fun expDday(): Int? {
        if (identifiedExpiration.value.isBlank()) return null

        return try {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val endDate = dateFormat.parse(identifiedExpiration.value)
            val startDate = java.util.Date()

            val dDay = ((endDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt()
            _dDayExp.value = dDay
            dDay
        } catch (e: Exception) {
            Log.e("expDday", "날짜 파싱 실패: ${e.message}")
            null
        }
    }

    fun expFiltering(): Boolean { // UserExp
        if (identifiedExpiration.value.isBlank()) return true

        val dDay = expDday()
        return dDay != null && dDay > UserExp
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