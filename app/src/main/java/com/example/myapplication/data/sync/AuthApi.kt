package com.example.myapplication.data.sync

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val childId: String,
    val deviceId: String,
    val label: String
)

data class RegisterResponse(
    val success: Boolean,
    val childId: String
)

interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<RegisterResponse>
}