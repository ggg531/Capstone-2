package com.example.foodtap.feature.camera

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope // viewModelScope import 추가
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse
import com.example.foodtap.api.ProductNameRequest
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.UserHist
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

    private val _identifiedProductName = MutableStateFlow("")
    val identifiedProductName: StateFlow<String> = _identifiedProductName

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

    private val _isSafeCalculated = MutableStateFlow(false)
    val isSafeCalculated: StateFlow<Boolean> = _isSafeCalculated

    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val _elapsedTimeMillis = MutableStateFlow(0L) // 경과 시간 (밀리초)
    private var timerJob: Job? = null // 타이머 코루틴 Job
    private var hapticJob: Job? = null

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

    fun setScanningState(isScanning: Boolean) { //
        _isScanning.value = isScanning
    }

    // 타이머 시작 (카메라 화면 진입 시 호출)
    fun startScanTimer() {
        stopScanTimer() // 기존 타이머 중지
        _elapsedTimeMillis.value = 0L // 타이머 초기화
        val context = getApplication<Application>()

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                _elapsedTimeMillis.value = System.currentTimeMillis() - startTime
                delay(100) // 0.1초마다 업데이트
            }
        }

        hapticJob = viewModelScope.launch {
            while (true) {
                vibrateOnce(context)
                delay(3000) // 1초마다 햅틱
            }
        }
    }

    // 타이머 정지 (카메라 화면 이탈 시 호출)
    fun stopScanTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTimeMillis.value = 0L

        hapticJob?.cancel()
        hapticJob = null
    }

    private fun vibrateOnce(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(150, 200) // 150ms, 강도 200
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }

    // OcrAnalyzer로부터 직접 OcrResponse 객체를 받는 함수
    fun handleProcessedOcrResponse(ocrResponse: OcrResponse?) {
        // 이미 팝업 표시 O or 스캔 중 X => 처리 X
        if (_showDialog.value || !_isScanning.value) {
            return
        }

        if (ocrResponse != null) {
            val approval = ocrResponse.approval
            val allergy = ocrResponse.allergy
            val desc = ocrResponse.desc
            val expiration = ocrResponse.expiration
            val product_name = ocrResponse.product_name

            Log.d("CameraViewModel", "Handling processed OCR response: approval=$approval, allergy=$allergy, desc=$desc, expiration=$expiration, p_name=$product_name")

            val currentTimeElapsed = _elapsedTimeMillis.value // 현재 경과 시간
            val tenSecondsThreshold = 10000L //


            if (approval) { // 식품 정보 업데이트
                _identifiedAllergy.value = _identifiedAllergy.value.union(allergy).toList() // 알레르기 성분 리스트
                if (expiration.isNotEmpty() && _dDayExp.value == null) {
                    _identifiedExpiration.value = expiration // 소비기한
                    _dDayExp.value = expDday() // D-day 계산
                }
                _identifiedDesc.value = desc // 상세 정보 설명
                Log.d("CameraViewModel", "Identified exp: ${_identifiedExpiration.value}, Identified dDay: ${dDayExp.value}")

                if (_identifiedProductName.value.isBlank()) {
                    _identifiedProductName.value = product_name // 식품명
                }

                Log.d("CameraViewModel", "Identified product name: ${_identifiedProductName.value}")

                var userHistList : List<UserHist>? = null
                if (product_name.isNotBlank()) {
                    val userId = FileManager.loadUserData(getApplication())?.id
                    val productNameRequest = ProductNameRequest(product_name)

                    if (userId != null) {
                        RetrofitClient.getHist.getHist(userId, productNameRequest) //
                            .enqueue(object : Callback<List<UserHist>> { //
                                override fun onResponse(call: Call<List<UserHist>>, response: Response<List<UserHist>>) {
                                    if (response.isSuccessful) {
                                        userHistList = response.body()
                                        Log.d("MA_TEXT", "userHistList successfully: $userHistList")

                                        if (!userHistList.isNullOrEmpty()) {  // 구매기록이 있는 경우 안전하다고 판단하고 바로 팝업
                                            _isSafeCalculated.value = true
                                            _showDialog.value = true
                                            _isScanning.value = false
                                            stopScanTimer()
                                        }
                                    } else {
                                        Log.e("MA_TEXT", "Failed userHistList: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<List<UserHist>>, t: Throwable) {
                                    Log.e("MA_TEXT", "API call failed: ${t.message}")
                                }
                            })
                    }

                }



                _isSafeCalculated.value = expFiltering(getApplication()) && allergyFiltering(getApplication())

                if (!_isSafeCalculated.value) { // 위험한 경우: 즉시 팝업 표시
                    _showDialog.value = true
                    _isScanning.value = false
                    stopScanTimer()
                } else { // 안전한 경우: 10초 경과 후 팝업 표시
                    if (currentTimeElapsed >= tenSecondsThreshold) {
                        _showDialog.value = true
                        _isScanning.value = false
                        stopScanTimer()
                    } else { // 10초 이내: 팝업 표시 X (계속 스캔)
                        Log.d("CameraViewModel", "Safe result, but less than 10s elapsed. Not showing dialog yet.")
                    }
                }



            } else {  // approval == false: 팝업 표시 X (계속 스캔)
                Log.d("CameraViewModel", "Approval denied or incomplete result from processed OCR.")
            }
        } else { // null
            Log.d("CameraViewModel", "Received null OCR response.")
        }
    }

    // 스캔 초기화 (재시작)
    fun resetScan() {
        _isScanning.value = true
        _showDialog.value = false
        // 이전 인식 결과 초기화
        _identifiedAllergy.value = emptyList()
        _identifiedDesc.value = ""
        _identifiedExpiration.value = ""
        _dDayExp.value = null
        _identifiedProductName.value = ""
        startScanTimer()
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        stopScanTimer() // ViewModel 소멸 시 타이머 정지
    }

    init {
        val context = getApplication<Application>()
        /* (필요시) FileManager 초기화 관련 코드
        FileManager.createUserExpIfNotExists(context)
        FileManager.createUserAllergyIfNotExists(context)
        userExp = FileManager.loadUserExp(context)
        userAllergy = FileManager.loadAllergyList(context)
        */
    }

    fun expDday(): Int? {
        if (_identifiedExpiration.value.isBlank()) return null // 소비기한 인식 X (Dday 계산 X)

        return try {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) //
            val endDate = dateFormat.parse(_identifiedExpiration.value)
            val startDate = java.util.Date() // 현재 날짜

            val dDay = ((endDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt() + 1
            dDay
        } catch (e: Exception) {
            Log.e("expDday", "날짜 파싱 실패: ${e.message}")
            null
        }
    }

    fun expFiltering(context : Context): Boolean {
        if (_identifiedExpiration.value.isBlank()) return true // 안전하다고 간주 (필터링 X)

        val userExp = FileManager.loadUserData(context)?.expi_date
        val dDay = expDday()

        if (userExp != null && dDay != null) {
            // 사용자 설정보다 dDay가 작으면 위험 (false)
            return userExp.toInt() < dDay
        }

        return true
    }

    fun allergyFiltering(context : Context): Boolean {
        if (_identifiedAllergy.value.isEmpty()) return true // 안전하다고 간주 (필터링 X)

        val userAllergy = FileManager.loadUserData(context)?.allergy

        if (userAllergy != null) {
            // 인식된 알레르기 성분이 사용자 알레르기 목록에 하나라도 포함되면 위험 (false)
            return _identifiedAllergy.value.none { userAllergy.contains(it) }
        }

        return true
    }

    fun putUserHist(productName: String, allergy: List<String>, context: Context) {
        val userData = FileManager.loadUserData(context)
        val userHist = userData?.let { UserHist(it.id, allergy.toString(), productName) } //

        if (userHist != null) {
            RetrofitClient.putHist.putHist(userHist) //
                .enqueue(object : Callback<String> { //
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        if (response.isSuccessful) {
                            val message = response.body() //
                            Log.d("MA_TEXT", "Confirmation status: $message") //
                        } else {
                            Log.e("MA_TEXT", "Failed to get confirmation: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Log.e("MA_TEXT", "API call failed: ${t.message}")
                    }
                })
        }
    }
}