package com.example.foodtap.feature.camera

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse // OcrResponse import
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.util.FileManager
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

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val _dDayExp = MutableStateFlow<Int?>(null)
    val dDayExp: StateFlow<Int?> = _dDayExp

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

    fun setScanningState(isScanning: Boolean) {
        _isScanning.value = isScanning
    }

    // OcrAnalyzer로부터 직접 OcrResponse 객체를 받는 함수
    fun handleProcessedOcrResponse(ocrResponse: OcrResponse?) {
        if (_isScanning.value && ocrResponse != null) {
            val approval = ocrResponse.approval
            val allergy = ocrResponse.allergy
            val desc = ocrResponse.desc
            val expiration = ocrResponse.expiration

            Log.d("CameraViewModel", "Handling processed OCR response: approval=$approval, allergy=$allergy, desc=$desc, expiration=$expiration")

            if (approval) {
                //val temp = _identifiedAllergy.value
                //Log.d("CameraViewModel", "before union allergy=$temp")
                _identifiedAllergy.value = _identifiedAllergy.value.union(allergy).toList()
                //val temp2 = _identifiedAllergy.value
                //Log.d("CameraViewModel", "after union allergy=$temp2")
                if (expiration.isNotEmpty() && _dDayExp.value == null) {
                    _identifiedExpiration.value = expiration
                }

                if (_dDayExp.value == null) {
                    _dDayExp.value = expDday()
                }
                _identifiedDesc.value = desc
                Log.d("CameraViewModel", "Identified exp: ${_identifiedExpiration.value}, Identified dDay: ${dDayExp.value}")

                _showDialog.value = true
                _isScanning.value = false // 성공적으로 정보를 얻으면 스캔 중지
            } else {
                Log.d("CameraViewModel", "Approval denied or incomplete result from processed OCR.")
            }
        } else if (ocrResponse == null) {
            Log.d("CameraViewModel", "Received null OCR response.")
        }
    }

    fun resetScan() {
        _isScanning.value = true
        _showDialog.value = false
        // 다이얼로그 닫을 때 이전 인식 결과 초기화
        //_identifiedAllergy.value = emptyList()
        _identifiedDesc.value = ""
        //_identifiedExpiration.value = ""
        //_dDayExp.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }

    init {
        val context = getApplication<Application>()
        // FileManager 초기화 관련 코드는 필요에 따라 주석 해제하여 사용
        // FileManager.createUserExpIfNotExists(context)
        // FileManager.createUserAllergyIfNotExists(context)
        // userExp = FileManager.loadUserExp(context)
        // userAllergy = FileManager.loadAllergyList(context)
    }

    fun expDday(): Int? {
        // identifiedExpiration.value에 날짜가 "yyyyMMdd" 형식으로 들어와야 함
        if (identifiedExpiration.value.isBlank()) return null

        return try {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
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

    fun expFiltering(ctx : Context): Boolean {
        if (identifiedExpiration.value.isBlank()) return true

        val userExp = FileManager.loadUserData(ctx)?.expi_date

        val dDay = expDday()
        if (userExp != null) {
            return dDay != null && dDay > userExp.toInt()
        }

        return true
    }

    fun allergyFiltering(ctx : Context): Boolean {
        if (identifiedAllergy.value.isEmpty()) return true

        val userAllergy = FileManager.loadUserData(ctx)?.allergy

        if (userAllergy != null) {
            return identifiedAllergy.value.none { userAllergy.contains(it) }
        }

        return true
    }
}