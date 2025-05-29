package com.example.foodtap.feature.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay

@Composable
fun SigninScreen(navController: NavController, viewModel: SigninViewModel = viewModel()) {
    val context = LocalContext.current

    val userStatus by viewModel.userStatus.observeAsState(UserStatus.UNKNOWN)
    val isLoadingInitialCheck by viewModel.isLoading.observeAsState(true) // 초기 ID 확인 (로딩 상태)

    // 사용자 ID 가져오기 및 생성
    LaunchedEffect(Unit) {
        viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")

        val userId: String
        try {
            userId = FileManager.getOrCreateId(context) // ID 가져오기 or 생성
            Log.d("SigninScreen", "Generated User ID: $userId")
        } catch (e: Exception) {
            Log.e("SigninScreen", "Failed to get or create user ID: ${e.message}", e)
            viewModel.checkUserStatus("fallbackUserId") // ID 생성 실패 시 대체 ID
            return@LaunchedEffect
        }

        viewModel.checkUserStatus(userId) // 사용자 상태 확인 요청
    }

    // 사용자 상태에 따른 네비게이션 처리
    LaunchedEffect(userStatus) {
        if (isLoadingInitialCheck || userStatus == UserStatus.LOADING_USER_DATA || userStatus == UserStatus.UNKNOWN) {
            return@LaunchedEffect // 로딩 중 or UNKNOWN 상태: 네이게이션 로직 실행 X
        }

        when (userStatus) {
            UserStatus.NEW_USER -> {
                Log.d("SigninScreen", "Navigating to initScreen for new user.")
                viewModel.speak("식품 톡톡에 오신 것을 환영합니다!")
                delay(3000)
                navController.navigate("init") {
                    popUpTo("signin") { inclusive = true }
                }
            }

            UserStatus.EXISTING_USER -> {
                Log.d("SigninScreen", "Navigating to my screen for existing user.")
                viewModel.speak("식품 톡톡을 다시 찾아주셔서 감사합니다!")
                delay(3000)
                navController.navigate("camera") {
                    popUpTo("signin") { inclusive = true }
                }
            }

            UserStatus.ERROR -> {
                Log.e("SigninScreen", "Error occurred. Navigating to initScreen as fallback.")
                viewModel.speak("오류가 발생했습니다.")
                delay(3000)
                navController.navigate("init") {
                    popUpTo("signin") { inclusive = true }
                }
            }

            else -> {
                Log.d("SigninScreen", "User status is $userStatus. Waiting or already handled.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Main),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .weight(0.4f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "식품 톡톡",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Main,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isLoadingInitialCheck || userStatus == UserStatus.LOADING_USER_DATA || userStatus == UserStatus.UNKNOWN -> {
                    Text(
                        text = "사용자 정보를 확인 중입니다.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = Color.White)
                }

                userStatus == UserStatus.NEW_USER -> Text(
                    text = "환영합니다!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                userStatus == UserStatus.EXISTING_USER -> { // UserStatus.LOADING_USER_DATA
                    Text(
                        text = "사용자 정보를\n불러오는 중입니다.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = Color.White)
                }

                userStatus == UserStatus.ERROR -> Text(
                    text = "오류가 발생했습니다.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center
                )

                else -> Text(
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