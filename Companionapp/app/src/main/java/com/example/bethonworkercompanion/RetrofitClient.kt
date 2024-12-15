package com.example.bethonworkercompanion

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val PRIMARY_BASE_URL = "http://10.6.128.19:5000"  // School IP

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val client = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor())
        .addInterceptor { chain ->  // Add Content-Type interceptor
            val original = chain.request()
            val request = original.newBuilder()
                .header("Content-Type", "application/json") // Set Content-Type header
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(PRIMARY_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val instance: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
        