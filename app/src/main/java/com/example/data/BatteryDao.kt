package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.BatterySessionLog
import com.example.model.BenchmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_session_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentSessionLogs(): Flow<List<BatterySessionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionLog(log: BatterySessionLog): Long

    @Query("SELECT * FROM cable_benchmarks ORDER BY timestamp DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(benchmark: BenchmarkEntity): Long

    @Query("DELETE FROM battery_session_logs")
    suspend fun clearSessionLogs()

    @Query("DELETE FROM cable_benchmarks")
    suspend fun clearBenchmarks()
}
