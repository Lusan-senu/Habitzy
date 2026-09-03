package com.htj.habitzy.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_notes",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("habitId")],
)
data class HabitNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    val text: String,
    val photoUri: String? = null,
)
