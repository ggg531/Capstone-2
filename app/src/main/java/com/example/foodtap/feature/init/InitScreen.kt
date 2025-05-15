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
import kotlinx.coroutines.delay

@Composable
fun InitScreen(navController: NavController, viewModel: InitViewModel = viewModel()) {
    val context = LocalContext.current

    val isListening by viewModel.isListening.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val displayAllergyText by viewModel.displayAllergyText.collectAsState()
    // val processedAllergyList by viewModel.processedAllergyList.collectAsState() // 직접 사용하지 않으면 주석 처리 가능
    val apiCallStatus by viewModel.apiCallStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = !isListening && apiCallStatus != ApiStatus.LOADING) { // 로딩 중 아닐 때만 클릭 가능
                if (!isListening) {
                    viewModel.startListening()
                }
                // STT 중지 로직은 startListening 내부 또는 onResults/onError에서 관리됨
                // else {
                // viewModel.stopListening() // 이 부분은 불필요할 수 있음
                // viewModel.tapShowDialog(true) // API 호출 결과에 따라 다이얼로그 표시됨
                // }
            }
            .padding(16.dp), // 전체적인 패딩
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isListening || apiCallStatus == ApiStatus.LOADING) {
            micAnimation()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "음성 인식 중입니다.",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                /*
                fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
                 */
            )
        } else {
            Text(
                text = "화면을 탭하여 알레르기 성분을\n음성으로 등록하세요.",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showDialog) {
        // API 성공적으로 호출되어 결과가 있거나 (빈 리스트 포함), STT/API 오류가 발생했을 때 다이얼로그 표시
        val hasValidResult = apiCallStatus == ApiStatus.SUCCESS && displayAllergyText.isNotBlank() && displayAllergyText != "인식된 알레르기 성분이 없습니다."
        val sttresult = if (hasValidResult) "$displayAllergyText 성분을 등록하시겠습니까?" else "알레르기 성분을 다시 등록하세요."
        val dialogTitle = when {
            apiCallStatus == ApiStatus.LOADING -> "처리 중..." // 이 경우는 거의 없음 (showDialog는 보통 로딩 후에 true가 됨)
            hasValidResult -> "보유 알레르기 성분"
            apiCallStatus == ApiStatus.SUCCESS && (displayAllergyText.isBlank() || displayAllergyText == "인식된 알레르기 성분이 없습니다.") -> "결과 없음"
            else -> "재등록 필요" // ApiStatus.ERROR 또는 STT 결과 없음 등
        }

        LaunchedEffect(showDialog) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(150, 200)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        }

        LaunchedEffect(sttresult) {
            delay(500)
            viewModel.speak(sttresult)
        }

        AlertDialog(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (hasValidResult) "보유 알레르기 성분" else "재등록 필요",
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
                    Text(
                        text = if (hasValidResult) "$displayAllergyText\n성분을 등록하시겠습니까?" else "알레르기 성분을\n다시 등록하세요.",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            confirmButton = {
                if (hasValidResult) {
                    Button(
                        onClick = {
                            viewModel.confirmResult()
                            navController.navigate("camera") {
                                popUpTo("init") { inclusive = true }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors =  ButtonDefaults.buttonColors(containerColor = Main),
                        modifier = Modifier.size(width = 360.dp, height = 72.dp)
                    ) {
                        Text(
                            text = "확인",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.resetResult()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Main),
                    modifier = Modifier.size(width = 360.dp, height = 72.dp)
                ) {
                    Text(
                        text = if (hasValidResult) "다시 듣기 ? 재등록" else "재등록",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            },
            onDismissRequest = {}
        )
    }
}

@Composable
fun micAnimation() {
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