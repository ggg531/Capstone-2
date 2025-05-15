package com.example.foodtap.feature.auth

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.foodtap.api.RetrofitClient
// UserData는 현재 ViewModel에서 직접 사용하지 않지만, RetrofitClient가 참조할 수 있음
// import com.example.foodtap.api.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

// 사용자 상태를 나타내는 Enum 클래스
enum class UserStatus {
    UNKNOWN, // 초기 상태
    NEW_USER,
    EXISTING_USER,
    ERROR
}

class SigninViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(application, this)
    private var isTtsInitialized = false

    private val _userStatus = MutableLiveData<UserStatus>(UserStatus.UNKNOWN)
    val userStatus: LiveData<UserStatus> = _userStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isTtsInitialized = true
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    fun speak(text: String, utteranceId: String = "signin_speech") {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun checkUserStatus(userId: String) {
        _isLoading.value = true
        _userStatus.value = UserStatus.UNKNOWN // 상태 초기화
        Log.d("SigninViewModel", "Checking user status for ID: $userId")

        RetrofitClient.put_instance.putUser(userId).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                _isLoading.value = false
                if (response.isSuccessful) { // 신규 사용자
                    Log.d("SigninViewModel", "PUT successful (New User). Code: ${response.code()}")
                    _userStatus.value = UserStatus.NEW_USER
                } else if (response.code() == 409) { // HTTP 409 Conflict - 기존 사용자
                    Log.d("SigninViewModel", "PUT failed (Existing User). Code: ${response.code()}")
                    _userStatus.value = UserStatus.EXISTING_USER
                } else { // 그 외 오류
                    Log.e("SigninViewModel", "PUT failed. Code: ${response.code()}, Message: ${response.message()}")
                    _userStatus.value = UserStatus.ERROR
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                _isLoading.value = false
                Log.e("SigninViewModel", "PUT error: ${t.message}", t)
                _userStatus.value = UserStatus.ERROR
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
    }
}