package com.example.foodtap.feature.camera

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.feature.ocr.ClovaOcrAnalyzer
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.ui.theme.Safe
import com.example.foodtap.ui.theme.Show
import com.example.foodtap.ui.theme.Unsafe
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel = viewModel()) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() } // 이미지 분석을 위한 백그라운드 Executor
    val apiCallExecutor = remember { Executors.newSingleThreadExecutor() } // API 호출을 위한 별도 Executor (필요시, Retrofit은 자체 스레드 풀 사용)

    val ocrTextList = remember { mutableStateListOf<String>() }

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val nutritionText by viewModel.nutritionText.collectAsStateWithLifecycle()
    val expiryText by viewModel.expiryText.collectAsStateWithLifecycle()
    val identifiedAllergy by viewModel.identifiedAllergy.collectAsStateWithLifecycle()
    val identifiedDesc by viewModel.identifiedDesc.collectAsStateWithLifecycle()
    val identifiedExpiration by viewModel.identifiedExpiration.collectAsStateWithLifecycle()
    val dDay by viewModel.dDayExp.collectAsStateWithLifecycle()

    // Clova OCR API 호출을 담당하는 Analyzer 인스턴스 생성
    // ClovaOcrAnalyzer 인스턴스 생성 및 콜백 람다 정의
    val clovaOcrAnalyzer = remember {
        ClovaOcrAnalyzer(executor = executor) { inferTextLines ->
            // 이 람다 함수는 ClovaOcrAnalyzer에서 API 응답을 받은 후 메인 스레드에서 호출됨
            // inferTextLines는 Clova API에서 인식된 텍스트 라인들의 String임
            Log.d("CameraScreen", "Received OCR lines from Analyzer: $inferTextLines")

            viewModel.clovaAnalyze(inferTextLines)
        }
    }

    // ImageAnalysis 유스케이스 설정 및 분석기 연결
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            // .setTargetResolution(Size(640, 480)) // 원하는 해상도 설정 (API 요구사항 고려)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 가장 최신 프레임만 분석
            .build()
            .also {
                // Analyzer와 실행할 Executor 연결
                it.setAnalyzer(executor, clovaOcrAnalyzer)
            }
    }

    LaunchedEffect(Unit) {
        delay(500)
        viewModel.speak("식품 정보를 촬영하세요.")
    }

    val previewView = remember {
        PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // 카메라 미리보기 및 이미지 분석 유스케이스 바인딩
    LaunchedEffect(previewView, imageAnalysis) { // imageAnalysis가 변경될 때 재실행
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            // Preview 유스케이스와 ImageAnalysis 유스케이스를 함께 바인딩
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Button(
            onClick = {
                viewModel.stopSpeaking()
                navController.navigate("my") {
                    //
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Show),
            modifier = Modifier
                .padding(bottom = 80.dp)
                .size(width = 330.dp, height = 100.dp)
                .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "마이 페이지",
                color = Color.Black,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "마이 페이지로 이동" }
            )
        }
    }

    if (showDialog) {
        val isSafe = viewModel.expFiltering() && viewModel.allergyFiltering()

        LaunchedEffect(showDialog) {
            val vibrator = ctx.getSystemService(Vibrator::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = if (isSafe) {
                    VibrationEffect.createOneShot(150, 200)
                } else {
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 50, 200), intArrayOf(0, 255, 0, 255), -1)
                }
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isSafe) 150 else 200)
            }
        }

        AlertDialog(
            containerColor = if (isSafe) Safe else Unsafe,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "식품 정보",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            text = {
                Box(modifier = Modifier.height(330.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "소비 기한",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (identifiedDesc.isBlank() || dDay == null) "없음" else "$identifiedExpiration (D-$dDay)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "알레르기 성분",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (identifiedAllergy.isEmpty()) "없음" else identifiedAllergy.joinToString(", "),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.Black,
                            lineHeight = 30.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "식품 상세 정보",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .height(100.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = identifiedDesc.ifBlank { "없음" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Black,
                                lineHeight = 25.sp,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopSpeaking()
                        viewModel.resetScan()
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
            },
            dismissButton = {
                Button(
                    onClick = {
                        val listen = buildString {
                            append("소비기한은 ")
                            append(if (identifiedExpiration.isNotBlank()) "$identifiedExpiration 일 까지 입니다." else "인식되지 않았습니다.")
                            append("알레르기 성분은 ")
                            append(if (identifiedAllergy.isNotEmpty()) "$identifiedAllergy 입니다." else "인식되지 않았습니다.")
                        }
                        viewModel.speak(listen)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors =  ButtonDefaults.buttonColors(containerColor = Main),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 360.dp, height = 80.dp)
                ) {
                    Text(
                        text = "음성으로 듣기",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            },
            onDismissRequest = {}
        )
    }
}