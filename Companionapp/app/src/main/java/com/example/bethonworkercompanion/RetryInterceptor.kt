package com.example.bethonworkercompanion

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.util.Log

class RetryInterceptor : Interceptor {
    private val secondaryBaseUrl = "10.6.128.19:5000" //falling to school IP on fail
    private val timeout = 22000 // 22 seconds

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        return try {
            chain.withConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                .withReadTimeout(timeout, TimeUnit.MILLISECONDS)
                .withWriteTimeout(timeout, TimeUnit.MILLISECONDS)
                .proceed(request)
        } catch (e: IOException) {
            Log.e("RetryInterceptor", "Primary URL failed: ${e.message}")

            // Extract hostname and port from secondaryBaseUrl
            val urlParts = secondaryBaseUrl.split(":")
            val hostname = urlParts[0]
            val port = urlParts[1].toInt()

            // Build new URL with extracted hostname and port
            val newUrl = request.url.newBuilder()
                .host(hostname)
                .port(port)
                .build()

            request = request.newBuilder().url(newUrl).build()
            try {
                chain.withConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .withReadTimeout(timeout, TimeUnit.MILLISECONDS)
                    .withWriteTimeout(timeout, TimeUnit.MILLISECONDS)
                    .proceed(request)
            } catch (e: IOException) {
                Log.e("RetryInterceptor", "Secondary URL failed: ${e.message}")
                throw e
            }
        }
    }
}