// TODO: 유통기한도 GPT로 처리하기


package com.example.foodtap.feature.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import com.example.foodtap.feature.ocr.ClovaOcrAnalyzer
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import java.util.concurrent.Executors
import java.util.regex.Pattern

@Composable
fun CameraScreen(navController: NavController) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() } // 이미지 분석을 위한 백그라운드 Executor
    val apiCallExecutor = remember { Executors.newSingleThreadExecutor() } // API 호출을 위한 별도 Executor (필요시, Retrofit은 자체 스레드 풀 사용)

    val ocrTextList = remember { mutableStateListOf<String>() }

    var nutritionText by remember { mutableStateOf("") }
    var expiryText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) } // 스캔 중인지 여부 제어

    var identifiedAllergy by remember { mutableStateOf<List<String>>(emptyList()) }
    var identifiedDesc by remember { mutableStateOf("") }

    // Clova OCR API 호출을 담당하는 Analyzer 인스턴스 생성
    // ClovaOcrAnalyzer 인스턴스 생성 및 콜백 람다 정의
    val clovaOcrAnalyzer = remember {
        ClovaOcrAnalyzer(executor = executor) { inferTextLines ->
            // 이 람다 함수는 ClovaOcrAnalyzer에서 API 응답을 받은 후 메인 스레드에서 호출됨
            // inferTextLines는 Clova API에서 인식된 텍스트 라인들의 List<String>임
            Log.d("CameraScreen", "Received OCR lines from Analyzer: $inferTextLines")

            if (isScanning) { // 스캔 중일 때만 처리
                // 받은 텍스트 라인들을 CameraScreen의 상태에 반영
                ocrTextList.clear()
                ocrTextList.addAll(listOf(inferTextLines))

                // OCR 결과를 바탕으로 필요한 정보를 추출 (containsDate 함수 필요)
                val distinctText = ocrTextList.distinct() // 필요한 경우 중복 제거
                val nutrition = distinctText.joinToString("\n") // 예시: 모든 라인을 합침
                val expiry = distinctText.filter { containsDate(it) }.joinToString("\n") // 예시: containsDate 함수 필요

                if (nutrition.isNotBlank() || expiry.isNotBlank()) {
                    val fullText = "$nutrition\n$expiry"


                    Log.d("CameraScreen", "Triggering external approval API call with: $fullText")

                    // 외부 API 호출 및 결과 처리
                    val request = OcrRequest(ocr = fullText) // OcrRequest 데이터 클래스 필요
                    RetrofitClient.get_approval.getApproval(request).enqueue(object : Callback<OcrResponse> {
                        override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                            if (response.isSuccessful) {
                                val approval = response.body()?.approval == true
                                val allergy = response.body()?.allergy
                                val desc = response.body()?.desc

                                Log.d("API", "approval: $approval, allergy: $allergy, desc: $desc")

                                // 외부 API 응답 결과에 따라 UI 상태 업데이트
                                if (approval && (allergy != null && desc != null)) {
                                    identifiedAllergy = allergy
                                    identifiedDesc = desc
                                    // nutritionText와 expiryText는 OCR 결과에서 이미 추출했으므로 여기서 설정
                                    nutritionText = nutrition
                                    expiryText = expiry
                                    showDialog = true // 다이얼로그 표시
                                    isScanning = false // 스캔 중지
                                } else {
                                    // 승인되지 않거나 결과가 불완전한 경우 처리
                                    Log.d("API", "Approval denied or incomplete result.")
                                    // 필요에 따라 사용자에게 알리거나 스캔을 계속할 수 있습니다.
                                }

                            } else {
                                Log.e("API", "서버 응답 오류: ${response.code()}")
                                // 오류 처리 (사용자에게 알림 등)
                            }
                            // API 호출 완료 후 필요한 상태 정리 (예: ocrTextList.clear() 등)
                            // ocrTextList.clear() // 다음 프레임의 결과를 받기 위해 클리어
                        }

                        override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                            Log.e("API", "요청 실패: ${t.message}", t)
                            // 오류 처리 (사용자에게 알림 등)
                            // API 호출 완료 후 필요한 상태 정리
                            // ocrTextList.clear()
                        }
                    })

                    // 주의: API 호출 후 ocrTextList를 바로 clear하면 다음 프레임 결과와 섞이지 않지만,
                    // 여러 프레임에 걸친 텍스트를 모아서 분석해야 한다면 clear 시점을 조절해야 합니다.

                } else {
                    // 추출된 영양 정보나 유통기한이 없는 경우
                    Log.d("CameraScreen", "No significant text found for processing.")
                    // 필요에 따라 사용자에게 알리거나 스캔을 계속합니다.
                    // ocrTextList.clear() // 다음 프레임 결과를 위해 클리어
                }
            }
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
//                    Text(
//                        text = expiryText.ifBlank { "없음" },
//                        modifier = Modifier.padding(bottom = 12.dp)
//                    )
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