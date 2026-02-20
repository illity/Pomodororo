// data/AppDatabase.kt
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PomodoroStatsEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): PomodoroStatsDao
}