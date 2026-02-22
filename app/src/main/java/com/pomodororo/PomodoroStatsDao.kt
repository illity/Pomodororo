package com.pomodororo.data
// data/PomodoroStatsDao.kt
import androidx.room.*
import com.pomodororo.data.PomodoroStatsEntity
import com.pomodororo.model.PomodoroModel

@Dao
interface PomodoroStatsDao {

    @Query("SELECT * FROM pomodoro_stats WHERE id = 0")
    suspend fun getStats(): PomodoroStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: PomodoroStatsEntity)

//    @Query("UPDATE pomodoro_stats SET remainingSeconds = :value.remainingSeconds WHERE id = 0")
    @Update
    suspend fun update(value: PomodoroStatsEntity)
}