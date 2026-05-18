package com.example.myapplication.data.local

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class LocalStatsDataSource(private val dao: StatsDao) {
    fun observeAll(): Flow<List<DailyStatEntity>> = dao.observeAll()

    suspend fun updateToday(transform: (DailyStatEntity) -> DailyStatEntity) {
        val today = LocalDate.now().toString()
        val current = dao.getByDate(today) ?: DailyStatEntity(today, 0, 0, 0, 0, 0, 0f)
        dao.upsert(transform(current))
    }
}
