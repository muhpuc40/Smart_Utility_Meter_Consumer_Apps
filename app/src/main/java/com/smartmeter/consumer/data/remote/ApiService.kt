package com.smartmeter.consumer.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

// Consumer API: /consumer/*
interface ApiService {

    @POST("consumer/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginData>>

    @GET("consumer/profile")
    suspend fun profile(): Response<ApiResponse<Profile>>

    @PATCH("consumer/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<ApiResponse<ConsumerInfo>>

    @PATCH("consumer/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<MessageResponse>

    @GET("consumer/balance")
    suspend fun balance(): Response<ApiResponse<BalanceInfo>>

    @GET("consumer/usage/summary")
    suspend fun usageSummary(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiResponse<UsageSummary>>

    @GET("consumer/usage/readings")
    suspend fun readings(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ApiResponse<List<Reading>>>

    @POST("consumer/recharges")
    suspend fun createRecharge(@Body body: RechargeRequest): Response<ApiResponse<Recharge>>

    @GET("consumer/recharges")
    suspend fun recharges(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("status") status: String?,
    ): Response<ApiResponse<List<Recharge>>>

    @GET("consumer/transactions")
    suspend fun transactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<ApiResponse<List<Transaction>>>
}
