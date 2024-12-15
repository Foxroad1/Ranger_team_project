package com.example.bethonworkercompanion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/log_start_time")
    suspend fun logStartTime(@Body qrCode: Map<String, String>): Response<Void>

    @FormUrlEncoded
    @POST("/log_end_time")
    suspend fun logEndTime(): Response<Void>

    @GET("/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<User>
}