package com.pomodororo.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PomodoroCycleEntity::class,
                PomodoroSessionEntity::class,
                TagEntity::class],
    version = 1
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): PomodoroCycleDao
    abstract fun sessionDao(): PomodoroSessionDao
    abstract fun tagDao(): TagDao
}