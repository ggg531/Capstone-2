package com.example.foodtap.feature.init

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

//@Composable
//fun InitScreen(navController: NavController, viewModel: InitViewModel = viewModel()) {
//    val context = LocalContext.current
//
//    val isListening by viewModel.isListening.collectAsState()
//    val showDialog by viewModel.showDialog.collectAsState()
//    val allergySttText by viewModel.allergySttText.collectAsState()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .clickable {
//                if (!isListening) {
//                    viewModel.startListening()
//                } else {
//                    viewModel.stopListening()
//                    viewModel.tapShowDialog(true)
//                }
//            },
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        if (isListening) {
//            Icon(
//                imageVector = Icons.Default.Mic,
//                contentDescription = "음성 인식 중",
//                modifier = Modifier.size(64.dp)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            CircularProgressIndicator()
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "음성 인식 중입니다.",
//                style = MaterialTheme.typography.titleLarge,
//                textAlign = TextAlign.Center
//            )
//        } else {
//            Text(
//                text = "화면을 탭하여 보유 알레르기 성분을\n음성으로 등록하세요",
//                style = MaterialTheme.typography.titleLarge,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//
//    if (showDialog) {
//        val hasResult = allergySttText.isNotBlank()
//
//        LaunchedEffect(showDialog) {
//            val haptic = context.getSystemService(Vibrator::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                haptic?.vibrate(
//                    VibrationEffect.createOneShot(150, 200)
//                )
//            } else {
//                @Suppress("DEPRECATION")
//                haptic?.vibrate(150)
//            }
//        }
//
//        AlertDialog(
//            title = {
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Spacer(modifier = Modifier.height(24.dp))
//                    Text(
//                        text = if (hasResult) "보유 알레르기 성분" else "재등록 필요",
//                        fontSize = 32.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black
//                    )
//                }
//            },
//            text = {
//                Column(
//                    modifier = Modifier
//                        .width(320.dp)
//                        .height(200.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text(
//                        text = if (hasResult) "$allergySttText\n성분을 등록하시겠습니까?" else "알레르기 보유 성분을\n다시 등록하세요.",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        lineHeight = 40.sp,
//                        textAlign = TextAlign.Center,
//                    )
//                }
//            },
//            confirmButton = {
//                if (hasResult) {
//                    Button(
//                        onClick = {
//                            viewModel.confirmResult()
//                            navController.navigate("camera")
//                        },
//                        shape = RoundedCornerShape(16.dp),
//                        colors =  ButtonDefaults.buttonColors(containerColor = Main),
//                        modifier = Modifier.size(width = 360.dp, height = 72.dp)
//                    ) {
//                        Text(
//                            text = "확인",
//                            fontSize = 28.sp,
//                            fontWeight = FontWeight.Medium,
//                            color = Color.White
//                        )
//                    }
//                }
//            },
//            dismissButton = {
//                Button(
//                    onClick = {
//                        viewModel.resetResult()
//                    },
//                    shape = RoundedCornerShape(16.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = Main),
//                    modifier = Modifier.size(width = 360.dp, height = 72.dp)
//                ) {
//                    Text(
//                        text = if (hasResult) "다시 듣기 ? 재등록" else "재등록",
//                        fontSize = 28.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.White
//                    )
//                }
//            },
//            onDismissRequest = {}
//        )
//    }
//}


import androidx.compose.foundation.layout.Box // 로딩 인디케이터 중앙 정렬용

import androidx.compose.foundation.layout.padding // 패딩 추가


@Composable
fun InitScreen(navController: NavController, viewModel: InitViewModel = viewModel()) {
    val context = LocalContext.current

    val isListening by viewModel.isListening.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val displayAllergyText by viewModel.displayAllergyText.collectAsState()
    // val processedAllergyList by viewModel.processedAllergyList.collectAsState() // 직접 사용하지 않으면 주석 처리 가능
    val apiCallStatus by viewModel.apiCallStatus.collectAsState()

    val haptic = context.getSystemService(Vibrator::class.java) // 진동기 인스턴스화

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
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "음성 인식 중" else "알레르기 정보 처리 중",
                modifier = Modifier.size(64.dp),
                tint = if (isListening) Main else Color.Gray // 상태에 따른 아이콘 색상 변경
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isListening) "음성 인식 중입니다..." else "알레르기 정보 처리 중입니다...",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "화면을 탭하여 보유 알레르기 성분을\n음성으로 등록하세요",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), // 폰트 크기 조정
                textAlign = TextAlign.Center
            )
        }
    }

    if (showDialog) {
        // API 성공적으로 호출되어 결과가 있거나 (빈 리스트 포함), STT/API 오류가 발생했을 때 다이얼로그 표시
        val hasValidResult = apiCallStatus == ApiStatus.SUCCESS && displayAllergyText.isNotBlank() && displayAllergyText != "인식된 알레르기 성분이 없습니다."
        val dialogTitle = when {
            apiCallStatus == ApiStatus.LOADING -> "처리 중..." // 이 경우는 거의 없음 (showDialog는 보통 로딩 후에 true가 됨)
            hasValidResult -> "보유 알레르기 성분"
            apiCallStatus == ApiStatus.SUCCESS && (displayAllergyText.isBlank() || displayAllergyText == "인식된 알레르기 성분이 없습니다.") -> "결과 없음"
            else -> "재등록 필요" // ApiStatus.ERROR 또는 STT 결과 없음 등
        }

        LaunchedEffect(showDialog) { // 진동 효과는 그대로 유지
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                haptic?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                haptic?.vibrate(150)
            }
        }

        AlertDialog(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = dialogTitle,
                        fontSize = 30.sp, // 폰트 크기 조정
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            text = {
                Box( // 텍스트 중앙 정렬 및 최소 높이 확보
                    modifier = Modifier
                        .width(320.dp)
                        // .heightIn(min = 150.dp), // 최소 높이 지정
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            hasValidResult -> "$displayAllergyText\n\n성분을 등록하시겠습니까?"
                            apiCallStatus == ApiStatus.SUCCESS && (displayAllergyText.isBlank() || displayAllergyText == "인식된 알레르기 성분이 없습니다.") -> "인식된 알레르기 성분이 없습니다.\n다시 등록하시겠습니까?"
                            else -> displayAllergyText.ifBlank { "알레르기 보유 성분을\n다시 등록하세요." } // 오류 메시지 또는 기본 재등록 메시지
                        },
                        fontSize = 22.sp, // 폰트 크기 조정
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        lineHeight = 36.sp, // 줄 간격 조정
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                if (hasValidResult) { // 유효한 결과가 있을 때만 "확인" 버튼 표시
                    Button(
                        onClick = {
                            viewModel.confirmResult()
                            navController.navigate("camera") // 다음 화면으로 이동
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Main),
                        modifier = Modifier
                            .fillMaxWidth() // 너비 채우기
                            .height(60.dp) // 높이 조정
                            .padding(horizontal = 16.dp) // 좌우 패딩
                    ) {
                        Text(
                            text = "확인",
                            fontSize = 24.sp, // 폰트 크기 조정
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = { // "재등록" 또는 "다시 듣기" 버튼
                Button(
                    onClick = {
                        viewModel.resetResult() // ViewModel의 리셋 함수 호출
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasValidResult) Color.Gray else Main), // 조건부 색상
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp)
                        .padding(top = if (hasValidResult) 0.dp else 8.dp, bottom = 8.dp) // 상단 패딩 추가 (확인 버튼 없을 때)
                ) {
                    Text(
                        // 문맥에 따라 버튼 텍스트 변경
                        text = if (hasValidResult) "다시 등록" else "재등록",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            },
            onDismissRequest = {
                // 다이얼로그 바깥을 클릭해도 닫히지 않도록 하려면 비워둠
                // 또는 viewModel.resetResult() 또는 특정 작업 수행
            }
        )
    }
}