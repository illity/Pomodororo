package com.pomodororo.data

import androidx.room.*
import com.pomodororo.model.PomodoroSessionModel
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {

    @Query("SELECT pomodoro_session.*, color FROM pomodoro_session left join pomodoro_tag on pomodoro_session.tag = pomodoro_tag.tag WHERE cycleId = :cycleId")
    suspend fun getAll(cycleId: Int): List<PomodoroSessionModel>

    @Query("SELECT pomodoro_session.*, color FROM pomodoro_session left join pomodoro_tag on pomodoro_session.tag = pomodoro_tag.tag WHERE cycleId = :cycleId AND active = 1")
    suspend fun getActive(cycleId: Int): PomodoroSessionModel?

    @Insert
    suspend fun insert(entity: PomodoroSessionEntity): Long

    @Upsert
    suspend fun upsert(value: PomodoroSessionEntity)

    @Query("SELECT * FROM pomodoro_session WHERE cycleId = :cycleId")
    fun observeSessions(cycleId: Long): Flow<List<PomodoroSessionEntity>>
}