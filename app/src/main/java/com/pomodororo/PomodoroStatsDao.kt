// data/PomodoroStatsDao.kt
import androidx.room.*

@Dao
interface PomodoroStatsDao {

    @Query("SELECT * FROM pomodoro_stats WHERE id = 0")
    suspend fun getStats(): PomodoroStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: PomodoroStatsEntity)

    @Query("UPDATE pomodoro_stats SET phaseSwitchCount = phaseSwitchCount + 1 WHERE id = 0")
    suspend fun incrementPhaseSwitch()
}