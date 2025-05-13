package com.example.foodtap.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://6dycp5qklj.execute-api.ap-northeast-2.amazonaws.com"
    private const val AI_BASE_URL = "https://9zm59ookul.execute-api.ap-northeast-2.amazonaws.com"
    private const val CLOVA_URL = "https://1nx7cqsn3a.apigw.ntruss.com/custom/v1/41762/110b3733b61c6b2e4147e94139307d757d9c0780603f4b9b428a33959afff854/"

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

    val get_approval: GetApprovalService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(GetApprovalService::class.java)
    }

    val stt2Allergy: SttService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(SttService::class.java)
    }

    val instance: ClovaOcrService by lazy {
        Retrofit.Builder()
            .baseUrl(CLOVA_URL)
            .addConverterFactory(GsonConverterFactory.create())  // <-- 이 부분 중요
            .client(OkHttpClient.Builder().build())
            .build()
            .create(ClovaOcrService::class.java)
    }
}
