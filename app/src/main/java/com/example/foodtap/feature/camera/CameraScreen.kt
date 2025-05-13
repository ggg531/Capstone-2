package com.example.foodtap.feature.camera

import android.os.Build
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.feature.ocr.OcrAnalyzer
import com.example.foodtap.ui.theme.Main
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.foodtap.ui.theme.Show
import com.example.foodtap.ui.theme.Unsafe
import com.example.foodtap.ui.theme.Safe


// 외부 API로 OCR 결과 전송
//suspend fun sendOcrResultToApi(ocrText: String): Boolean {
//    return try {
//        val response = withContext(Dispatchers.IO) {
//            // Retrofit 또는 OkHttp를 통한 API 호출
//            // 예: apiService.sendOcrResult(RequestBody(ocrText))
//        }
//        response.approval  // Boolean 필드 반환
//    } catch (e: Exception) {
//        e.printStackTrace()
//        false
//    }
//}

@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    val identifiedAllergy by viewModel.identifiedAllergy.collectAsStateWithLifecycle()
    val identifiedDesc by viewModel.identifiedDesc.collectAsStateWithLifecycle()

    val analyzer = remember {
        OcrAnalyzer(recognizer, executor) { result ->
            if (result.isNotBlank() && isScanning) {
                viewModel.addOcrResult(result)
            }
        }.create()
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        viewModel.speak("식품 정보를 촬영하세요.")
    }

    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // OCR 결과 분석 및 UI 반영
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(3000)
            viewModel.processResults()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 30.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween

    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 330.dp, height = 72.dp)
                .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                .background(Show, RoundedCornerShape(16.dp))
        ) {
            Text(
                text = "식품 정보를 촬영하세요.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        Button(
            onClick = {
                viewModel.stopSpeaking()
                navController.navigate("my") {
                    //
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Main),
            modifier = Modifier
                .size(width = 330.dp, height = 100.dp)
                .border(3.dp, Color.White, RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "마이 페이지",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "마이 페이지로 이동" }
            )
        }
    }

    if (showDialog) {
        //val isSafe = viewModel.descFiltering() && viewModel.allergyFiltering()
        val isSafe = viewModel.allergyFiltering()

        LaunchedEffect(showDialog) {
            val vibrator = context.getSystemService(Vibrator::class.java)

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
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "식품 정보",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            text = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "소비기한:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = identifiedDesc.ifBlank { "없음" },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "알레르기:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = identifiedAllergy.toString().ifBlank { "없음" },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
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
                    modifier = Modifier.size(width = 360.dp, height = 72.dp)
                ) {
                    Text(
                        text = "확인",
                        fontSize = 28.sp,
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
                            append(if (identifiedDesc.isNotBlank()) "$identifiedDesc 입니다." else "인식되지 않았습니다.")
                            append("알레르기 성분은 ")
                            append(if (identifiedAllergy.isNotEmpty()) "$identifiedAllergy 입니다." else "인식되지 않았습니다.")
                        }
                        viewModel.speak(listen)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors =  ButtonDefaults.buttonColors(containerColor = Main),
                    modifier = Modifier.size(width = 360.dp, height = 72.dp)
                ) {
                    Text(
                        text = "음성으로 듣기",
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