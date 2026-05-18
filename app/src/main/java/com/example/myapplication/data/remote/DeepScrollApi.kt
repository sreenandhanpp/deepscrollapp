package com.example.myapplication.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepScrollApi {
    @POST("/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("/sync")
    suspend fun sync(@Body body: SyncRequest): Response<Unit>
}
