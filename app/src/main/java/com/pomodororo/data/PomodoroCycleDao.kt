package com.pomodororo.data

import androidx.room.*
import com.pomodororo.model.PomodoroCycleModel

@Dao
interface PomodoroCycleDao {

    @Query("SELECT pomodoro_cycle.*, color FROM pomodoro_cycle LEFT JOIN pomodoro_tag on pomodoro_cycle.tag = pomodoro_tag.tag WHERE active = 1")
    suspend fun getActiveCycle(): PomodoroCycleModel?

    @Insert
    suspend fun insert(entity: PomodoroCycleEntity): Long

    @Upsert
    suspend fun upsert(value: PomodoroCycleEntity)
}