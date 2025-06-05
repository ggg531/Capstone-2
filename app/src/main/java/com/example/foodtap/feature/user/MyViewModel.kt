package com.example.foodtap.feature.user

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodtap.api.ExpiData
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.UserData
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Locale

class MyViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val _isTtsDone = MutableStateFlow(false)
    val isTtsDone: StateFlow<Boolean> = _isTtsDone

    private val _userExp = MutableStateFlow(5)
    val userExp: StateFlow<Int> = _userExp

    private val _userAllergy = MutableStateFlow<List<String>>(emptyList())
    val userAllergy: StateFlow<List<String>> = _userAllergy

    init {
        val context = getApplication<Application>()
        loadUserPreferences(context)
    }

    private fun loadUserPreferences(context: Context) {
        viewModelScope.launch {
            val expFile = File(context.filesDir, "user_exp.txt")
            val allergyFile = File(context.filesDir, "user_allergy.txt")

            if (expFile.exists()) {
                _userExp.value = expFile.readText().toIntOrNull() ?: 5
            }

            if (allergyFile.exists()) {
                _userAllergy.value = allergyFile.readText()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        }
    }

    fun speak(text: String, utteranceId: String = "my") {
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

    fun speakWithCallback(text: String, utteranceId: String = "exp_done") {
        if (isTtsInitialized) {
            _isTtsDone.value = false
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    _isTtsDone.value = true
                }

                override fun onError(utteranceId: String?) {}
            })
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun confirmResult(newExpDate: Int, onResult: (Boolean) -> Unit = {}) {
        //         if (_processedAllergyList.value.isNotEmpty()) {

        val userId = FileManager.getOrCreateId(getApplication()) // 사용자 ID
        val request = ExpiData(expi = newExpDate.toString())

        Log.d(
            "API_CALL_PUT_EXPI",
            "Requesting putExpirDate for user: $userId with expi: $newExpDate"
        )
        RetrofitClient.put_expi.putExpirDate(userId, request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    // 로컬 파일에도 변경된 소비기한 정보 업데이트
                    val currentLocalUserData = FileManager.loadUserData(getApplication())
                    val updatedUserData = UserData(
                        id = userId,
                        allergy = currentLocalUserData?.allergy ?: "", // 기존 알레르기 성분 유지
                        expi_date = newExpDate.toString() // 소비기한 변경 결과 저장
                    )
                    FileManager.saveUserData(getApplication(), updatedUserData)
                    Log.d("MyViewModel", "Local UserData updated with new expi.")
                    onResult(true)
                } else {
                    Log.e(
                        "API_PUT_EXPI_ERROR",
                        "Failed to save expi. Code: ${response.code()}, Message: ${response.message()}"
                    )
                    onResult(true)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e(
                    "API_PUT_EXPI_FAILURE",
                    "Network failure while saving expi: ${t.message}",
                    t
                )
                onResult(false)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }
}