package com.example.employeedigitalhandbook.api

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object ApiClient {
    // 10.0.2.2 for emulator
//    private const val BASE_URL = "http://10.0.2.2:5000/"

    //connecting android app to API
    private const val BASE_URL = "https://handbook-api-770247469632.europe-west3.run.app/"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    //cache interceptor for doctor, policy + resource requests
    private val cacheInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        val isGet = request.method.equals("GET", ignoreCase = true)
        val isDoctorEndpoint = request.url.encodedPath.contains("doctor", ignoreCase = true)
        val isPolicyEndpoint = request.url.encodedPath.contains("policy", ignoreCase = true)
        val isResourcesEndpoint = request.url.encodedPath.contains("resource", ignoreCase = true)

        val isCacheableEndpoint = isDoctorEndpoint || isPolicyEndpoint || isResourcesEndpoint

        if (isGet && response.isSuccessful && isCacheableEndpoint) {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=21600")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }

    private val okHttpClient : OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor (cacheInterceptor)

        appContext?.cacheDir?.let { cacheDirectory ->
            val cacheSize = 10L * 1024 * 1024
            builder.cache(Cache(File(cacheDirectory, "http_cach"), cacheSize))
        }

        builder.build()
    }


    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun clearCache() {
        try {
            okHttpClient.cache?.evictAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}