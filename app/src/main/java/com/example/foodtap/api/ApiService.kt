package com.example.foodtap.api

import com.example.foodtap.api.allergy.KeywordResponse
import com.example.foodtap.api.allergy.TextRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
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

interface AllergyKeywordService {
    @POST("/extract_keywords")
    fun extractKeywords(@Body request: TextRequest): KeywordResponse
}