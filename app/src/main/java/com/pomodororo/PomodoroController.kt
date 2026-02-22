package com.pomodororo

import android.content.Context
import androidx.room.Room
import com.pomodororo.data.AppDatabase
import com.pomodororo.data.PomodoroStatsDao
import com.pomodororo.data.PomodoroStatsEntity
import com.pomodororo.data.mapper.toEntity
import com.pomodororo.data.mapper.toModel
import com.pomodororo.model.PomodoroModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object PomodoroController {


    private lateinit var database: AppDatabase
    private lateinit var statsDao: PomodoroStatsDao

    fun init(context: Context) {
        println("first")
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "pomodoro_db"
        ).build()

        statsDao = database.statsDao()

        // Ensure row exists
        CoroutineScope(Dispatchers.IO).launch {
            val stats = statsDao.getStats()
            if (stats == null) {
                statsDao.insert(PomodoroStatsEntity())
            }  else {
                _state.value = stats.toModel()
            }
        }
    }

    private val _state = MutableStateFlow(PomodoroModel())
    val state: StateFlow<PomodoroModel> = _state.asStateFlow()

    private var job: Job? = null

    fun togglePlayPause() {
        if (_state.value.isRunning) stopTimer() else startTimer()
        save()

    }

    private fun startTimer() {
        if(_state.value.completedSessions == _state.value.totalSessions) {
            _state.value = _state.value.copy(
                completedSessions = 0
            )
        }
        _state.value = _state.value.copy(isRunning = true)
        job = CoroutineScope(Dispatchers.Default).launch {
            while (_state.value.remainingSeconds > 0 && _state.value.isRunning) {
                delay(1000)
                _state.value = _state.value.copy(
                    remainingSeconds = _state.value.remainingSeconds - 1
                )
            }
            phaseCheck()
        }
    }

    private fun phaseCheck() {
        if (_state.value.completedSessions == _state.value.totalSessions) {
            cancel()
        }
        if (_state.value.remainingSeconds <= 0) {

            // switch phase
            if (_state.value.currentPhase == "focus") _state.value = _state.value.copy(
                doneSessions = _state.value.doneSessions + 1
            )
            if (_state.value.currentPhase == "rest") _state.value = _state.value.copy(
                completedSessions = _state.value.completedSessions + 1
            )
            val nextPhase = if (_state.value.currentPhase == "focus") "rest" else "focus"
            val nextSeconds = if (nextPhase == "focus") _state.value.focusSeconds else _state.value.restSeconds
            _state.value = _state.value.copy(currentPhase = nextPhase,
                remainingSeconds = nextSeconds,
                isRunning = false
            )
            save()
            job?.cancel()
        }
    }


    private fun save() {
        println("save is called")
        CoroutineScope(Dispatchers.IO).launch {
            val stats = statsDao.getStats()
            println(stats?.remainingSeconds)
            println(_state.value.toEntity().remainingSeconds)
            statsDao.update(_state.value.toEntity())
            val stats2 = statsDao.getStats()
            println(stats2?.remainingSeconds)
        }
    }
    private fun stopTimer() {
        _state.value = _state.value.copy(isRunning = false)
        save()
        job?.cancel()
    }

    fun cancel() {
        _state.value = PomodoroModel(
            doneSessions = _state.value.doneSessions
        )
        stopTimer()
    }

    fun restart() {
        stopTimer()
        _state.value = _state.value.copy(
            remainingSeconds = if (_state.value.currentPhase == "focus") _state.value.focusSeconds else _state.value.restSeconds
        )
//        startTimer()
    }

    fun skip() {
        _state.value = _state.value.copy(
            remainingSeconds = 0,
        )
        phaseCheck()
    }
}
