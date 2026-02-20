// model/PomodoroModel.kt
package com.pomodororo.model

data class PomodoroModel(
    val tag: String = "study",
    val focusSeconds: Int = 25 * 60,
    val restSeconds: Int = 5 * 60,
    val currentPhase: String = "focus",
    val remainingSeconds: Int = 25 * 60,
    var completedSessions: Int = 0,
    val totalSessions: Int = 4,
    val isRunning: Boolean = false
)
