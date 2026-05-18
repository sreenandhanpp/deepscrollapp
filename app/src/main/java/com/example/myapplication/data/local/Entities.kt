package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val date: String,
    val reelsViewed: Int,
    val deepScrollCount: Int,
    val usageMinutes: Int,
    val sessions: Int,
    val deepScrollStreak: Int,
    val intensityScore: Float
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val enqueuedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
