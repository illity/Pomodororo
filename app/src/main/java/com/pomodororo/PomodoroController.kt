package com.pomodororo

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pomodororo.data.AppDatabase
import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.model.PomodoroSessionModel
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
    private lateinit var dao: PomodoroRepository



    private val _state = MutableStateFlow(PomodoroCycleModel())
    private val _session = MutableStateFlow(PomodoroSessionModel())
    val state: StateFlow<PomodoroCycleModel> = _state.asStateFlow()

    fun init(context: Context) {
        Log.d("Controller", "init called")
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "pomodoro_db"
        ).build()

        dao = PomodoroRepository(database.cycleDao(),
                                 database.sessionDao())

        CoroutineScope(Dispatchers.IO).launch {
            println("try to load")
            val cycle = dao.load()
            val session = dao.loadSession(cycle.id)
            Log.d("Controller", cycle.tag)
            _state.value = cycle
            _session.value = session
        }
    }

    private var job: Job? = null

    fun togglePlayPause() {
        if (_state.value.isRunning) stopTimer() else startTimer()

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
        save()
    }

    private fun phaseCheck() {
        if (_state.value.completedSessions == _state.value.totalSessions) {
            cancel()
        }
        if (_state.value.remainingSeconds <= 0) {

            // switch phase
            if (_state.value.currentPhase == "focus") {
                _state.value = _state.value.copy(
                    doneSessions = _state.value.doneSessions + 1
                )
                saveSessionAndNext()
            }
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
            dao.save(_state.value)
        }
    }

    private fun saveSessionAndNext() {
        Log.d("Controller", "saveSessionAndNextCalled")
        CoroutineScope(Dispatchers.IO).launch {
            _session.value = _session.value.copy(
                active = false,
                endTime = System.currentTimeMillis()
            )
            dao.saveSession(_session.value)
            dao.nextSession(_state.value.id)
            _session.value = dao.loadSession(_state.value.id) //since has no active session in current cycle, create a new one
        }

    }

    private fun stopTimer() {
        _state.value = _state.value.copy(isRunning = false)
        save()
        job?.cancel()
    }

    fun cancel() {
        Log.d("Controller", "deactivating the ${_state.value.id}")
        _state.value = _state.value.copy(
            active = false
        )
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("Controller", "saving the ${_state.value.id}, current Status: ${_state.value.active}")
            dao.save(_state.value)
            Log.d("Controller", "creating a new model")
            dao.next()
            _state.value = dao.load()
            Log.d("Controller", "loaded model: ${_state.value.id}")
        }
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
