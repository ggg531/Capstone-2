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
import com.example.foodtap.feature.ocr.OcrAnalyzer // OcrAnalyzer import
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.ui.theme.Safe
import com.example.foodtap.ui.theme.Show
import com.example.foodtap.ui.theme.Unsafe
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() } // 이미지 분석을 위한 백그라운드 Executor

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val identifiedAllergy by viewModel.identifiedAllergy.collectAsStateWithLifecycle()
    val identifiedDesc by viewModel.identifiedDesc.collectAsStateWithLifecycle()
    val identifiedExpiration by viewModel.identifiedExpiration.collectAsStateWithLifecycle()
    val dDay by viewModel.dDayExp.collectAsStateWithLifecycle()

    val ocrAnalyzer = remember {
        OcrAnalyzer(executor = executor) { ocrResponse ->
            Log.d("CameraScreen", "Received processed OCR response from Analyzer: $ocrResponse")
            viewModel.handleProcessedOcrResponse(ocrResponse) // OCR 결과를 ViewModel로 전달하여 처리
        }
    }
    val imageAnalysis = remember {
        ocrAnalyzer.create() // ocrAnalyzer 연결
    }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startScanTimer() // 타이머 시작 (스캔 시작)
        delay(500) //
        viewModel.speak("식품 정보를 촬영하세요.")
    }

    // CameraScreen이 컴포지션에서 사라질 때
    DisposableEffect(lifecycleOwner) {
        onDispose {
            viewModel.stopScanTimer() // 타이머 중지
            viewModel.stopSpeaking() // tts 중지
        }
    }

    // 카메라 미리보기 및 이미지 분석 유스케이스 바인딩
    LaunchedEffect(previewView, imageAnalysis, isScanning) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll() // 이전에 바인딩된 모든 유스케이스 해제 (초기화)
            if (isScanning) { // 바인딩
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            }
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
                viewModel.setScanningState(false) //
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
        val isSafeForVibrationAndColor = viewModel.expFiltering(context) && viewModel.allergyFiltering(context)

        LaunchedEffect(showDialog) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = if (isSafeForVibrationAndColor) { // 안전한 경우: 150ms, 세기 200
                    VibrationEffect.createOneShot(150, 200)
                } else { // 위험한 경우: 250ms, 세기 255 (햅틱 2회)
                    VibrationEffect.createWaveform(longArrayOf(0, 250, 50, 250), intArrayOf(0, 255, 0, 255), -1)
                }
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isSafeForVibrationAndColor) 150 else 200)
            }

            if (isSafeForVibrationAndColor) {
                viewModel.speak("안전한 식품입니다. 상세 정보를 들으시려면 가장 아래에 위치한 버튼을 클릭하세요.")
            } else {
                viewModel.speak("구매에 적합하지 않은 식품입니다. 상세 정보를 들으시려면 가장 아래에 위치한 버튼을 클릭하세요.")
            }
        }

        AlertDialog(
            containerColor = if (isSafeForVibrationAndColor) Safe else Unsafe,
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
                Box(modifier = Modifier.height(200.dp)) {
                    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)) {
                        Text(
                            text = "소비 기한",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (identifiedDesc.isBlank() || dDay == null) {
                                "없음"
                            } else {
                                val dDayStr = if (dDay!! >= 0) "D-$dDay" else "D+${-dDay!!}"
                                "$identifiedExpiration ($dDayStr)"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(bottom = 20.dp)
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
                            modifier = Modifier
                                .height(80.dp)
                                .verticalScroll(scrollState),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopSpeaking()
                        viewModel.speak("식품 정보를 촬영하세요.")
                        viewModel.resetScan() // ViewModel에서 스캔 재개 및 타이머 초기화
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
                            append(if (identifiedExpiration.isNotBlank()) "$identifiedExpiration 까지로, ${dDay}일 남았습니다." else "인식되지 않았습니다.")
                            append("알레르기 성분은 ")
                            append(if (identifiedAllergy.isNotEmpty()) "$identifiedAllergy 입니다." else "인식되지 않았습니다.")
                            append (if (identifiedDesc.isNotBlank()) "\t$identifiedDesc" else "")
                        }
                        viewModel.speak(listen)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors =  ButtonDefaults.buttonColors(containerColor = Main),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 360.dp, height = 80.dp)
                ) {
                    Text(
                        text = "상세 정보 듣기",
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