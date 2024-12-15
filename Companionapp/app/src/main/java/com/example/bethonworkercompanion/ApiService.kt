package com.example.bethonworkercompanion

import retrofit2.Call
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

    @POST("/api/log_start_time")
    suspend fun logStartTime(@Header("Authorization") token: String, @Body qrCode: Map<String, String>): Response<Void>

    @FormUrlEncoded
    @POST("/api/log_end_time")
    suspend fun logEndTime(): Response<Void>

    @GET("/api/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<User>

    @POST("logout")
    fun logout(): Call<Void>
}