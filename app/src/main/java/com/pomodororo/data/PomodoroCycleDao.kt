package com.pomodororo.data

import androidx.room.*

@Dao
interface PomodoroCycleDao {

    @Query("SELECT * FROM pomodoro_cycle WHERE active = 1")
    suspend fun getActiveCycle(): PomodoroCycleEntity?


    @Insert
    suspend fun insert(entity: PomodoroCycleEntity): Long

    @Upsert
    suspend fun upsert(value: PomodoroCycleEntity)
}