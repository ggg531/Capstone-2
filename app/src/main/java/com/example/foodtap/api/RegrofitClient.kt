package com.example.foodtap.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://6dycp5qklj.execute-api.ap-northeast-2.amazonaws.com"
    private const val AI_BASE_URL = "https://9zm59ookul.execute-api.ap-northeast-2.amazonaws.com"

    val put_instance: PutUserIdService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(PutUserIdService::class.java)
    }

    val get_instance: GetUserIdService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(GetUserIdService::class.java)
    }

/*
    val allergy_keyword_instance: AllergyKeywordService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("$BASE_URL")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(AllergyKeywordService::class.java)
    }

 */

    val get_approval: GetApprovalService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(GetApprovalService::class.java)
    }
}
