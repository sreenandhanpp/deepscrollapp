package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String, // format YYYY-MM-DD
    val reelsViewed: Int = 0,
    val deepScrollCount: Int = 0,
    val usageMinutes: Int = 0,
    val sessions: Int = 0
)
