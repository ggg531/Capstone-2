package com.example.foodtap.feature.camera

import android.util.Log
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
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse
import com.example.foodtap.api.RetrofitClient
import com.example.foodtap.feature.ocr.OcrAnalyzer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import java.util.concurrent.Executors
import java.util.regex.Pattern

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

    var identifiedAllergy by remember { mutableStateOf<List<String>>(emptyList()) }
    var identifiedDesc by remember { mutableStateOf("") }
    
    // OCR 결과 분석 및 UI 반영
    LaunchedEffect(isScanning) {
        while (isScanning) {
            delay(3000L)

            val distinctText = ocrTextList.distinct()
            val nutrition = distinctText.joinToString("\n")
            val expiry = distinctText.filter { containsDate(it) }.joinToString("\n")

            if (nutrition.isNotBlank() || expiry.isNotBlank()) {
                val fullText = "$nutrition\n$expiry"

                // 외부 API를 호출하여 OCR 결과를 전송하고 허가 여부 결정
                val request = OcrRequest(ocr = fullText)


                RetrofitClient.get_approval.getApproval(request).enqueue(object : Callback<OcrResponse> {
                    override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                        if (response.isSuccessful) {
                            val approval = response.body()?.approval == true
                            val allergy = response.body()?.allergy
                            val desc = response.body()?.desc

                            Log.d("API", "approval: $approval")
                            Log.d("API", "allergy: $allergy")
                            Log.d("API", "desc: $desc")

                            if (approval && (allergy != null && desc != null)) {
                                identifiedAllergy = allergy
                                identifiedDesc = desc
                                nutritionText = nutrition
                                expiryText = expiry
                                showDialog = true
                                isScanning = false
                            }

                        } else {
                            Log.e("API", "서버 응답 오류: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                        Log.e("API", "요청 실패: ${t.message}")
                    }
                })


            }

            ocrTextList.clear()
        }
    }

//    LaunchedEffect(isScanning) {
//        if (isScanning) {
//            delay(3000L)
//
//            val distinctText = ocrTextList.distinct()
//            val nutrition = distinctText.joinToString("\n")
//            val expiry = distinctText.filter { containsDate(it) }.joinToString("\n")
//
//            if (nutrition.isNotBlank() || expiry.isNotBlank()) {
//                nutritionText = nutrition
//                expiryText = expiry
//
//                //
//
//                showDialog = true
//                isScanning = false
//            }
//            ocrTextList.clear()
//        }
//    }

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
                        text = expiryText.ifBlank { "없음" },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "알레르기:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
//                    Text(
//                        text = nutritionText.ifBlank { "없음" }
//                    )
                    Text(
                        text = identifiedAllergy.toString().ifBlank { "없음" }
                    )
                    Text(
                        text = identifiedDesc.ifBlank { "없음" }
                    )
//                    Text(
//                        text = nutritionText.ifBlank { "없음" }
//                    )
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