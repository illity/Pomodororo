package com.pomodororo.model

data class PomodoroCycleModel(
    val id: Int = 0,
    val tag: String = "study",
    val focusSeconds: Int = 25 * 60,
    val restSeconds: Int = 5 * 60,
    val currentPhase: String = "focus",
    val remainingSeconds: Int = 25 * 60,
    var completedSessions: Int = 0,
    val totalSessions: Int = 4,
    val isRunning: Boolean = false,
    val doneSessions: Int = 0,
    val active: Boolean = true
)
