package com.pomodororo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_session")
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cycleId: Int,
    val tag: String = "study",
    val endTime: Long,
    val active: Boolean = true,
    val currentPhase: String = "focus"
)