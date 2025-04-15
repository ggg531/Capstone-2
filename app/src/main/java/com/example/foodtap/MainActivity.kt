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
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.api.UserData
import com.example.foodtap.ui.theme.FoodTapTheme
import com.example.foodtap.util.FileManager
import retrofit2.Call

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // ID 가져오기
//        val userId = FileManager.getOrCreateId(this)
//        val put_user_call = RetrofitClient.put_instance.putUser(userId)
//        val get_user_call = RetrofitClient.get_instance.getUser(userId)
//        var new_user = false
//
//        put_user_call.enqueue(object : retrofit2.Callback<String> {
//            override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
//                if (response.isSuccessful) {
//                    // 신규 사용자 (처음 어플리케이션 실행시)
//                    Log.d("PUT", "Success: ${response.body()}")
//                    new_user = true
//                } else if (response.code() == 409) {
//                    // 기존 사용자 (이미 어플리케이션 실행시)
//                    Log.d("PUT", "Already exists")
//                }
//                else{
//                    Log.e("PUT", "Failed with code: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<String>, t: Throwable) {
//                Log.e("PUT", "Error: ${t.message}")
//            }
//        })
//
//        if (new_user) {
//
//        }
//        else {
//            get_user_call.enqueue(object : retrofit2.Callback<UserData> {
//                override fun onResponse(call: Call<UserData>, response: retrofit2.Response<UserData>) {
//                    if (response.isSuccessful) {
//                        val userResponse = response.body()
//                        if (userResponse != null) {
//                            val allergy = userResponse.allergy
//                            val id = userResponse.id
//                            // allergy와 id를 사용하여 필요한 작업 수행
//                            println("allergy: $allergy, id: $id")
//                        }
//                        Log.d("GET", "Success: ${response.body()}")
//                    }
//                    else{
//                        Log.e("GET", "Failed with code: ${response.code()}")
//                    }
//                }
//
//                override fun onFailure(call: Call<UserData>, t: Throwable) {
//                    Log.e("GET", "Error: ${t.message}")
//                }
//            })
//        }


        enableEdgeToEdge()
        setContent {
            FoodTapTheme {
                MainApp()
            }
        }
    }
}