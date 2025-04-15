package com.example.foodtap.api

import retrofit2.Call
import retrofit2.http.GET
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