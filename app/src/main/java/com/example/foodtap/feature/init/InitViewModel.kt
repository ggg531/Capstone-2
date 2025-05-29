package com.example.foodtap.feature.init

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtap.api.ConfirmData
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.SttRequest
import com.example.foodtap.api.SttResponse
import com.example.foodtap.api.UserData
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
    IDLE, // 초기 상태
    LOADING,
    SUCCESS,
    ERROR
}

enum class ConfirmApiStatus {
    IDLE, // 초기 상태
    SUCCESS,
    ERROR
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

    private val _isConfirmListening = MutableStateFlow(false)
    val isConfirmListening: StateFlow<Boolean> = _isConfirmListening.asStateFlow()

    private val _rawSttConfirmText = MutableStateFlow("")
    val rawSttConfirmText: StateFlow<String> = _rawSttConfirmText.asStateFlow()

    private val _apiCallStatus = MutableStateFlow(ApiStatus.IDLE)
    val apiCallStatus: StateFlow<ApiStatus> = _apiCallStatus.asStateFlow()

    private val _confirmApiStatus = MutableStateFlow(ConfirmApiStatus.IDLE)
    val confirmApiStatus: StateFlow<ConfirmApiStatus> = _confirmApiStatus.asStateFlow()

    // 알레르기 저장 API 호출 상태
    private val _saveAllergyStatus = MutableStateFlow(ApiStatus.IDLE)
    val saveAllergyStatus: StateFlow<ApiStatus> = _saveAllergyStatus.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    //private var tts: TextToSpeech = TextToSpeech(application, this)
    private val tts: TextToSpeech = TextToSpeech(application) { status ->
        isTtsInitialized = (status == TextToSpeech.SUCCESS)
    }
    private var isTtsInitialized = false

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(application.applicationContext)
    private val recognizerIntent: Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, application.packageName)
        }

    private var confirmSpeechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(application.applicationContext)
    private val confirmRecognizerIntent: Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
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
                    callStt2AllergyApi(resultText) // 제공하는 알레르기 성분 키워드로 변경
                } else { //
                    _displayAllergyText.value = "" // "음성 인식 결과가 없습니다."
                    _processedAllergyList.value = emptyList()
                    _apiCallStatus.value = ApiStatus.ERROR //
                    _showDialog.value = true
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

        confirmSpeechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                _isConfirmListening.value = false
                Log.d("STT_CONFIRM", "onResults called")

                val confirmresultText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                _rawSttConfirmText.value = confirmresultText
                Log.d("STT_CONFIRM", confirmresultText)

                if (confirmresultText.isNotBlank()) {
                    sendSttConfirmRequest(confirmresultText) // 등록 여부 반환
                } else {
                    //_apiCallStatus.value = ApiStatus.ERROR
                    //_confirmApiStatus.value = confirmApiStatus.ERROR

                }
            }

            override fun onError(error: Int) {
                _isConfirmListening.value = false
                Log.e("STT_CONFIRM_ERROR", "Error code: $error")
                //_apiCallStatus.value = ApiStatus.ERROR
                //_confirmApiStatus.value = confirmApiStatus.ERROR
            }

            override fun onEndOfSpeech() {
                _isConfirmListening.value = false
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
                        _processedAllergyList.value = sttResponse.allergy // 알레르기 성분 키워드 저장
                        _displayAllergyText.value = sttResponse.allergy.joinToString(", ")
                        _apiCallStatus.value = ApiStatus.SUCCESS
                        Log.d("API_SUCCESS", "Processed allergies: ${sttResponse.allergy}")
                    } else { // API는 성공 / 결과가 빈 경우
                        _displayAllergyText.value = "인식된 알레르기 성분이 없습니다."
                        _processedAllergyList.value = emptyList()
                        _apiCallStatus.value = ApiStatus.SUCCESS
                        Log.d("API_EMPTY", "No allergies found or empty response body.")
                    }
                } else {
                    _displayAllergyText.value = "알레르기 정보 처리 중 오류가 발생했습니다. (서버 응답 오류)"
                    _processedAllergyList.value = emptyList()
                    _apiCallStatus.value = ApiStatus.ERROR
                    Log.e("API_ERROR", "Server error: ${response.code()} - ${response.message()}")
                }
                _showDialog.value = true
            }

            override fun onFailure(call: Call<SttResponse>, t: Throwable) {
                _displayAllergyText.value = "알레르기 정보 처리 중 오류가 발생했습니다. (네트워크 오류)"
                _processedAllergyList.value = emptyList()
                _apiCallStatus.value = ApiStatus.ERROR
                Log.e("API_FAILURE", "Network failure: ${t.message}", t)
                _showDialog.value = true
            }
        })
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            _isListening.value = true
            _rawSttText.value = ""
            _displayAllergyText.value = ""
            _processedAllergyList.value = emptyList()
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

    fun confirmResult() {
        if (_processedAllergyList.value.isNotEmpty()) {
            _saveAllergyStatus.value = ApiStatus.LOADING
            val userId = FileManager.getOrCreateId(getApplication()) // 사용자 ID
            val allergyListToSave = _processedAllergyList.value
            val request = SttResponse(allergy = allergyListToSave)

            Log.d("API_CALL_PUT_ALLERGY", "Requesting putAllergy for user: $userId with allergies: $allergyListToSave")
            RetrofitClient.put_allergy.putAllergy(userId, request).enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        // 로컬 파일에도 변경된 알레르기 정보 업데이트
                        val currentLocalUserData = FileManager.loadUserData(getApplication())
                        val updatedUserData = UserData(
                            id = userId,
                            allergy = allergyListToSave.joinToString(","), // 쉼표로 구분된 문자열로 저장
                            expi_date = currentLocalUserData?.expi_date ?: "5" // 기존 소비기한 유지
                        )
                        FileManager.saveUserData(getApplication(), updatedUserData)
                        Log.d("InitViewModel", "Local UserData updated with new allergies.")
                        _saveAllergyStatus.value = ApiStatus.SUCCESS
                    } else {
                        Log.e("API_PUT_ALLERGY_ERROR", "Failed to save allergies. Code: ${response.code()}, Message: ${response.message()}")
                        _saveAllergyStatus.value = ApiStatus.ERROR
                        // ApiStatus.ERROR에 대한 별도 UI 반응 없음 (에러 처리 X)
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("API_PUT_ALLERGY_FAILURE", "Network failure while saving allergies: ${t.message}", t)
                    _saveAllergyStatus.value = ApiStatus.ERROR
                    // ApiStatus.ERROR에 대한 별도 UI 반응 없음 (에러 처리 X)
                }
            })
        } else { // 저장할 알레르기가 없는 경우, 바로 성공으로 간주
            Log.w("API_PUT_ALLERGY_SKIP", "No allergies to save.")
            _saveAllergyStatus.value = ApiStatus.SUCCESS // SUCCESS => "my"로 이동 O
        }
        _showDialog.value = false
    }

    fun resetResult() {
        _rawSttText.value = ""
        _displayAllergyText.value = ""
        _processedAllergyList.value = emptyList()
        _apiCallStatus.value = ApiStatus.IDLE
        _showDialog.value = false
        viewModelScope.launch {
            delay(500) //
            speak("화면을 탭하여 알레르기 성분을 등록하세요", "starttap")
        }
    }

