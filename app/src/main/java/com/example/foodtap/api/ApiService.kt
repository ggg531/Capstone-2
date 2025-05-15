package com.example.foodtap.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PutUserIdService {
    @PUT("/user/{id}")
    fun putUser(
        @Path("id") id: String,
    ): Call<String>
}

interface GetUserIdService {
    @GET("/user/{id}")
    fun getUser(
        @Path("id") id: String,
    ): Call<UserData>
}

interface DeleteUserIdService {
    @DELETE("/user/{id}")
    fun deleteUser(
        @Path("id") id: String,
    ): Call<String>
}

interface GetApprovalService {
    @Headers("Content-Type: application/json")
    @POST("/test")
    fun getApproval(
        @Body request: OcrRequest
    ): Call<OcrResponse>
}

// for Naver CLOVA OCR
interface ClovaOcrService {
    @POST("general")
    fun sendOcrRequest(
        @Body requestBody: RequestBody,
        @Header("X-OCR-SECRET") secret: String = "WkJkVlhuT3lsVGFoSUZhZ1BhT3BZd3VmekxOY0p1T1c="
    ): Call<ClovaOcrResponse>
}

interface SttService {
    @Headers("Content-Type: application/json")
    @POST("/stt")
    fun stt2Allergy(@Body request: SttRequest): Call<SttResponse>
}

