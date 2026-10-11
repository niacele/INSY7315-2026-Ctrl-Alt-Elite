package com.example.employeedigitalhandbook.api

import com.example.employeedigitalhandbook.data.BragBook
import com.example.employeedigitalhandbook.data.CommunityEvent
import com.example.employeedigitalhandbook.data.Doctor
import com.example.employeedigitalhandbook.data.Policy
import com.example.employeedigitalhandbook.data.Quiz
import com.example.employeedigitalhandbook.data.QuizSubmission
import com.example.employeedigitalhandbook.data.Resource
import com.example.employeedigitalhandbook.api.ApiResponse
import com.example.employeedigitalhandbook.sessions.UserSessionPayload
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    //corrected routes for login, community and quizzes

//    @POST("api/authapi/login")
//    suspend fun login(@Body request: ApiLoginRequest): Response<ApiLoginResponse>


    //TEST - trying to make pdfs show
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>
    @POST("api/Auth/login")
    suspend fun login(@Body request: ApiLoginRequest): Response<ApiLoginResponse>

    @POST("api/Users/sessions/log")
    suspend fun logUserSession(@Body payload: UserSessionPayload): Response<ResponseBody>

    // --- COMMUNITY EVENTS ---
//    @GET("api/CommunityEvents")
//    suspend fun getCommunityEvents(): Response<List<CommunityEvent>>
    @GET("api/Community")
    suspend fun getCommunityEvents(): Response<ApiResponse<List<CommunityEvent>>>

    // --- DOCTORS ---
    // Corrected in ApiService.kt
    @GET("api/Doctors")
    suspend fun getDoctors(): Response<ApiResponse<List<Doctor>>>

    @GET("api/Doctors/{id}")
    suspend fun getDoctorById(@Path("id") id: Int): Response<ApiResponse<Doctor>>

    // --- BRAG BOOK ---
    @GET("api/bragbook")
    suspend fun getBragPosts(): Response<ApiResponse<List<BragBook>>>

    @POST("api/bragbook")
    suspend fun createBragPost(@Body post: BragBook): Response<ApiResponse<BragBook>>

    // --- QUIZZES ---
//    @GET("api/Quizs")
//    suspend fun getQuizzes(): Response<List<Quiz>>

    @GET("api/Quizzes")
    suspend fun getQuizzes(): Response<ApiResponse<List<Quiz>>>

    @GET("api/Quizzes/{id}")
    suspend fun getQuizById(@Path("id") id: Int): Response<ApiResponse<Quiz>>

    // Unwrapped fallback
    @GET("api/Quizzes/{id}")
    suspend fun getRawQuizById(@Path("id") id: Int): Response<Quiz>

    @POST("api/quizzes/submit")
    suspend fun submitQuizResult(@Body submission: QuizSubmission): Response<ApiResponse<QuizSubmission>>

    // --- RESOURCE ---
    @GET("api/resources")
    suspend fun getResources(): Response<ApiResponse<List<Resource>>>

    @GET("api/resources/{id}")
    suspend fun getResourceById(@Path("id") id: Int): Response<ApiResponse<Resource>>

    // --- POLICY ---
    @GET("api/policies")
    suspend fun getPolicies(): Response<ApiResponse<List<Policy>>>

    @GET("api/policies/{id}")
    suspend fun getPolicyById(@Path("id") id: Int): Response<ApiResponse<Policy>>
}