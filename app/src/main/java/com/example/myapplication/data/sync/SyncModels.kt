package com.example.myapplication.data.sync

data class SyncRequest(
    val deviceId: String,
    val stats: List<StatDto>
)

data class StatDto(
    val date: String,
    val reelsViewed: Int,
    val deepScrollCount: Int,
    val usageMinutes: Int,
    val sessions: Int
)

data class SyncResponse(
    val success: Boolean,
    val synced: Int
)