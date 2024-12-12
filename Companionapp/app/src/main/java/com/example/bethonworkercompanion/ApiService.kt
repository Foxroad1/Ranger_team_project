package com.example.bethonworkercompanion

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("/profile")
    suspend fun getUserProfile(): Response<User>

    @POST("/log_start_time")
    suspend fun logStartTime(@Body qrCode: String): Response<Void>

    @POST("/log_end_time")
    suspend fun logEndTime(): Response<Void>

}