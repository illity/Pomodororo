package com.pomodororo

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.pomodororo.data.AppDatabase
import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.model.PomodoroSessionModel
import com.pomodororo.model.TagModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object PomodoroController {

    private lateinit var database: AppDatabase
    private lateinit var repository: PomodoroRepository

    /* -------------------- STATE -------------------- */

    private val _state = MutableStateFlow(PomodoroCycleModel())
    val state: StateFlow<PomodoroCycleModel> = _state.asStateFlow()

    private val _sessions = MutableStateFlow<List<PomodoroSessionModel>>(emptyList())
    val sessions: StateFlow<List<PomodoroSessionModel>> = _sessions.asStateFlow()

    private val _tags = MutableStateFlow<List<TagModel>>(emptyList())
    val tags: StateFlow<List<TagModel>> = _tags.asStateFlow()


    /** Derived current session (active one) */
    val currentSession: StateFlow<PomodoroSessionModel?> =
        _sessions.map { list -> list.firstOrNull { it.active } }
            .stateIn(
                CoroutineScope(Dispatchers.Default),
                SharingStarted.Eagerly,
                null
            )

    private var job: Job? = null

    /* -------------------- INIT -------------------- */

    fun init(context: Context) {

        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "pomodoro_db"
        ).build()

        repository = PomodoroRepository(
            database.cycleDao(),
            database.sessionDao(),
            database.tagDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            val cycle = repository.load()
            Log.d("Controller", "${cycle.color}")
            _state.value = cycle

            val sessions = repository.loadSessions(cycle.id)
            _sessions.value = sessions

            val tags = repository.getAllTags()
            _tags.value = tags
        }
    }

    /* -------------------- TIMER -------------------- */

    fun togglePlayPause() {
        if (_state.value.isRunning) stopTimer()
        else startTimer()
    }

    private fun startTimer() {

        if (_state.value.completedSessions == _state.value.totalSessions) {
            _state.value = _state.value.copy(completedSessions = 0)
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

        saveCycle()
    }

    private fun stopTimer() {
        _state.value = _state.value.copy(isRunning = false)
        saveCycle()
        job?.cancel()
    }

    /* -------------------- PHASE LOGIC -------------------- */

    private fun phaseCheck() {

        if (_state.value.remainingSeconds > 0) return

        if (_state.value.doneSessions == 4) {
            cancel()
        }



        if (_state.value.currentPhase == "focus") {

            _state.value = _state.value.copy(
                doneSessions = _state.value.doneSessions + 1
            )
            updateCurrentSession("rest")
        }



        if (_state.value.currentPhase == "rest") {
            _state.value = _state.value.copy(
                completedSessions = _state.value.completedSessions + 1
            )

            deactivateCurrentSession()
            if (_sessions.value.size < 4) {
                createNextSession("focus")
            }

        }

        val nextPhase =
            if (_state.value.currentPhase == "focus") "rest"
            else "focus"

        val nextSeconds =
            if (nextPhase == "focus")
                _state.value.focusSeconds
            else
                _state.value.restSeconds

        _state.value = _state.value.copy(
            currentPhase = nextPhase,
            remainingSeconds = nextSeconds,
            isRunning = false
        )

        saveCycle()
        job?.cancel()
    }

    /* -------------------- SESSION LOGIC -------------------- */
    private fun updateCurrentSession(currentPhase: String) {

        val current = currentSession.value ?: return

        val updated = current.copy(
            currentPhase = currentPhase,
            endTime = System.currentTimeMillis()
        )

        updateSessionInMemory(updated)

        CoroutineScope(Dispatchers.IO).launch {
            repository.saveSession(updated)
        }
    }


    private fun deactivateCurrentSession() {

        val current = currentSession.value ?: return

        val updated = current.copy(
            active = false,
            endTime = System.currentTimeMillis()
        )

        updateSessionInMemory(updated)

        CoroutineScope(Dispatchers.IO).launch {
            repository.saveSession(updated)
        }
    }

    private fun createNextSession(currentPhase: String) {

        CoroutineScope(Dispatchers.IO).launch {

            val id = repository.nextSession(_state.value.id)

            val newSession = PomodoroSessionModel(
                id = id.toInt(),
                cycleId = _state.value.id,
                active = true,
                currentPhase = currentPhase,
                color = _state.value.color
            )

            _sessions.value = _sessions.value + newSession
        }
    }

    private fun updateSessionInMemory(updated: PomodoroSessionModel) {
        _sessions.value = _sessions.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    /* -------------------- PERSISTENCE -------------------- */

    private fun saveCycle() {
        CoroutineScope(Dispatchers.IO).launch {
            repository.save(_state.value)
        }
    }

    fun cancel() {

        stopTimer()

        CoroutineScope(Dispatchers.IO).launch {

            _state.value = _state.value.copy(active = false)
            repository.save(_state.value)

            repository.next()

            val newCycle = repository.load()
            _state.value = newCycle

            _sessions.value = emptyList()

            createNextSession(_state.value.currentPhase)
        }
    }

    fun restart() {
        stopTimer()
        _state.value = _state.value.copy(
            remainingSeconds =
                if (_state.value.currentPhase == "focus")
                    _state.value.focusSeconds
                else
                    _state.value.restSeconds
        )
    }

    fun loadTags() {
        CoroutineScope(Dispatchers.IO).launch {
            _tags.value = repository.getAllTags()
        }
    }

    fun updateTag(tag: TagModel) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.upsertTag(tag)
            _tags.value = repository.getAllTags() // refresh after update
        }
    }

    fun selectTag(tag: TagModel) {
        _state.value = _state.value.copy(tag = tag.tag, color = tag.color)
        saveCycle()
    }

    fun skip() {
        _state.value = _state.value.copy(remainingSeconds = 0)
        phaseCheck()
    }

    suspend fun getColor(tag: String): Long? {
        return repository.getColor(tag)
    }
}