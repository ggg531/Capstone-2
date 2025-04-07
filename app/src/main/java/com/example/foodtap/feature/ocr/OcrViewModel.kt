package com.example.foodtap.feature.ocr

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val _ocrResult = MutableStateFlow("처리 중...")
    val ocrResult: StateFlow<String> = _ocrResult

    private val ocrUrl = "https://nmk7zjhci8.apigw.ntruss.com/custom/v1/40521/c6ffe0f030ac2018426b489a7f8f356ec0fed0941d31da1b8ddec3c51e69c104/general"
    private val ocrSecret = "TGVQSE1SSmlIZXpObndaV1Z5dWlTclFkcXJZY01xRG0="

    fun startOcr(bitmap: Bitmap) {
        performClovaOcr(bitmap)
    }

    private fun performClovaOcr(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = buildOcrRequest(bitmap)
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val text = parseOcrResult(responseBody)
                    _ocrResult.value = text
                } else {
                    _ocrResult.value = "OCR 실패: ${response.code}\n${responseBody ?: "응답 없음"}"
                }
            } catch (e: Exception) {
                _ocrResult.value = "예외 발생: ${e.message ?: "Unknown error"}"
            }
        }
    }

    private fun buildOcrRequest(bitmap: Bitmap): Request {
        val base64Image = bitmapToBase64(bitmap)

        val imageObject = JSONObject().apply {
            put("format", "jpg")
            put("name", "demo")
            put("data", base64Image)
        }

        val jsonBody = JSONObject().apply {
            put("version", "V2")
            put("requestId", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis())
            put("images", JSONArray().put(imageObject))
        }

        return Request.Builder()
            .url(ocrUrl)
            .addHeader("X-OCR-SECRET", ocrSecret)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
    }

    private fun parseOcrResult(response: String): String {
        val fields = JSONObject(response)
            .getJSONArray("images")
            .getJSONObject(0)
            .getJSONArray("fields")

        return buildString {
            for (i in 0 until fields.length()) {
                append(fields.getJSONObject(i).getString("inferText"))
                append("\n")
            }
        }.trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
