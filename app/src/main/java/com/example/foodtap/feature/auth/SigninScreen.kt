package com.example.foodtap.feature.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay

// FileManager가 프로젝트 내에 정의되어 있다고 가정하고 import 합니다.
// 예: import com.example.foodtap.util.FileManager

@Composable
fun SigninScreen(navController: NavController, viewModel: SigninViewModel = viewModel()) {
    val userStatus by viewModel.userStatus.observeAsState(UserStatus.UNKNOWN)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val context = LocalContext.current

    // 1. 사용자 ID 가져오기 및 사용자 상태 확인 요청
    LaunchedEffect(Unit) {
        viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")

        // 실제 ID 생성 로직으로 대체해야 합니다.
        var userId: String? = null
        try {
            userId = FileManager.getOrCreateId(context) // 실제 ID 생성/가져오기 로직
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
        if (isLoading) return@LaunchedEffect // 로딩 중에는 네비게이션 로직을 실행하지 않음

        when (userStatus) {
            UserStatus.NEW_USER -> {
                Log.d("SigninScreen", "Navigating to initScreen for new user.")
                // 신규 사용자의 경우, TTS 안내 후 잠시 대기하고 이동하거나 바로 이동
                delay(1000) // TTS 메시지 등을 위한 약간의 딜레이 (선택 사항)
                navController.navigate("init") { // 사용자가 initScreen으로 명시
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.EXISTING_USER -> {
                Log.d("SigninScreen", "Navigating to my screen for existing user.")
                // 기존 사용자의 경우, TTS 안내 후 잠시 대기하고 이동하거나 바로 이동
                delay(1000) // TTS 메시지 등을 위한 약간의 딜레이 (선택 사항)
                navController.navigate("my") { // 사용자가 my 스크린으로 명시
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.ERROR -> {
                Log.e("SigninScreen", "Error occurred. Navigating to initScreen as fallback.")
                // 오류 발생 시 기본 화면으로 이동 (예: initScreen)
                delay(1000)
                navController.navigate("init") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.UNKNOWN -> {
                // 초기 상태이거나 아직 API 응답이 오지 않은 경우 대기
                Log.d("SigninScreen", "User status is UNKNOWN. Waiting for API response.")
            }
        }
    }

    // UI 표시
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading || userStatus == UserStatus.UNKNOWN) { // 로딩 중이거나 아직 상태 모를 때
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("사용자 정보를 확인 중입니다...", fontSize = 18.sp)
        } else {
            Text(
                text = "식품 톡톡", // 문구는 자유롭게 수정
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (userStatus) {
                UserStatus.NEW_USER -> Text("환영합니다! 초기 설정 화면으로 이동합니다.", fontSize = 18.sp)
                UserStatus.EXISTING_USER -> Text("다시 찾아주셔서 감사합니다! 내 정보 화면으로 이동합니다.", fontSize = 18.sp)
                UserStatus.ERROR -> Text("오류가 발생했습니다. 잠시 후 다시 시도해주세요.", fontSize = 18.sp, color = Color.Red)
                else -> Text("잠시만 기다려주세요...", fontSize = 18.sp) // UNKNOWN이지만 isLoading이 false인 경우
            }
        }
    }
}