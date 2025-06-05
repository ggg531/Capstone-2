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
import com.example.foodtap.api.ProductNameRequest
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.SttRequest
import com.example.foodtap.api.UserData
import com.example.foodtap.api.UserHist
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
        val productNameRequest = ProductNameRequest("참크래커")
        val userHist = UserHist(userId, "[조개류]", "참크래커") //
//        RetrofitClient.putHist.putHist(userHist) //
//            .enqueue(object : Callback<String> { //
//                override fun onResponse(call: Call<String>, response: Response<String>) {
//                    if (response.isSuccessful) {
//                        val message = response.body() //
//                        Log.d("MA_TEXT", "Confirmation status: $message") //
//                    } else {
//                        Log.e("MA_TEXT", "Failed to get confirmation: ${response.errorBody()?.string()}")
//                    }
//                }
//
//                override fun onFailure(call: Call<String>, t: Throwable) {
//                    Log.e("MA_TEXT", "API call failed: ${t.message}")
//                }
//            })

        RetrofitClient.getHist.getHist(userId, productNameRequest) //
            .enqueue(object : Callback<List<UserHist>> { //
                override fun onResponse(call: Call<List<UserHist>>, response: Response<List<UserHist>>) {
                    if (response.isSuccessful) {
                        val userHistList = response.body()
                        Log.d("MA_TEXT", "userHistList successfully: $userHistList")
                    } else {
                        Log.e("MA_TEXT", "Failed userHistList: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<List<UserHist>>, t: Throwable) {
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