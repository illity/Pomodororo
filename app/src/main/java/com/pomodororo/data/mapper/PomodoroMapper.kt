package com.pomodororo.data.mapper

import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.data.PomodoroCycleEntity
import com.pomodororo.data.PomodoroSessionEntity
import com.pomodororo.data.TagEntity
import com.pomodororo.model.PomodoroSessionModel
import com.pomodororo.model.TagModel


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

fun PomodoroSessionModel.toEntity(): PomodoroSessionEntity =
    PomodoroSessionEntity(
        id = id,
        cycleId = cycleId,
        tag = tag,
        endTime = endTime,
        active = active,
        currentPhase = currentPhase
    )

fun TagEntity.toModel(): TagModel = TagModel(tag = this.tag, color = this.color)
fun TagModel.toEntity(): TagEntity = TagEntity(tag = this.tag, color = this.color)