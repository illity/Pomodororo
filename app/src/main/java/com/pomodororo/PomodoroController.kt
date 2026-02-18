import androidx.compose.runtime.mutableStateOf
import com.pomodororo.model.PomodoroModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PomodoroController {

    var state = mutableStateOf(PomodoroModel())
        private set

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /** Toggle Play / Pause */
    fun togglePlayPause() {
        if (state.value.isRunning) pause() else play()
    }

    private fun play() {
        if (state.value.isRunning) return

        state.value = state.value.copy(isRunning = true)

        timerJob = scope.launch {
            while (state.value.isRunning) {
                delay(1000)
                tick()

                // check for zero and switch phase
                if (state.value.remainingSeconds <= 0) {
                    switchPhase()
                }
            }
        }
    }

    private fun pause() {
        timerJob?.cancel()
        state.value = state.value.copy(isRunning = false)
    }

    /** Cancel current session and reset remaining seconds */
    fun restart() {
        pause()
        val newRemaining = if (state.value.currentPhase == "focus")
            state.value.focusSeconds else state.value.restSeconds
        state.value = state.value.copy(remainingSeconds = newRemaining)
    }

    fun cancel() {
        pause()
        state.value = state.value.copy(
            remainingSeconds = state.value.focusSeconds,
            currentPhase = "focus",
            completedSessions = 0
        )
    }


    /** Decrease remaining seconds by 1 */
    private fun tick() {
        val current = state.value
        state.value = current.copy(
            remainingSeconds = (current.remainingSeconds - 1).coerceAtLeast(0)
        )
    }

    /** Switch between focus and rest phases */
    private fun switchPhase() {
        val current = state.value
        val nextPhase = if (current.currentPhase == "focus") "rest" else "focus"
        val nextRemaining = if (nextPhase == "focus") current.focusSeconds else current.restSeconds

        // optionally increment completedSessions only when finishing a focus session
        val completedSessions = if (current.currentPhase == "rest") current.completedSessions + 1 else current.completedSessions

        state.value = current.copy(
            isRunning = false,
            currentPhase = nextPhase,
            remainingSeconds = nextRemaining,
            completedSessions = completedSessions
        )
    }
}
