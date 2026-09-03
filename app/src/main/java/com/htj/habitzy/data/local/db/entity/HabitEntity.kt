package com.htj.habitzy.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val iconKey: String,
    val colorSeedArgb: Int,
    val type: String,
    val amountGoal: Double? = null,
    val amountUnit: String? = null,
    val checklistItemsJson: String? = null,
    val scheduleType: String,
    val scheduleWeekdaysMask: Int = 0,
    val scheduleTimesTarget: Int = 0,
    val scheduleEveryNDays: Int = 0,
    val categoryTag: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochDay: Long,
    val isArchived: Boolean = false,
    val vacationStartEpochDay: Long? = null,
    val vacationEndEpochDay: Long? = null,
)
