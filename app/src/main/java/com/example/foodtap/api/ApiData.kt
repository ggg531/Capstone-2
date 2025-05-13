package com.example.foodtap.api

data class OcrResponse(
    val approval: Boolean,
    val allergy: List<String>,
    val desc: String,
    val expiration: String
)

data class OcrRequest(
    val ocr: String
)

data class SttResponse(
    val userAllergy: List<String>
)

data class SttRequest(
    val stt: String
)