package com.example.foodtap.feature.camera

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope // viewModelScope import 추가
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    // 새로 추가된 타이머 관련 변수
    private val _elapsedTimeMillis = MutableStateFlow(0L) // 경과 시간 (밀리초)
    private var timerJob: Job? = null // 타이머 코루틴 Job

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

    // 카메라 화면 진입 시 호출될 타이머 시작 함수
    fun startScanTimer() {
        stopScanTimer() // 기존 타이머가 있다면 중지
        _elapsedTimeMillis.value = 0L // 타이머 초기화
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                _elapsedTimeMillis.value = System.currentTimeMillis() - startTime
                delay(100) // 0.1초마다 업데이트
            }
        }
    }

    // 카메라 화면 이탈 시 호출될 타이머 정지 함수
    fun stopScanTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTimeMillis.value = 0L // 타이머 초기화
    }

    // OcrAnalyzer로부터 직접 OcrResponse 객체를 받는 함수
    fun handleProcessedOcrResponse(ocrResponse: OcrResponse?) {
        // 이미 팝업이 떠있거나 스캔 중이 아니라면 처리하지 않음
        if (_showDialog.value || !_isScanning.value) {
            return
        }

        if (ocrResponse != null) {
            val approval = ocrResponse.approval
            val allergy = ocrResponse.allergy
            val desc = ocrResponse.desc
            val expiration = ocrResponse.expiration

            Log.d("CameraViewModel", "Handling processed OCR response: approval=$approval, allergy=$allergy, desc=$desc, expiration=$expiration")

            val currentTimeElapsed = _elapsedTimeMillis.value // 현재 경과 시간 가져옴
            val tenSecondsThreshold = 10000L // 10초 (밀리초)

            if (approval) {
                // OCR 결과가 'approval = true'일 경우에만 정보를 업데이트
                _identifiedAllergy.value = _identifiedAllergy.value.union(allergy).toList()
                if (expiration.isNotEmpty() && _dDayExp.value == null) {
                    _identifiedExpiration.value = expiration
                    _dDayExp.value = expDday() // D-day 계산
                }
                _identifiedDesc.value = desc
                Log.d("CameraViewModel", "Identified exp: ${_identifiedExpiration.value}, Identified dDay: ${dDayExp.value}")


                val isSafeCalculated = expFiltering(getApplication()) && allergyFiltering(getApplication())

                if (!isSafeCalculated) {
                    // 안전하지 않은 경우: 즉시 팝업 표시
                    _showDialog.value = true
                    _isScanning.value = false // 팝업이 뜨면 스캔 중지
                    stopScanTimer() // 팝업이 뜨면 타이머 중지
                } else {
                    // 안전한 경우: 10초 경과 후 팝업 표시
                    if (currentTimeElapsed >= tenSecondsThreshold) {
                        _showDialog.value = true
                        _isScanning.value = false // 팝업이 뜨면 스캔 중지
                        stopScanTimer() // 팝업이 뜨면 타이머 중지
                    } else {
                        // 10초가 지나지 않았다면 팝업을 띄우지 않음 (계속 스캔)
                        Log.d("CameraViewModel", "Safe result, but less than 10s elapsed. Not showing dialog yet.")
                    }
                }
            } else {
                Log.d("CameraViewModel", "Approval denied or incomplete result from processed OCR.")
                // approval이 false일 경우, 팝업을 띄우지 않고 계속 스캔을 진행하도록 함
                // _showDialog.value = false; // 불필요, 이미 false일 것이므로
            }
        } else {
            Log.d("CameraViewModel", "Received null OCR response.")
        }
    }


    fun resetScan() {
        _isScanning.value = true // 스캔 재개
        _showDialog.value = false
        // 다이얼로그 닫을 때 이전 인식 결과 초기화
        //_identifiedAllergy.value = emptyList()
        _identifiedDesc.value = ""
        //_identifiedExpiration.value = ""
        //_dDayExp.value = null
        startScanTimer() // 스캔 재개와 함께 타이머 다시 시작
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        stopScanTimer() // ViewModel 소멸 시 타이머 정지
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
        // identifiedExpiration.value에 날짜가 "yyyy.MM.dd" 형식으로 들어와야 함
        if (_identifiedExpiration.value.isBlank()) return null

        return try {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val endDate = dateFormat.parse(_identifiedExpiration.value)
            val startDate = java.util.Date() // 현재 날짜

            val dDay = ((endDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt() + 1
            dDay
        } catch (e: Exception) {
            Log.e("expDday", "날짜 파싱 실패: ${e.message}")
            null
        }
    }

    fun expFiltering(ctx : Context): Boolean {
        // _identifiedExpiration.value가 비어있으면 안전하다고 간주 (필터링 필요 없음)
        if (_identifiedExpiration.value.isBlank()) return true

        val userExp = FileManager.loadUserData(ctx)?.expi_date

        val dDay = expDday()
        if (userExp != null && dDay != null) {
            return dDay > userExp.toInt() // 사용자 설정보다 D-day가 크면 안전 (true)
        }

        return true // 사용자 설정이 없거나, D-day 계산이 안되면 안전하다고 간주
    }

    fun allergyFiltering(ctx : Context): Boolean {
        // _identifiedAllergy.value가 비어있으면 안전하다고 간주 (필터링 필요 없음)
        if (_identifiedAllergy.value.isEmpty()) return true

        val userAllergy = FileManager.loadUserData(ctx)?.allergy

        if (userAllergy != null) {
            // 인식된 알레르기 성분 중 사용자 알레르기 목록에 하나라도 포함되면 안전하지 않음 (false)
            return _identifiedAllergy.value.none { userAllergy.contains(it) }
        }

        return true // 사용자 설정이 없으면 안전하다고 간주
    }
}