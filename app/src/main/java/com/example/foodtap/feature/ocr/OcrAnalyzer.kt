package com.example.foodtap.feature.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions // 한국어 옵션 추가
import java.util.concurrent.ExecutorService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.foodtap.api.OcrRequest
import com.example.foodtap.api.OcrResponse
import com.example.foodtap.api.RetrofitClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// API 호출 중복 방지를 위한 AtomicBoolean
private val isApiCallInProgress = AtomicBoolean(false)

class OcrAnalyzer(
    private val executor: ExecutorService, // 백그라운드 스레드에서 이미지 분석 수행
    private val onProcessedResult: (OcrResponse?) -> Unit // ViewModel로 OCR 결과 및 API 응답을 전달할 콜백
) {
    // Google ML Kit TextRecognizer 인스턴스를 클래스 프로퍼티로 선언하고 한국어 인식기 사용
    private val recognizer: TextRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    // 마지막으로 외부 API 호출을 시도한 시간을 기록하는 변수
    private var lastApiCallTime = 0L
    // 외부 API 호출 간 최소 간격 (0.5초 = 500밀리초)
    private val MIN_API_CALL_INTERVAL_MS = 500L

    fun create(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(executor) { imageProxy ->
                    analyzeImage(imageProxy)
                }
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val currentTime = System.currentTimeMillis()

        // 이미 API 호출이 진행 중이거나, 마지막 호출로부터 최소 간격이 지나지 않았다면
        // 현재 프레임은 분석하지 않고 건너뜜
        if (isApiCallInProgress.get() || (currentTime - lastApiCallTime < MIN_API_CALL_INTERVAL_MS)) {
            imageProxy.close()
            return
        }

        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // API 호출을 시도하기 직전에 플래그 및 시간 업데이트
            isApiCallInProgress.set(true)
            lastApiCallTime = currentTime

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text.trim()

                    if (detectedText.isNotBlank()) {
                        // ML Kit으로 인식된 텍스트를 외부 API로 전송
                        callExternalApi(detectedText)
                    } else {
                        // 텍스트가 없으면 API 호출 플래그 해제
                        isApiCallInProgress.set(false)
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e("OcrAnalyzer", "ML Kit OCR 인식 실패", e)
                    // 실패 시 API 호출 플래그 해제
                    isApiCallInProgress.set(false)
                }
                .addOnCompleteListener {
                    // 이미지 프록시 닫기 (OCR 분석이 완료되면)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun callExternalApi(ocrText: String) {
        // 줄바꿈 문자를 전부 공백으로 변경
        val processedOcrText = ocrText.replace("\n", " ")

        // get_approval 호출 전에 전체 inferred text를 로그로 띄움
        Log.d("OcrAnalyzer", "Sending to external API (processed): $processedOcrText") // 수정된 로그

        val request = OcrRequest(ocr = processedOcrText) // ML Kit으로 인식된 전체 텍스트


        RetrofitClient.get_approval.getApproval(request).enqueue(object : Callback<OcrResponse> {
            override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                // API 호출 완료 후 플래그 초기화
                isApiCallInProgress.set(false)

                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    Log.d("OcrAnalyzer", "External API Response: $ocrResponse")
                    // Main 스레드에서 ViewModel 콜백 호출
                    MainScope().launch { onProcessedResult(ocrResponse) }
                } else {
                    Log.e("OcrAnalyzer", "External API HTTP 오류: ${response.code()} - ${response.message()}")
                    MainScope().launch { onProcessedResult(null) } // 오류 발생 시 null 전달
                }
            }

            override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                // API 호출 실패 후 플래그 초기화
                isApiCallInProgress.set(false)
                Log.e("OcrAnalyzer", "External API 요청 실패: ${t.message}", t)
                MainScope().launch { onProcessedResult(null) } // 오류 발생 시 null 전달
            }
        })
    }
}