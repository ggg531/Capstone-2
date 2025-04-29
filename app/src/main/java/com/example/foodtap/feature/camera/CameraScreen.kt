package com.example.foodtap.feature.camera

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.foodtap.feature.ocr.OcrAnalyzer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.regex.Pattern

@Composable
fun CameraScreen(navController: NavController) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }
    val ocrTextList = remember { mutableStateListOf<String>() }

    var nutritionText by remember { mutableStateOf("") }
    var expiryText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }

    val analyzer = remember {
        OcrAnalyzer(recognizer, executor) { result ->
            if (result.isNotBlank() && isScanning) {
                val lines = result.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                ocrTextList.addAll(lines)
            }
        }.create()
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

    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
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
            delay(3000L)

            val distinctText = ocrTextList.distinct()
            val nutrition = distinctText.joinToString("\n")
            val expiry = distinctText.filter { containsDate(it) }.joinToString("\n")

            if (nutrition.isNotBlank() || expiry.isNotBlank()) {
                nutritionText = nutrition
                expiryText = expiry

                //

                showDialog = true
                isScanning = false
            }
            ocrTextList.clear()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                isScanning = true
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        //
                    }) {
                        Text("다시 듣기")
                    }
                    TextButton(onClick = {
                        showDialog = false
                        isScanning = true
                    }) {
                        Text("확인")
                    }
                }
            },
            title = { Text("식품 정보") },
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
            }
        )
    }
}

fun containsDate(text: String): Boolean {
    val datePatterns = listOf(
        "\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}",
        "\\d{2}[./-]\\d{1,2}[./-]\\d{1,2}"
    )
    return datePatterns.any { pattern ->
        Pattern.compile(pattern).matcher(text).find()
    }
}