package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.dto.RegisterRequest
import com.example.myapplication.data.remote.dto.SyncRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): Response<Unit>
}
