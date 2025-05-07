package com.example.foodtap.api

data class OcrResponse(
    val approval: Boolean,
    val allergy: List<String>,
    val desc: String
)

data class OcrRequest(
    val ocr: String
)