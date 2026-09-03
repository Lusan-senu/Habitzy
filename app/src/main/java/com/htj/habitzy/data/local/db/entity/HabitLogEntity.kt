package com.htj.habitzy.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("habitId"), Index(value = ["habitId", "epochDay"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    val isCompleted: Boolean,
    val amountValue: Double? = null,
    val checklistDoneMask: Int = 0,
    val completedAtEpochMillis: Long? = null,
)
