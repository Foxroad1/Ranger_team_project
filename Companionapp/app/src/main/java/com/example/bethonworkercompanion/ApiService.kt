package com.example.bethonworkercompanion

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("/profile")
    suspend fun getUserProfile(): Response<User>

    @FormUrlEncoded
    @POST("/log_start_time")
    suspend fun logStartTime(@Field("qrCode") qrCode: String): Response<Void>

    @FormUrlEncoded
    @POST("/log_end_time")
    suspend fun logEndTime(): Response<Void>
}