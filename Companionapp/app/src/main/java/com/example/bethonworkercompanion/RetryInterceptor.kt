package com.example.bethonworkercompanion

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import android.util.Log

class RetryInterceptor : Interceptor {
    private val secondaryBaseUrl = "88.115.201.36:5000"

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Log the error
            Log.e("RetryInterceptor", "Primary URL failed: ${e.message}")
            // If the primary IP fails, retry with the secondary IP
            val newUrl = request.url.newBuilder()
                .host(secondaryBaseUrl)
                .build()
            request = request.newBuilder().url(newUrl).build()
            try {
                chain.proceed(request)
            } catch (e: IOException) {
                Log.e("RetryInterceptor", "Secondary URL failed: ${e.message}")
                throw e
            }
        }
    }
}