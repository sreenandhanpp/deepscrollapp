package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val usageMinutes: Int = 0,
    val reelsViewed: Int = 0,
    val deepScrollCount: Int = 0,
    val sessions: Int = 0,
    val rapidScrollCount: Int = 0,
    val mindlessMinutes: Float = 0f,
    val deepScrollStreak: Int = 0,
    val intensityScore: Float = 0f
)
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val enqueuedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
