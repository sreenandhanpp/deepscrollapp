package com.example.myapplication.data.remote.dto

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
