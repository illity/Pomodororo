import android.content.Context
import androidx.room.Room
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
            if (statsDao.getStats() == null) {
                statsDao.insert(PomodoroStatsEntity())
            }
        }
    }

    private val _state = MutableStateFlow(PomodoroModel())
    val state: StateFlow<PomodoroModel> = _state.asStateFlow()

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
    }

    private fun phaseCheck() {
        if (_state.value.completedSessions == _state.value.totalSessions) {
            cancel()
        }
        if (_state.value.remainingSeconds <= 0) {
            CoroutineScope(Dispatchers.IO).launch {
                statsDao.incrementPhaseSwitch()
                val stats = statsDao.getStats()
                println(stats?.phaseSwitchCount)

            }

            // switch phase
            if (_state.value.currentPhase == "rest") _state.value = _state.value.copy(
                completedSessions = _state.value.completedSessions + 1
            )
            val nextPhase = if (_state.value.currentPhase == "focus") "rest" else "focus"
            val nextSeconds = if (nextPhase == "focus") _state.value.focusSeconds else _state.value.restSeconds
            _state.value = _state.value.copy(currentPhase = nextPhase,
                remainingSeconds = nextSeconds,
                isRunning = false
            )
            job?.cancel()
        }
    }

    private fun stopTimer() {
        _state.value = _state.value.copy(isRunning = false)
        job?.cancel()
    }

    fun cancel() {
        _state.value = PomodoroModel()
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
