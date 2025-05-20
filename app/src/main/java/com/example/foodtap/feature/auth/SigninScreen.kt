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
                delay(1000)
                navController.navigate("init") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.EXISTING_USER -> {
                // UserData가 ViewModel에 로드된 후 이 상태가 됨
                Log.d("SigninScreen", "Navigating to my screen for existing user. UserData should be available.")
                delay(1000)
                navController.navigate("my") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            UserStatus.ERROR -> {
                Log.e("SigninScreen", "Error occurred during sign-in process. Navigating to initScreen as fallback.")
                delay(1000)
                navController.navigate("init") { // 오류 시 기본 화면으로 이동
                    popUpTo("signin") { inclusive = true }
                }
            }
            else -> {
                // UserStatus.LOADING_USER_DATA, UserStatus.UNKNOWN, 또는 isLoadingInitialCheck가 true인 경우는 위에서 처리됨
                Log.d("SigninScreen", "User status is $userStatus. Waiting or already handled.")
            }
        }
    }

    // UI 표시
    Column(
        modifier = Modifier.fillMaxSize(),
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
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (userStatus) {
                UserStatus.NEW_USER -> Text("환영합니다! 초기 설정 화면으로 이동합니다.", fontSize = 18.sp)
                UserStatus.EXISTING_USER -> Text("다시 찾아주셔서 감사합니다! 내 정보 화면으로 이동합니다.", fontSize = 18.sp)
                UserStatus.ERROR -> Text("오류가 발생했습니다. 잠시 후 다시 시도해주세요.", fontSize = 18.sp, color = Color.Red)
                else -> Text("잠시만 기다려주세요...", fontSize = 18.sp)
            }
        }
    }
}