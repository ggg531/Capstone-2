package com.example.foodtap.api

data class OcrResponse(
    val approval: Boolean,
    val allergy: List<String>,
    val desc: String,
    val expiration: String,
    val product_name: String
)

data class OcrRequest(
    val ocr: String
)

data class SttResponse(
    val allergy: List<String>
)

data class SttRequest(
    val stt: String
)

data class ExpiData(
    val expi: String
)

data class ConfirmData(
    val confirm: Boolean
)

data class UserHist(
    val id: String,
    val allergy: String,
    val product_name: String
)

data class ProductNameRequest(
    val product_name: String
)