package com.example.bethonworkercompanion

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Field

class RetryInterceptor : Interceptor {
    private val primaryBaseUrl = "http://10.6.128.19"
    private val secondaryBaseUrl = "http://88.115.201.36"

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val response: Response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // If the primary IP fails, retry with the secondary IP
            val urlField: Field = request::class.java.getDeclaredField("url")
            urlField.isAccessible = true
            val url = urlField.get(request) as okhttp3.HttpUrl
            val newUrl = url.newBuilder()
                .host(secondaryBaseUrl)
                .build()
            request = request.newBuilder().url(newUrl).build()
            chain.proceed(request)
        }
        return response
    }
}