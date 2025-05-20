package com.example.foodtap.feature.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay

@Composable
fun SigninScreen(navController: NavController, viewModel: SigninViewModel = viewModel()) {
    val userStatus by viewModel.userStatus.observeAsState(UserStatus.UNKNOWN)
    val isLoadingInitialCheck by viewModel.isLoading.observeAsState(true) // 초기 ID 확인 중 로딩 상태
    val context = LocalContext.current

    // 1. 사용자 ID 가져오기 및 사용자 상태 확인 요청
    LaunchedEffect(Unit) {
        viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")

        val userId: String
        try {
            userId = FileManager.getOrCreateId(context)
            Log.d("SigninScreen", "Generated User ID: $userId")
        } catch (e: Exception) {
            Log.e("SigninScreen", "Failed to get or create user ID: ${e.message}", e)
            // ID 생성 실패 시 에러 처리 또는 기본값 사용
            viewModel.checkUserStatus("fallbackUserId") // ID 생성 실패 시 대체 ID
            return@LaunchedEffect
        }

        viewModel.checkUserStatus(userId)
    }

    // 2. 사용자 상태에 따른 네비게이션 처리
    LaunchedEffect(userStatus) {
        // 로딩 중이거나 (PUT API 또는 GET API) 아직 상태 모를 때는 네비게이션 로직 실행하지 않음
        if (userStatus == UserStatus.LOADING_USER_DATA || userStatus == UserStatus.UNKNOWN || isLoadingInitialCheck) {
            return@LaunchedEffect
        }

        when (userStatus) {
            UserStatus.NEW_USER -> {
                Log.d("SigninScreen", "Navigating to initScreen for new user.")
                // 신규 사용자의 경우, TTS 안내 후 잠시 대기하고 이동하거나 바로 이동
                viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")
                delay(3000)
                navController.navigate("init") { // 사용자가 initScreen으로 명시
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.EXISTING_USER -> {
                Log.d("SigninScreen", "Navigating to my screen for existing user.")
                // 기존 사용자의 경우, TTS 안내 후 잠시 대기하고 이동하거나 바로 이동
                viewModel.speak("식품 톡톡을 다시 찾아주셔서 감사합니다!")
                delay(3000)
                navController.navigate("camera") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.ERROR -> {
                Log.e("SigninScreen", "Error occurred. Navigating to initScreen as fallback.")
                // 오류 발생 시 기본 화면으로 이동 (예: initScreen)
                viewModel.speak("오류가 발생했습니다.")
                delay(3000)
                navController.navigate("init") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            else -> {
                // UserStatus.LOADING_USER_DATA, UserStatus.UNKNOWN, 또는 isLoadingInitialCheck가 true인 경우는 위에서 처리됨
                Log.d("SigninScreen", "User status is $userStatus. Waiting or already handled.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Main)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoadingInitialCheck || userStatus == UserStatus.UNKNOWN || userStatus == UserStatus.LOADING_USER_DATA) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                when (userStatus) {
                    UserStatus.LOADING_USER_DATA -> "사용자 정보를 불러오는 중입니다..."
                    else -> "사용자 정보를 확인 중입니다..."
                },
                fontSize = 18.sp
            )
        } else {
            Text(
                text = "식품 톡톡",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (userStatus) {
                UserStatus.NEW_USER -> Text(
                    text = "환영합니다!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                UserStatus.EXISTING_USER -> Text(
                    text = "다시 찾아주셔서 감사합니다!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                UserStatus.ERROR -> Text(
                    text = "오류가 발생했습니다.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center
                )
                else -> Text( // UNKNOWN이지만 isLoading이 false인 경우
                    text = "잠시만 기다려주세요.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}