package com.pomodororo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_tag")
data class TagEntity(
    @PrimaryKey
    val tag: String = "study",

    val color: Long = 0xfff3644c,
)