package com.example.foodtap.feature.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import java.util.concurrent.ExecutorService

class OcrAnalyzer(
    private val recognizer: TextRecognizer,
    private val executor: ExecutorService,
    private val onTextDetected: (String) -> Unit
) {
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

        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    val firstBlock = visionText.textBlocks.firstOrNull()

                    if (text.isNotBlank()) {
                        onTextDetected(text)
                    }
                }
                .addOnFailureListener { e : Exception  ->
                    Log.e("OcrAnalyzer", "OCR 인식 실패", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
