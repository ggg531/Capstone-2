package com.example.foodtap.feature.ocr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import com.example.foodtap.api.RetrofitClient // RetrofitClient 인스턴스를 가져오는 코드가 있다고 가정
import com.example.foodtap.api.ClovaOcrResponse // API 응답 데이터 클래스
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean // API 호출 중복 방지

// API 호출 중복 방지를 위한 AtomicBoolean (기존과 동일하게 유지)
private val isApiCallInProgress = AtomicBoolean(false)

class ClovaOcrAnalyzer(
    private val executor: Executor,
    private val onOcrResult: (String) -> Unit // OCR 결과 처리를 위한 콜백 (필요하다면)
) : ImageAnalysis.Analyzer {

    // 마지막으로 API 호출을 시도한 시간을 기록하는 변수
    private var lastApiCallTime = 0L

    // API 호출 간 최소 간격 (0.5초 = 500밀리초)
    private val MIN_API_CALL_INTERVAL_MS = 500L

    // ImageProxy를 Bitmap으로 변환하는 헬퍼 함수 (YUV_420_888 형식 처리)
    private fun ImageProxy.toBitmap(): Bitmap? {
        @SuppressLint("UnsafeOptInUsageError") // Experimental usage
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        // JPEG로 압축 (Clova API 요구 형식에 따라 PNG로 변경 가능)
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 70, out) // 압축 품질 70% 예시
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // 비트맵을 Base64 문자열로 변환하는 함수
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // API 형식에 맞게 JPEG 또는 PNG 선택
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        // Clova API 문서 확인하여 NO_WRAP 또는 DEFAULT 사용
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }


    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // API 호출이 진행 중이거나, 마지막 호출로부터 최소 간격이 지나지 않았다면 새 프레임 무시
        if (isApiCallInProgress.get() || (currentTime - lastApiCallTime < MIN_API_CALL_INTERVAL_MS)) {
            imageProxy.close() // 프레임 사용 완료 알림
            return
        }

        // ImageProxy를 비트맵으로 변환 (또는 다른 원하는 형식으로)
        val bitmap = imageProxy.toBitmap()

        // 프레임 처리가 끝나면 반드시 close() 호출
        imageProxy.close()

        // 비트맵이 null이거나 유효하지 않으면 처리하지 않음
        if (bitmap == null) {
            return
        }

        // API 호출을 시도하기 직전에 시간 기록 및 플래그 설정
        lastApiCallTime = currentTime
        isApiCallInProgress.set(true)

        // 비트맵을 Base64 문자열로 변환
        val base64Image = bitmapToBase64(bitmap)

        // Clova OCR API 요청 JSON 생성
        val requestJson = JSONObject().apply {
            put("images", JSONArray().apply {
                put(JSONObject().apply {
                    put("format", "jpg") // toBitmap에서 JPEG으로 변환했으므로 "jpg"로 설정
                    put("name", "medium")
                    put("data", base64Image)
                })
            })
            put("lang", "ko")
            put("requestId", "string") // 더미 값 일단 넣어둠
            put("resultType", "string") // 더미 값 일단 넣어둠. 나중에 변경?
            put("timestamp", System.currentTimeMillis())
            put("version", "V1")
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        RetrofitClient.instance.sendOcrRequest(requestBody).enqueue(object : Callback<ClovaOcrResponse> {
            override fun onResponse(
                call: Call<ClovaOcrResponse>,
                response: Response<ClovaOcrResponse>
            ) {
                // API 호출 완료 후 플래그 초기화
                isApiCallInProgress.set(false)

                if (response.isSuccessful) {
                    val inferTextList = response.body()
                        ?.images
                        ?.flatMap { it.fields }
                        ?.map { it.inferText }
                        ?: emptyList()

                    Log.d("CLOVA_OCR", "inferText: $inferTextList")

                    // OCR 결과 처리 (UI 업데이트를 위해 메인 스레드로 전환)
                    MainScope().launch { onOcrResult(inferTextList.joinToString("\n")) }

                } else {
                    Log.e("CLOVA_OCR", "HTTP 오류: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ClovaOcrResponse>, t: Throwable) {
                // API 호출 실패 후 플래그 초기화
                isApiCallInProgress.set(false)
                Log.e("CLOVA_OCR", "요청 실패: ${t.message}", t)
            }
        })
    }
}