package com.example.foodtap.feature.init

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtap.feature.init.InitManager
import com.example.foodtap.feature.init.TtsUtteranceId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// import kotlinx.coroutines.delay // 딜레이 사용 시 필요

// 어플리케이션의 현재 상태를 나타내는 Enum
enum class InitFlowState {
    IDLE,                // 초기 상태 또는 완료 후 유휴 상태
    INITIALIZING,        // TTS 등 초기화 진행 중 상태
    SPEAKING_WELCOME,    // 환영 메시지 TTS 중
    REQUESTING_PERMISSION, // STT 시작 전 권한 요청 필요 상태
    LISTENING,           // STT 진행 중
    PROCESSING_RESULT,   // STT 결과 처리 및 결과 TTS 준비 중
    SPEAKING_RESULT,     // STT 결과 TTS 중
    WAITING_USER_INTERACTION, // 결과 안내 후 사용자 탭 대기 상태
    ERROR                // 오류 발생 상태
}

// UI 상태 데이터 클래스
data class InitUiState(
    val welcomeText: String = "환영합니다.",
    val flowState: InitFlowState = InitFlowState.INITIALIZING,
    val recognizedText: String? = null,
    val displayText: String = "초기화 중...",
    val interactionHint: String = "",
    val errorMessage: String? = null,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false
)

class InitViewModel(application: Application) : AndroidViewModel(application) {

    // --- State ---
    private val _uiState = MutableStateFlow(InitUiState())
    val uiState: StateFlow<InitUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    @Volatile private var isTtsReady = false

    private val initManager = InitManager(
        context = application.applicationContext,
        onTtsReadyCallback = ::handleTtsReady,
        onTtsDoneCallback = ::handleTtsDone,
        onSttResultCallback = ::handleSttResult,
        onSttErrorCallback = ::handleSttError,
        onListeningStateChanged = ::handleListeningStateChange
    )

    // --- Manager Callback Handlers ---

