package com.example.foodtap.feature.camera

import android.os.Build
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.foodtap.ui.theme.Show

@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val nutritionText by viewModel.nutritionText.collectAsStateWithLifecycle()
    val expiryText by viewModel.expiryText.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()

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
            .padding(top = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally

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
    }

    if (showDialog) {
        LaunchedEffect(showDialog) {
            val haptic = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                haptic?.vibrate(
                    VibrationEffect.createOneShot(150, 200)
                )
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
                        text = if (expiryText.isNotBlank()) expiryText else "없음",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "알레르기:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = if (nutritionText.isNotBlank()) nutritionText else "없음"
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
                            append(if (expiryText.isNotBlank()) expiryText else "인식되지 않았습니다.")
                            append("알레르기 성분은 ")
                            append(if (nutritionText.isNotBlank()) nutritionText else "인식되지 않았습니다.")
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