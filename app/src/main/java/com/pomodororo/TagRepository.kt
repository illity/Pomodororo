package com.pomodororo

import android.util.Log
import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.data.mapper.*
import com.pomodororo.data.PomodoroCycleDao
import com.pomodororo.data.PomodoroSessionDao
import com.pomodororo.data.TagDao
import com.pomodororo.data.TagEntity
import com.pomodororo.model.PomodoroSessionModel
import com.pomodororo.model.TagModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class TagRepository(
    private val cycleDao: PomodoroCycleDao,
    private val sessionDao: PomodoroSessionDao,
    private val tagDao: TagDao
) {

    suspend fun load(): PomodoroCycleModel {
        return cycleDao.getActiveCycle()
            ?: PomodoroCycleModel() // default if DB empty
    }

    suspend fun save(model: PomodoroCycleModel) {
        Log.d("PomodoroRepository", "save is called")
        cycleDao.upsert(model.toEntity())
    }

    suspend fun loadSessions(cycleId: Int): List<PomodoroSessionModel> {
        Log.d("PomodoroRepository", "LoadSession, cycleId: ${cycleId}")
        return sessionDao.getAll(cycleId)// default if DB empty
    }

    suspend fun saveSession(model: PomodoroSessionModel) {
        Log.d("PomodoroRepository", "saveSession is called. Session ${model.id} is saved")
        sessionDao.upsert(model.toEntity())
    }

    suspend fun next() {
        cycleDao.insert(PomodoroCycleModel().toEntity())
    }

    suspend fun nextSession(cycleId: Int): Long {
        val id = sessionDao.insert(PomodoroSessionModel(
            cycleId = cycleId
        ).toEntity())
        Log.d("PomodoroRepository", "nextSession is called. inserted to cycleId ${cycleId}, ${id}")
        return id
    }

    suspend fun getAllTags(): List<TagModel> {
        return tagDao.getAll().map { it.toModel() }
    }

    suspend fun upsertTag(tag: TagModel) {
        tagDao.upsert(tag.toEntity())
    }


    suspend fun getColor(tag: String) : Long? {
        return tagDao.get(tag)
    }

}