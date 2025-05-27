package com.example.foodtap

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodtap.api.ConfirmData
import com.example.foodtap.api.ExpiData
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.SttRequest
import com.example.foodtap.api.UserData
import com.example.foodtap.feature.auth.UserStatus
import com.example.foodtap.ui.theme.FoodTapTheme
import com.example.foodtap.util.FileManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = "58442700"
        val expiData = ExpiData("7")

        RetrofitClient.put_expi.putExpirDate(userId, expiData) //
            .enqueue(object : Callback<String> { //
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        val message = response.body()
                        Log.d("MA_TEXT", "Expiration date updated successfully: $message")
                    } else {
                        Log.e("MA_TEXT", "Failed to update expiration date: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("MA_TEXT", "API call failed: ${t.message}")
                }
            })

        val sttRequest = SttRequest("그래 알았어") //
        RetrofitClient.get_confirm.getConfirm(sttRequest) //
            .enqueue(object : Callback<ConfirmData> { //
                override fun onResponse(call: Call<ConfirmData>, response: Response<ConfirmData>) {
                    if (response.isSuccessful) {
                        val confirmData = response.body() //
                        confirmData?.let {
                            Log.d("MA_TEXT", "Confirmation status: ${it.confirm}") //
                        }
                    } else {
                        Log.e("MA_TEXT", "Failed to get confirmation: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ConfirmData>, t: Throwable) {
                    Log.e("MA_TEXT", "API call failed: ${t.message}")
                }
            })

        val userSttRequest = SttRequest("7일로 바꿔줘") //
        RetrofitClient.stt2ExpiDate.stt2ExpiDate(userSttRequest) //
            .enqueue(object : Callback<ExpiData> { //
                override fun onResponse(call: Call<ExpiData>, response: Response<ExpiData>) {
                    if (response.isSuccessful) {
                        val newExpiData = response.body() //
                        newExpiData?.let {
                            Log.d("MA_TEXT", "Extracted expiration date: ${it.expi}") //
                        }
                    } else {
                        Log.e("MA_TEXT", "Failed to get expiration date from STT: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ExpiData>, t: Throwable) {
                    Log.e("MA_TEXT", "API call failed: ${t.message}")
                }
            })

        enableEdgeToEdge()
        setContent {
            FoodTapTheme {
                MainApp()
            }
        }
    }
}