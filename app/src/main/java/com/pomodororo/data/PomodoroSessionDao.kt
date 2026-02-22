package com.pomodororo.data

import androidx.room.*

@Dao
interface PomodoroSessionDao {

    @Query("SELECT * FROM pomodoro_session WHERE cycleId = :cycleId AND active = 1")
    suspend fun getActive(cycleId: Int): PomodoroSessionEntity?

    @Insert
    suspend fun insert(entity: PomodoroSessionEntity): Long

    @Upsert
    suspend fun upsert(value: PomodoroSessionEntity)
}