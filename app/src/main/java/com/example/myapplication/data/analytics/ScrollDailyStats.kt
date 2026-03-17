package com.example.myapplication.data.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scroll_daily_stats")
data class ScrollDailyStats(

    @PrimaryKey
    val date: String,

    val reelsViewed: Int = 0,
    val deepScrollCount: Int = 0,
    val usageMinutes: Int = 0,
    val sessions: Int = 0,

    val isSynced: Boolean = false // 🔥 NEW
)