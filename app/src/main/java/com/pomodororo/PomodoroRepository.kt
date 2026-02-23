package com.pomodororo

import android.util.Log
import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.data.mapper.*
import com.pomodororo.data.PomodoroCycleDao
import com.pomodororo.data.PomodoroSessionDao
import com.pomodororo.model.PomodoroSessionModel


class PomodoroRepository(
    private val cycleDao: PomodoroCycleDao,
    private val sessionDao: PomodoroSessionDao
) {

    suspend fun load(): PomodoroCycleModel {
        return cycleDao.getActiveCycle()?.toModel()
            ?: PomodoroCycleModel() // default if DB empty
    }

    suspend fun save(model: PomodoroCycleModel) {
        Log.d("PomodoroRepository", "save is called")
        cycleDao.upsert(model.toEntity())
    }

    suspend fun loadSession(cycleId: Int): PomodoroSessionModel {
        Log.d("PomodoroRepository", "LoadSession, cycleId: ${cycleId}")
        return sessionDao.getActive(cycleId)?.toModel()
            ?: PomodoroSessionModel(
                cycleId = cycleId
            ) // default if DB empty
    }

    suspend fun saveSession(model: PomodoroSessionModel) {
        Log.d("PomodoroRepository", "saveSession is called. Session ${model.id} is saved")
        sessionDao.upsert(model.toEntity())
    }

    suspend fun next() {
        cycleDao.insert(PomodoroCycleModel().toEntity())
    }

    suspend fun nextSession(cycleId: Int) {
        val id = sessionDao.insert(PomodoroSessionModel(cycleId = cycleId).toEntity())
        Log.d("PomodoroRepository", "nextSession is called. inserted to cycleId ${cycleId}, ${id}")
    }

}