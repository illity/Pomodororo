package com.pomodororo.model

data class PomodoroSessionModel(
    val id: Int = 0,
    val cycleId: Int = 0,
    val tag: String = "study",
    val endTime: Long = 0,
    val active: Boolean = true
)
