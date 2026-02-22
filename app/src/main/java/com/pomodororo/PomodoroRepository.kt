package com.pomodororo

import com.pomodororo.model.PomodoroModel
import com.pomodororo.data.mapper.*
import com.pomodororo.data.PomodoroStatsDao


class PomodoroRepository(
    private val dao: PomodoroStatsDao
) {

    suspend fun load(): PomodoroModel {
        return dao.getStats()?.toModel()
            ?: PomodoroModel() // default if DB empty
    }

    suspend fun save(model: PomodoroModel) {
        dao.update(model.toEntity())
    }
}