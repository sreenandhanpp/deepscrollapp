package com.example.myapplication.data.sync

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncApi {

    @POST("sync")
    suspend fun syncStats(
        @Body body: SyncRequest
    ): Response<SyncResponse>
}