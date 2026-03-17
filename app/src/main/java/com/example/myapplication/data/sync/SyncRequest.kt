
package com.example.myapplication.data.sync

import com.example.myapplication.data.analytics.ScrollDailyStats

data class SyncRequest(
    val deviceId: String,
    val stats: List<ScrollDailyStats>
)