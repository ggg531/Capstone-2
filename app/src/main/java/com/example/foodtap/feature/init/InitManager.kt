package com.example.foodtap.feature.init

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * TTS 및 STT 기능의 초기화, 실행, 콜백 처리를 담당하는 클래스.
 * ViewModel에 상태와 결과를 콜백 함수를 통해 전달합니다.
 */

// TTS Utterance ID 정의용 객체
object TtsUtteranceId {
    const val WELCOME = "utterance_welcome"
    const val RESULT = "utterance_result"
    const val ERROR = "utterance_error"
}

class InitManager(
    private val context: Context,
    // ViewModel에서 구현하여 전달할 콜백 함수들
    private val onTtsReadyCallback: () -> Unit,                    // TTS 엔진 초기화 완료 시 호출
    private val onTtsDoneCallback: (utteranceId: String?) -> Unit, // TTS 발화 완료 시 호출
    private val onSttResultCallback: (result: String) -> Unit,     // STT 결과 수신 시 호출
    private val onSttErrorCallback: (error: String) -> Unit,       // STT/TTS 관련 에러 발생 시 호출
    private val onListeningStateChanged: (isListening: Boolean) -> Unit // STT 리스닝 상태 변경 시 호출
) : TextToSpeech.OnInitListener { // TTS 초기화 리스너 인터페이스 구현

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile
    private var isTtsInitialized = false // TTS 초기화 상태 플래그
    private var isRecognizerAvailable = false // STT 사용 가능 여부

    // --- State Flows for direct UI update (optional) ---
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // --- Initialization ---
    init {
        Log.d("InitManager", "InitManager 초기화 시작")
        initializeStt() // STT Recognizer 사용 가능 여부 확인 및 리스너 설정 준비
        initializeTts() // TTS 초기화 시작 (비동기)
    }

    /** TTS 엔진 초기화 시작. 결과는 onInit 콜백으로 전달됨. */
    private fun initializeTts() {
        try {
            Log.d("InitManager", "TTS 초기화 시도...")
            tts = TextToSpeech(context, this) // onInit 콜백 리스너 등록
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.update { true }
                    Log.d("InitManager", "TTS onStart: $utteranceId")
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.update { false }
                    Log.d("InitManager", "TTS onDone: $utteranceId")
                    onTtsDoneCallback(utteranceId)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.update { false }
                    val errorMsg = "TTS 에러 발생 (Utterance: $utteranceId, Code: $errorCode)"
                    Log.e("InitManager", errorMsg)
                    onSttErrorCallback(errorMsg)
                    onTtsDoneCallback(utteranceId) // 완료 콜백 호출 (흐름 제어용)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { onError(utteranceId, TextToSpeech.ERROR) }
            })
        } catch (e: Exception) {
            Log.e("InitManager", "TTS 객체 생성 실패", e)
            isTtsInitialized = false
            onSttErrorCallback("TTS 객체 생성 실패: ${e.message}")
        }
    }

    /** TextToSpeech.OnInitListener 구현 메소드 (TTS 초기화 완료 시 호출됨) */
    override fun onInit(status: Int) {
        Log.d("InitManager", "TTS onInit 호출됨, status: $status")
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("InitManager", "한국어 TTS를 지원하지 않습니다.")
                isTtsInitialized = false
                onSttErrorCallback("한국어 TTS를 지원하지 않습니다.")
            } else {
                Log.d("InitManager", "TTS 초기화 성공 및 언어 지원 확인 (언어: 한국어)")
                isTtsInitialized = true
                onTtsReadyCallback() // ViewModel에 준비 완료 알림
            }
        } else {
            Log.e("InitManager", "TTS 엔진 초기화 실패, status: $status")
            isTtsInitialized = false
            onSttErrorCallback("TTS 엔진 초기화 실패 (Status: $status)")
        }
    }

    /** STT Recognizer 초기화 (객체 생성 및 리스너 설정). */
    private fun initializeStt() {
        Log.d("InitManager", "STT 초기화 시도...")
        // 기존 Recognizer가 있다면 정리 (initializeStt가 여러번 호출될 경우 대비)
        if (speechRecognizer != null) {
            Log.w("InitManager", "기존 SpeechRecognizer가 있어 resetRecognizer 호출")
            resetRecognizer()
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            isRecognizerAvailable = true
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("InitManager", "STT 준비 완료 (onReadyForSpeech)")
                    onListeningStateChanged(true)
                }
                override fun onBeginningOfSpeech() { Log.d("InitManager", "STT 음성 인식 시작 (onBeginningOfSpeech)") }
                override fun onRmsChanged(rmsdB: Float) { /* Need to implement */ }
                override fun onBufferReceived(buffer: ByteArray?) { /* Need to implement */ }
                override fun onEndOfSpeech() { Log.d("InitManager", "STT 음성 인식 종료 감지 (onEndOfSpeech)") }

                override fun onError(errorType: Int) {
                    val errorMessage = getSttErrorMessage(errorType)
                    Log.e("InitManager", "STT 에러 발생: $errorMessage ($errorType)")
                    onSttErrorCallback("STT 에러: $errorMessage") // ViewModel 에러 알림
                    onListeningStateChanged(false) // 리스닝 종료 알림
                    // Client Error 등 특정 에러 시 Recognizer 리셋은 ViewModel에서 판단 후 요청
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val resultText = matches[0]
                        Log.d("InitManager", "STT 결과 수신: $resultText")
                        onSttResultCallback(resultText)
                    } else {
                        Log.w("InitManager", "STT 결과 없음 (onResults)")
                        onSttErrorCallback("STT 결과를 찾을 수 없습니다.") // 결과 없음을 에러로 처리
                    }
                    onListeningStateChanged(false) // 리스닝 종료 알림
                }
                override fun onPartialResults(partialResults: Bundle?) { /* Need to implement */ }
                override fun onEvent(eventType: Int, params: Bundle?) { /* Need to implement */ }
            })
            Log.d("InitManager", "STT Recognizer 생성 및 리스너 설정 완료.")
        } else {
            isRecognizerAvailable = false
            Log.e("InitManager", "STT 서비스 사용 불가.")
            onSttErrorCallback("음성 인식 서비스를 사용할 수 없습니다.")
        }
    }

    /** STT 에러 코드를 사용자 친화적 메시지로 변환. */
    private fun getSttErrorMessage(errorType: Int): String {
        return when (errorType) {
            SpeechRecognizer.ERROR_AUDIO -> "오디오 입력 에러"
            SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러" // 이 에러 발생 시 reset 필요
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족 (RECORD_AUDIO)"
            SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과"
            SpeechRecognizer.ERROR_NO_MATCH -> "일치하는 음성 결과 없음"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식 서비스 사용 중" // 이 에러 발생 시 reset 필요
            SpeechRecognizer.ERROR_SERVER -> "서버 에러"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간 초과"
            else -> "알 수 없는 STT 에러 ($errorType)"
        }
    }

    // --- Public Methods ---

    /** 주어진 텍스트를 TTS로 재생. */
    fun speak(text: String, utteranceId: String) {
        if (!isTtsInitialized) {
            val errorMsg = "TTS가 아직 준비되지 않았습니다."
            Log.w("InitManager", "$errorMsg (speak 호출 시점)")
            onSttErrorCallback(errorMsg)
            onTtsDoneCallback(utteranceId) // 완료 콜백 호출 (흐름 제어용)
            return
        }
        if (_isSpeaking.value && utteranceId != TtsUtteranceId.ERROR) {
            Log.w("InitManager", "이미 말하는 중 ($utteranceId 무시됨)")
            return
        }
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        Log.d("InitManager", "TTS speak 요청: [$utteranceId] '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /** STT 음성 인식 시작. */
    fun startListening() {
        if (!isRecognizerAvailable) {
            onSttErrorCallback("음성 인식 서비스를 사용할 수 없습니다.")
            Log.w("InitManager", "STT 사용 불가하여 시작할 수 없음")
            return
        }
        // Recognizer 객체 null 체크 및 생성 (resetRecognizer 호출 후 대비)
        if (speechRecognizer == null) {
            Log.d("InitManager", "SpeechRecognizer가 null이므로 새로 생성 시도.")
            initializeStt() // 리스너 재설정 포함하여 새로 생성
            if (speechRecognizer == null) {
                onSttErrorCallback("음성 인식기 생성 실패")
                Log.e("InitManager", "STT Recognizer 재생성 실패")
                return
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "듣고 있어요...")
        }
        try {
            speechRecognizer?.startListening(intent)
            Log.d("InitManager", "STT 시작 요청됨 (startListening)")
        } catch (e: Exception) {
            val errorMsg = when(e) {
                is SecurityException -> "RECORD_AUDIO 권한이 필요합니다."
                else -> "STT 시작 중 에러 발생: ${e.message}"
            }
            onSttErrorCallback(errorMsg)
            Log.e("InitManager", "STT 시작 중 에러", e)
            onListeningStateChanged(false) // 시작 실패 시 리스닝 상태 false 알림
        }
    }

    /** STT 음성 인식 강제 중지 요청. */
    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d("InitManager", "STT 중지 요청됨 (stopListening)")
        // 실제 상태 변경은 리스너 콜백에서 처리됨
    }

    /** SpeechRecognizer 인스턴스를 안전하게 종료하고 null로 설정 (ViewModel에서 호출). */
    fun resetRecognizer() {
        Log.w("InitManager", "resetRecognizer 호출됨 - SpeechRecognizer 강제 재설정 시도")
        if (speechRecognizer != null) {
            try {
                // 순서 중요: stop/cancel 후 destroy
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                Log.d("InitManager", "기존 SpeechRecognizer 인스턴스 destroy 완료.")
            } catch (e: Exception) {
                Log.e("InitManager", "SpeechRecognizer destroy 중 에러", e)
            } finally {
                speechRecognizer = null // 참조 제거 필수
            }
        } else {
            Log.d("InitManager", "resetRecognizer 호출 시 speechRecognizer가 이미 null 상태.")
        }
        // 리스닝 상태 변경 알림 (확실히 false 로)
        onListeningStateChanged(false)
    }

    // --- Cleanup ---
    /** TTS, STT 관련 리소스 해제. */
    fun shutdown() {
        Log.d("InitManager", "Shutdown 호출됨 - 리소스 해제 시작")
        // TTS 정리
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        _isSpeaking.update { false }
        Log.d("InitManager", "TTS 리소스 해제 완료")

        // STT 정리
        resetRecognizer() // STT 리소스 정리 및 참조 제거
        isRecognizerAvailable = false // 서비스 자체 사용 불가로 간주
        Log.d("InitManager", "STT 리소스 해제 완료 (shutdown)")
    }
}