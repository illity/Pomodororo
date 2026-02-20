// data/PomodoroStatsEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_stats")
data class PomodoroStatsEntity(
    @PrimaryKey val id: Int = 0, // always 0 (single row)
    val phaseSwitchCount: Int = 0
)