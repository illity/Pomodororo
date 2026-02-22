// data/PomodoroStatsEntity.kt

package com.pomodororo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_stats")
data class PomodoroStatsEntity(
    @PrimaryKey val id: Int = 0, // always 0 (single row)
    val tag: String = "study",
    val focusSeconds: Int = 25 * 60,
    val restSeconds: Int = 5 * 60,
    val currentPhase: String = "focus",
    val remainingSeconds: Int = 25 * 60,
    var completedSessions: Int = 0,
    val totalSessions: Int = 4,
    val isRunning: Boolean = false,
    val doneSessions: Int = 0
)