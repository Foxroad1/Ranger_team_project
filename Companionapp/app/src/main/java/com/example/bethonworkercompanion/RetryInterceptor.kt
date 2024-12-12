package com.example.bethonworkercompanion

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {
    private val secondaryBaseUrl = "http://88.115.201.36" //Home IP

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            // If the primary IP fails, retry with the secondary IP
            val newUrl = request.url.newBuilder()
                .host(secondaryBaseUrl)
                .build()
            request = request.newBuilder().url(newUrl).build()
            chain.proceed(request)
        }
    }
}