    /** TTS 엔진 준비 완료 시 InitManager 에서 호출 */
    private fun handleTtsReady() {
        Log.d("InitViewModel", "handleTtsReady 호출됨")
        if (!isTtsReady) {
            isTtsReady = true
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        flowState = InitFlowState.IDLE,
                        displayText = it.welcomeText,
                        interactionHint = "자동으로 시작합니다..."
                    )
                }
                startAutoFlow()
            }
        }
    }

    /** TTS 발화 완료 시 InitManager 에서 호출 */
    private fun handleTtsDone(utteranceId: String?) {
        Log.d("InitViewModel", "handleTtsDone: $utteranceId, Current state: ${_uiState.value.flowState}")
        viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = false) } // isSpeaking 먼저 false 로

            when (utteranceId) {
                TtsUtteranceId.WELCOME -> {
                    if (_uiState.value.flowState == InitFlowState.SPEAKING_WELCOME) {
                        checkPermissionAndStartListening()
                    }
                }
                TtsUtteranceId.RESULT -> {
                    if (_uiState.value.flowState == InitFlowState.SPEAKING_RESULT) {
                        _uiState.update {
                            it.copy(
                                flowState = InitFlowState.WAITING_USER_INTERACTION,
                                displayText = it.recognizedText ?: "결과 없음",
                                interactionHint = "탭하여 다시 질문하거나, 더블 탭하여 다음 단계로 이동하세요."
                            )
                        }
                    }
                }
                TtsUtteranceId.ERROR -> {
                    if (_uiState.value.flowState == InitFlowState.ERROR) {
                        _uiState.update {
                            it.copy(
                                flowState = InitFlowState.WAITING_USER_INTERACTION,
                                interactionHint = "오류 발생. 탭하여 다시 시도하거나 더블 탭하세요."
                            )
                        }
                    }
                }
                else -> { // TTS 준비 안됨 에러 등으로 speak 호출 실패 시 여기로 올 수 있음
                    Log.w("InitViewModel", "TTS 완료 콜백 처리 중 예상치 못한 utteranceId 또는 상태: $utteranceId, State: ${_uiState.value.flowState}")
                    // 현재 상태에 따라 안전한 상태로 복구 시도
                    if (_uiState.value.flowState == InitFlowState.SPEAKING_WELCOME || _uiState.value.flowState == InitFlowState.SPEAKING_RESULT || _uiState.value.flowState == InitFlowState.ERROR) {
                        // 말하는 중 또는 에러 상태였다면 사용자 대기 상태로 전환
                        handleSttError("TTS 발화 중 예상치 못한 완료 ($utteranceId)") // 에러 처리 함수 재활용
                    }
                }
            }
        }
    }

    /** STT 결과 수신 시 InitManager 에서 호출 */
    private fun handleSttResult(result: String) {
        Log.d("InitViewModel", "handleSttResult: '$result', Current state: ${_uiState.value.flowState}")
        viewModelScope.launch {
            if (_uiState.value.flowState == InitFlowState.LISTENING) {
                val resultTextToSpeak = "$result 라고 말씀하셨습니다."
                _uiState.update {
                    it.copy(
                        flowState = InitFlowState.PROCESSING_RESULT,
                        recognizedText = result,
                        isListening = false, // STT 종료됨
                        displayText = "결과: \"$result\"", // 화면 결과 업데이트
                        interactionHint = "결과를 읽어줍니다..."
                    )
                }
                speakResult(resultTextToSpeak) // 결과 TTS 시작
            }
        }
    }

    /** STT/TTS 에러 발생 시 InitManager 에서 호출 */
    private fun handleSttError(error: String) {
        Log.e("InitViewModel", "handleSttError: $error, Current state: ${_uiState.value.flowState}")
        viewModelScope.launch {
            var resetNeeded = false // Recognizer 리셋 필요 여부

            // 에러 메시지 분석하여 리셋 필요 여부 결정
            if (error.contains("클라이언트 에러") || error.contains("Recognizer busy")) {
                Log.w("InitViewModel", "클라이언트 또는 Recognizer Busy 에러 감지. Recognizer 리셋 예정.")
                resetNeeded = true
            }

            // 상태 업데이트 로직
            val newState = when {
                error.contains("결과 없음") || error.contains("음성 입력 시간 초과") -> {
                    _uiState.value.copy(
                        flowState = InitFlowState.WAITING_USER_INTERACTION,
                        errorMessage = error,
                        interactionHint = "$error\n탭하여 다시 시도하거나 더블 탭하세요."
                    )
                }
                error.contains("권한") -> {
                    _uiState.value.copy(
                        flowState = InitFlowState.IDLE,
                        errorMessage = error,
                        interactionHint = "마이크 권한 설정 후 다시 시도해주세요."
                    )
                }
                else -> { // 그 외 일반적인 에러 (클라이언트 에러 포함)
                    _uiState.value.copy(
                        flowState = InitFlowState.ERROR,
                        errorMessage = error,
                        displayText = "오류 발생",
                        interactionHint = "오류가 발생했습니다. 탭하여 다시 시도하거나 더블 탭하세요."
                    )
                }
            }
            // isListening, isSpeaking 상태는 항상 false 로 업데이트
            _uiState.value = newState.copy(isListening = false, isSpeaking = false)
            Log.d("InitViewModel", "State updated after error: ${_uiState.value.flowState}")


            // 리셋 필요 시 InitManager의 resetRecognizer 호출
            if (resetNeeded) {
                initManager.resetRecognizer()
            }
        }
    }

    /** STT 리스닝 상태 변경 시 InitManager 에서 호출 */
    private fun handleListeningStateChange(isListening: Boolean) {
        Log.d("InitViewModel", "handleListeningStateChange: $isListening, Current state: ${_uiState.value.flowState}")
        viewModelScope.launch {
            // 리스닝 상태 플래그 업데이트
            _uiState.update { it.copy(isListening = isListening) }

            // 리스닝 시작 시 상태 전환 (isListening = true)
            if (isListening &&
                (_uiState.value.flowState == InitFlowState.REQUESTING_PERMISSION ||
                        _uiState.value.flowState == InitFlowState.IDLE ||
                        _uiState.value.flowState == InitFlowState.WAITING_USER_INTERACTION ||
                        _uiState.value.flowState == InitFlowState.ERROR) // 에러 상태에서도 바로 리스닝 가능하게
            ) {
                _uiState.update {
                    it.copy(
                        flowState = InitFlowState.LISTENING,
                        errorMessage = null, // 에러 메시지 클리어
                        displayText = "듣고 있어요...",
                        interactionHint = ""
                    )
                }
            }
            // isListening 이 false 가 되는 경우는 onResult, onError 에서 flowState 를 이미 변경함
        }
    }

    // --- Public Actions from UI ---

    /** 자동 흐름 시작 (TTS 준비 완료 후 내부적으로 호출됨) */
    private fun startAutoFlow() {
        if (_uiState.value.flowState == InitFlowState.IDLE && isTtsReady) {
            Log.d("InitViewModel", "startAutoFlow 실행")
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        flowState = InitFlowState.SPEAKING_WELCOME,
                        errorMessage = null,
                        isSpeaking = true,
                        displayText = it.welcomeText,
                        interactionHint = "환영 메시지를 재생합니다."
                    )
                }
                initManager.speak(_uiState.value.welcomeText, TtsUtteranceId.WELCOME)
            }
        } else {
            Log.w("InitViewModel", "startAutoFlow 실행 조건 미충족 (State: ${_uiState.value.flowState}, TtsReady: $isTtsReady)")
        }
    }

    /** 권한 요청 결과 처리 (UI 에서 호출) */
    fun onPermissionResult(granted: Boolean) {
        Log.d("InitViewModel", "onPermissionResult: $granted, Current state: ${_uiState.value.flowState}")
        if (_uiState.value.flowState == InitFlowState.REQUESTING_PERMISSION) {
            viewModelScope.launch { // 상태 업데이트 및 매니저 호출은 코루틴에서
                if (granted) {
                    Log.d("InitViewModel", "권한 허용됨, STT 시작 요청")
                    _uiState.update {
                        it.copy(
                            displayText = "마이크 사용이 허용되었습니다.",
                            interactionHint = "듣기 시작합니다..."
                            // flowState 는 handleListeningStateChange 에서 LISTENING 으로 변경될 것임
                        )
                    }
                    initManager.startListening()
                } else {
                    Log.w("InitViewModel", "권한 거부됨")
                    // 권한 거부 시 에러 처리
                    handleSttError("마이크 권한이 거부되었습니다.")
                }
            }
        }
    }

    /** 화면 싱글 탭 시 호출 (UI 에서 호출) */
    fun onSingleTap() {
        Log.d("InitViewModel", "onSingleTap, Current state: ${_uiState.value.flowState}")
        val currentState = _uiState.value.flowState
        if (currentState == InitFlowState.WAITING_USER_INTERACTION || currentState == InitFlowState.ERROR || currentState == InitFlowState.IDLE) {
            Log.d("InitViewModel", "싱글 탭으로 다시 듣기 시작")
            // (선택적 딜레이)
            // viewModelScope.launch {
            //    if (currentState == InitFlowState.ERROR) delay(300)
            //    checkPermissionAndStartListening()
            // }
            checkPermissionAndStartListening()
        } else {
            Log.d("InitViewModel", "싱글 탭 무시됨 (현재 상태: $currentState)")
        }
    }

    /** 화면 더블 탭 시 호출 (UI 에서 호출) */
    fun onDoubleTap() {
        Log.d("InitViewModel", "onDoubleTap, Current state: ${_uiState.value.flowState}")
        val currentState = _uiState.value.flowState
        if (currentState == InitFlowState.WAITING_USER_INTERACTION || currentState == InitFlowState.ERROR || currentState == InitFlowState.IDLE) {
            viewModelScope.launch {
                Log.d("InitViewModel", "더블 탭으로 네비게이션 이벤트 발생")
                _navigationEvent.emit(Unit)
            }
        } else {
            Log.d("InitViewModel", "더블 탭 무시됨 (현재 상태: $currentState)")
        }
    }

    // --- Internal Helpers ---

    /** 마이크 권한 확인 후 STT 시작 또는 권한 요청 상태 변경 */
    private fun checkPermissionAndStartListening() {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Log.d("InitViewModel", "권한 있음, STT 시작 요청")
                _uiState.update {
                    it.copy(
                        errorMessage = null, // 이전 에러 지우기
                        displayText = "듣기 시작합니다...",
                        interactionHint = ""
                        // flowState는 handleListeningStateChange에서 LISTENING으로 변경
                    )
                }
                initManager.startListening()
            } else {
                Log.d("InitViewModel", "권한 없음, UI에 요청 상태 알림")
                _uiState.update {
                    it.copy(
                        flowState = InitFlowState.REQUESTING_PERMISSION,
                        errorMessage = null,
                        displayText = "마이크 권한 필요",
                        interactionHint = "음성 인식을 위해 마이크 권한이 필요합니다."
                    )
                }
            }
        }
    }

    /** STT 결과 텍스트를 TTS로 재생 */
    private fun speakResult(resultTextToSpeak: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    flowState = InitFlowState.SPEAKING_RESULT,
                    isSpeaking = true,
                    // displayText 는 handleSttResult 에서 이미 설정됨
                    interactionHint = "결과를 읽어줍니다..."
                )
            }
            initManager.speak(resultTextToSpeak, TtsUtteranceId.RESULT)
        }
    }

    /** 에러 메시지를 TTS로 재생 (선택적 기능) */
    private fun speakError(errorText: String) {
        viewModelScope.launch {
            // speakError 를 호출하기 전에 이미 상태는 ERROR 또는 WAITING_USER_INTERACTION 일 것임
            _uiState.update { it.copy(isSpeaking = true) }
            initManager.speak("오류: $errorText", TtsUtteranceId.ERROR)
        }
    }

    // --- Lifecycle ---
    override fun onCleared() {
        super.onCleared()
        initManager.shutdown()
        Log.d("InitViewModel", "onCleared 호출, InitManager shutdown")
    }
}