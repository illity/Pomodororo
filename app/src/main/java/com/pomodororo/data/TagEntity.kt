package com.pomodororo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_tag")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,        // unique identifier
    val tag: String,         // name can now be changed freely
    val color: Long = 0xfff3644c
)