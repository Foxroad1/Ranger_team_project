package com.example.bethonworkercompanion

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val PRIMARY_BASE_URL = "http://10.6.128.19:5000"  // School IP

    private val client = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor())  // No need to cast explicitly
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(PRIMARY_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}