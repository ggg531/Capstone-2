package com.example.foodtap.feature.init

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.collectLatest

@Composable
fun InitScreen(navController: NavController, viewModel: InitViewModel = viewModel()) {
    val context = LocalContext.current

    val isListening by viewModel.isListening.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val displayAllergyText by viewModel.displayAllergyText.collectAsState()
    val apiCallStatus by viewModel.apiCallStatus.collectAsState()
    val saveAllergyStatus by viewModel.saveAllergyStatus.collectAsState() // 알레르기 저장 API 상태

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = !isListening && apiCallStatus != ApiStatus.LOADING) { // 로딩 중 아닐 때만 클릭 가능
                if (!isListening) {
                    viewModel.startListening()
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if ((isListening || apiCallStatus == ApiStatus.LOADING) && !showDialog) {
            MicAnimation()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "음성 인식 중입니다.",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
        } else if (!showDialog) {
            Text(
                text = "화면을 탭하여 알레르기 성분을 등록하세요.",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    // 알레르기 저장 API 성공 시 "my" 화면으로 이동
    LaunchedEffect(saveAllergyStatus) {
        // collectLatest를 사용하여 최신 상태만 처리하고, 이전 네비게이션 시도를 취소 (중복 방지)
        viewModel.saveAllergyStatus.collectLatest { status ->
            if (status == ApiStatus.SUCCESS) {
                navController.navigate("my") {
                    popUpTo("init") { inclusive = true } // InitScreen을 백스택에서 제거
                    launchSingleTop = true // "my" 화면이 이미 스택에 있다면 새로 만들지 않음
                }
                // 성공 후 상태를 IDLE로 되돌려 중복 네비게이션 방지
                // viewModel.resetSaveStatus() // ViewModel에 이런 함수를 만들어서 호출 가능
            }
            // 에러 처리는 하지 않도록 요청받았으므로, ApiStatus.ERROR에 대한 별도 UI 반응 없음
        }
    }

    if (showDialog) {
        val hasValidResult = apiCallStatus == ApiStatus.SUCCESS && displayAllergyText.isNotBlank() && displayAllergyText != "인식된 알레르기 성분이 없습니다."
        //val noResult = apiCallStatus == ApiStatus.SUCCESS && (displayAllergyText.isBlank() || displayAllergyText == "인식된 알레르기 성분이 없습니다.")
        val dialogTitle = when {
            apiCallStatus == ApiStatus.LOADING -> "처리 중" // 이 경우는 거의 없음 (showDialog는 보통 로딩 후에 true가 됨)
            hasValidResult -> "보유 알레르기 성분"
            //noResult -> "결과 없음"
            else -> "재등록 필요" // ApiStatus.ERROR 또는 STT 결과 없음 등
        }
        val dialogText = when {
            hasValidResult -> "$displayAllergyText 성분을 등록하시겠습니까?"
            //noResult -> "보유 알레르기 성분이 없습니까?"
            else -> "알레르기 성분을 다시 등록하세요."
        }

        LaunchedEffect(apiCallStatus, displayAllergyText) {

            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(150, 200)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }

            if (apiCallStatus != ApiStatus.LOADING) {
                //viewModel.speak(dialogText)
                val speechText = if (hasValidResult) {
                    "$displayAllergyText 성분을 등록하시겠습니까? 맞으면 중간에 위치한 파란색 버튼을, 아니라면 가장 아래에 위치한 버튼을 클릭하세요."
                } else {
                    "알레르기 성분을 다시 등록하세요."
                }
                viewModel.speak(speechText)
            }

        }

        AlertDialog(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = dialogTitle,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (apiCallStatus != ApiStatus.LOADING) {
                        Text(
                            text = dialogText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    else {
                        CircularProgressIndicator(color = Main)
                    }
                }
            },
            confirmButton = {
                if (apiCallStatus != ApiStatus.LOADING && hasValidResult) {
                    Button(
                        onClick = {
                            viewModel.confirmResult()
                            navController.navigate("my") {
                                popUpTo("init") { inclusive = true }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors =  ButtonDefaults.buttonColors(containerColor = Main),
                        modifier = Modifier.size(width = 360.dp, height = 80.dp)
                    ) {
                        Text(
                            text = "확인",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                if (apiCallStatus != ApiStatus.LOADING) {
                    Button(
                        onClick = { viewModel.resetResult() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(width = 360.dp, height = 80.dp)
                    ) {
                        Text(
                            text = "재등록",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            },
            onDismissRequest = {}
        )
    }
}

@Composable
fun MicAnimation() {
    val infiniteTransition = rememberInfiniteTransition()

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Main.copy(alpha = alpha),
                radius = 40.dp.toPx() * scale
            )
        }
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "음성 인식 중입니다.",
            tint = Color.White,
            modifier = Modifier
                .size(64.dp)
                .background(Main, CircleShape)
                .padding(16.dp)
        )
    }
}