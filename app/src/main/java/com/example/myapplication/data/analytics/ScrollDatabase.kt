package com.example.myapplication.data.analytics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScrollDailyStats::class],
    version = 1
)
abstract class ScrollDatabase : RoomDatabase() {

    abstract fun statsDao(): ScrollStatsDao

    companion object {

        @Volatile
        private var INSTANCE: ScrollDatabase? = null

        fun getDatabase(context: Context): ScrollDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollDatabase::class.java,
                    "scroll_db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}