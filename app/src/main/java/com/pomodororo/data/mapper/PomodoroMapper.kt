package com.pomodororo.data.mapper

import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.data.PomodoroCycleEntity
import com.pomodororo.data.PomodoroSessionEntity
import com.pomodororo.model.PomodoroSessionModel

fun PomodoroCycleEntity.toModel(): PomodoroCycleModel =
    PomodoroCycleModel(
        id = id,
        tag = tag,
        focusSeconds = focusSeconds,
        restSeconds = restSeconds,
        currentPhase = currentPhase,
        remainingSeconds = remainingSeconds,
        completedSessions = completedSessions,
        totalSessions = totalSessions,
        isRunning = isRunning,
        doneSessions = doneSessions,
        active = active
    )

fun PomodoroCycleModel.toEntity(): PomodoroCycleEntity =
    PomodoroCycleEntity(
        id = id,
        tag = tag,
        focusSeconds = focusSeconds,
        restSeconds = restSeconds,
        currentPhase = currentPhase,
        remainingSeconds = remainingSeconds,
        completedSessions = completedSessions,
        totalSessions = totalSessions,
        isRunning = isRunning,
        doneSessions = doneSessions,
        active = active
    )

fun PomodoroSessionEntity.toModel(): PomodoroSessionModel =
    PomodoroSessionModel(
        id = id,
        cycleId = cycleId,
        tag = tag,
        endTime = endTime,
        active = active
    )

fun PomodoroSessionModel.toEntity(): PomodoroSessionEntity =
    PomodoroSessionEntity(
        id = id,
        cycleId = cycleId,
        tag = tag,
        endTime = endTime,
        active = active
    )