//
    fun startConfirmListening() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            _isConfirmListening.value = true
            _rawSttConfirmText.value = ""
            //_confirmApiStatus.value = confirmApiStatus.IDLE // 상태 초기화
            confirmSpeechRecognizer.startListening(confirmRecognizerIntent)
            viewModelScope.launch {
                //speak("등록?", "startListening")
            }
        } else {
            Log.e("STT_UNAVAILABLE", "Speech recognition not available")
        }
    }

//
    fun speakConfirmListening(text: String) {
        if (!isTtsInitialized) return
        tts.stop()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "speakConfirmListening: $utteranceId")
                viewModelScope.launch {
                    //delay(300)
                    startConfirmListening()
                }
            }

            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speakConfirm")
    }

//
    private fun sendSttConfirmRequest(sttConfirmText: String) {
        val sttRequest = SttRequest(stt = sttConfirmText)

        RetrofitClient.get_confirm.getConfirm(sttRequest)
            .enqueue(object : Callback<ConfirmData> {
                override fun onResponse(call: Call<ConfirmData>, response: Response<ConfirmData>) {
                    if (response.isSuccessful) {
                        val confirmData = response.body()
                        confirmData?.let {
                            Log.d("STT_CONFIRM", "Confirmation status: ${it.confirm}")
                            if (it.confirm) {
                                confirmResult()
                            }
                        }
                    } else {
                        Log.e("STT_CONFIRM", "Failed to get confirmation: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ConfirmData>, t: Throwable) {
                    Log.e("STT_CONFIRM",  "API call failed: ${t.message}")
                }
            })
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}