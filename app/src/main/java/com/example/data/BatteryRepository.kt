package com.example.data

import com.example.model.BatterySessionLog
import com.example.model.BenchmarkEntity
import kotlinx.coroutines.flow.Flow

class BatteryRepository(private val batteryDao: BatteryDao) {

    val recentSessions: Flow<List<BatterySessionLog>> = batteryDao.getRecentSessionLogs()
    val benchmarks: Flow<List<BenchmarkEntity>> = batteryDao.getAllBenchmarks()

    suspend fun logSession(sessionLog: BatterySessionLog): Long {
        return batteryDao.insertSessionLog(sessionLog)
    }

    suspend fun saveBenchmark(benchmark: BenchmarkEntity): Long {
        return batteryDao.insertBenchmark(benchmark)
    }

    suspend fun clearHistory() {
        batteryDao.clearSessionLogs()
        batteryDao.clearBenchmarks()
    }
}
