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
        enableEdgeToEdge()
        setContent {
            FoodTapTheme {
                MainApp()
            }
        }
    }
}