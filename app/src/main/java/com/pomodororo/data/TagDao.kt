package com.pomodororo.data

import androidx.room.*

@Dao
interface TagDao {

    @Query("SELECT color FROM pomodoro_tag WHERE tag = :tag")
    suspend fun get(tag: String): Long?

    @Insert
    suspend fun insert(entity: TagEntity): Long

    @Upsert
    suspend fun upsert(value: TagEntity)
}