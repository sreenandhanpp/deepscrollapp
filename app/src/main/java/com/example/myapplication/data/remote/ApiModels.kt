package com.example.myapplication.data.remote

data class RegisterRequest(val childId: String, val deviceId: String, val label: String)
data class RegisterResponse(val childId: String? = null, val success: Boolean = true)

data class SyncStatDto(
    val date: String,
    val reelsViewed: Int,
    val deepScrollCount: Int,
    val usageMinutes: Int,
    val sessions: Int
)

data class SyncRequest(val deviceId: String, val stats: List<SyncStatDto>)
