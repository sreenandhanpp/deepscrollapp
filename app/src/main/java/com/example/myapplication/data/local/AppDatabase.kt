package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyStatEntity::class, SyncQueueEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun syncQueueDao(): SyncQueueDao
}
