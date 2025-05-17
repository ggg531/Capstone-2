package com.example.foodtap.feature.init

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.SttRequest
import com.example.foodtap.api.SttResponse
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

// API 호출 상태를 나타내는 Enum
enum class ApiStatus {
    IDLE, LOADING, SUCCESS, ERROR
}

class InitViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rawSttText = MutableStateFlow("")
    val rawSttText: StateFlow<String> = _rawSttText.asStateFlow()

    // API를 통해 처리된 알레르기 목록 (List<String>)
    private val _processedAllergyList = MutableStateFlow<List<String>>(emptyList())
    val processedAllergyList: StateFlow<List<String>> = _processedAllergyList.asStateFlow()

    // 화면에 표시될 최종 알레르기 텍스트 (API 결과 기반)
    private val _displayAllergyText = MutableStateFlow("")
    val displayAllergyText: StateFlow<String> = _displayAllergyText.asStateFlow()

    private val _registeredAllergyText = MutableStateFlow("") // 이전에 등록 확정된 텍스트
    val registeredAllergyText: StateFlow<String> = _registeredAllergyText.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _apiCallStatus = MutableStateFlow(ApiStatus.IDLE)
    val apiCallStatus: StateFlow<ApiStatus> = _apiCallStatus.asStateFlow()

    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(application.applicationContext)

    private val recognizerIntent: Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 설정
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, application.packageName)
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
        viewModelScope.launch {
            delay(500)
            speak("화면을 탭하여 알레르기 성분을 등록하세요", "starttap")
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                Log.d("STT", "onResults called")
                val resultText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _rawSttText.value = resultText
                Log.d("STT", resultText)
                if (resultText.isNotBlank()) {
                    callStt2AllergyApi(resultText)
                } else { // STT 결과가 비어있을 경우 처리
                    _displayAllergyText.value = "" // 또는 "음성 인식 결과가 없습니다."
                    _processedAllergyList.value = emptyList()
                    _apiCallStatus.value = ApiStatus.ERROR // 또는 다른 적절한 상태
                    _showDialog.value = true // 다이얼로그 표시
                }
            }

            override fun onError(error: Int) {
                _isListening.value = false
                Log.e("STT_ERROR", "Error code: $error")
                _displayAllergyText.value = "음성 인식 중 오류가 발생했습니다."
                _processedAllergyList.value = emptyList()
                _apiCallStatus.value = ApiStatus.ERROR
                _showDialog.value = true
            }

            override fun onEndOfSpeech() {
                _isListening.value = false
                _apiCallStatus.value = ApiStatus.LOADING
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

    private fun callStt2AllergyApi(sttText: String) {
        _apiCallStatus.value = ApiStatus.LOADING
        val request = SttRequest(stt = sttText)
        Log.d("API_CALL", "Requesting stt2Allergy with: $sttText")

        RetrofitClient.stt2Allergy.stt2Allergy(request).enqueue(object : Callback<SttResponse> {
            override fun onResponse(call: Call<SttResponse>, response: Response<SttResponse>) {
                if (response.isSuccessful) {
                    val sttResponse = response.body()
                    Log.d("API_CALL", "$sttResponse")
                    if (sttResponse != null && sttResponse.allergy.isNotEmpty()) {
                        _processedAllergyList.value = sttResponse.allergy //
                        _displayAllergyText.value = sttResponse.allergy.joinToString(", ")
                        _apiCallStatus.value = ApiStatus.SUCCESS
                        Log.d("API_SUCCESS", "Processed allergies: ${sttResponse.allergy}")
                    } else {
                        _displayAllergyText.value = "인식된 알레르기 성분이 없습니다."
                        _processedAllergyList.value = emptyList()
                        _apiCallStatus.value = ApiStatus.SUCCESS // API는 성공했으나 결과가 비었을 수 있음
                        Log.d("API_EMPTY", "No allergies found or empty response body.")
                    }
                } else {
                    _displayAllergyText.value = "알레르기 정보 처리 중 오류가 발생했습니다. (서버 응답 오류)"
                    _processedAllergyList.value = emptyList()
                    _apiCallStatus.value = ApiStatus.ERROR
                    Log.e("API_ERROR", "Server error: ${response.code()} - ${response.message()}")
                }
                _showDialog.value = true // API 호출 완료 후 다이얼로그 표시
            }

            override fun onFailure(call: Call<SttResponse>, t: Throwable) {
                _displayAllergyText.value = "알레르기 정보 처리 중 오류가 발생했습니다. (네트워크 오류)"
                _processedAllergyList.value = emptyList()
                _apiCallStatus.value = ApiStatus.ERROR
                Log.e("API_FAILURE", "Network failure: ${t.message}", t)
                _showDialog.value = true // API 호출 완료 후 다이얼로그 표시
            }
        })
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            _isListening.value = true
            _rawSttText.value = ""
            _displayAllergyText.value = "" // 초기화
            _processedAllergyList.value = emptyList() // 초기화
            _apiCallStatus.value = ApiStatus.IDLE // 상태 초기화
            speechRecognizer.startListening(recognizerIntent)
            viewModelScope.launch {
                delay(300)
                speak("음성 인식 중입니다.", "startListening")
            }
        } else {
            Log.e("STT_UNAVAILABLE", "Speech recognition not available")
        }
    }

    fun stopListening() { // 사용자가 수동으로 중지할 때
        speechRecognizer.stopListening() // STT 중지
        // onResults나 onError가 호출될 것이므로 거기서 _isListening.value = false 처리됨
        // _showDialog.value = true // API 호출 결과에 따라 다이얼로그가 표시되도록 변경
        // _isListening.value = false // onResults/onError에서 관리
    }

    fun tapShowDialog(value: Boolean) {
        _showDialog.value = value
    }

    fun confirmResult() {
        FileManager.saveAllergyList(getApplication(), _processedAllergyList.value)
        _showDialog.value = false
    }

    fun resetResult() {
        _rawSttText.value = ""
        _displayAllergyText.value = ""
        _processedAllergyList.value = emptyList()
        _apiCallStatus.value = ApiStatus.IDLE
        _showDialog.value = false
        viewModelScope.launch {
            delay(500)
            speak("화면을 탭하여 알레르기 성분을 등록하세요", "starttap")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}