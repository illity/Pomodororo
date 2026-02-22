package com.pomodororo.data.mapper

import com.pomodororo.model.PomodoroModel
import com.pomodororo.data.PomodoroStatsEntity

fun PomodoroStatsEntity.toModel(): PomodoroModel =
    PomodoroModel(
        tag = tag,
        focusSeconds = focusSeconds,
        restSeconds = restSeconds,
        currentPhase = currentPhase,
        remainingSeconds = remainingSeconds,
        completedSessions = completedSessions,
        totalSessions = totalSessions,
        isRunning = isRunning,
        doneSessions = doneSessions
    )

fun PomodoroModel.toEntity(): PomodoroStatsEntity =
    PomodoroStatsEntity(
        id = 0, // always 0 (single row pattern)
        tag = tag,
        focusSeconds = focusSeconds,
        restSeconds = restSeconds,
        currentPhase = currentPhase,
        remainingSeconds = remainingSeconds,
        completedSessions = completedSessions,
        totalSessions = totalSessions,
        isRunning = isRunning,
        doneSessions = doneSessions
    )