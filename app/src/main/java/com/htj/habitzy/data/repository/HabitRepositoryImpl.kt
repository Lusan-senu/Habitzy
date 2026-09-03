package com.htj.habitzy.data.repository

import com.htj.habitzy.data.local.db.dao.HabitDao
import com.htj.habitzy.data.local.db.dao.HabitLogDao
import com.htj.habitzy.data.local.db.dao.HabitNoteDao
import com.htj.habitzy.data.local.db.dao.ReminderDao
import com.htj.habitzy.data.local.db.entity.HabitEntity
import com.htj.habitzy.data.local.db.entity.HabitLogEntity
import com.htj.habitzy.data.local.db.entity.HabitNoteEntity
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitLog
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.domain.model.DayNote
import com.htj.habitzy.domain.model.Reminder
import com.htj.habitzy.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val noteDao: HabitNoteDao,
    private val reminderDao: ReminderDao,
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> = habitDao.observeActiveHabits().map { it.map(::toHabit) }

    override fun observeArchivedHabits(): Flow<List<Habit>> = habitDao.observeArchivedHabits().map { it.map(::toHabit) }

    override fun observeHabit(id: Long): Flow<Habit?> = habitDao.observeHabit(id).map { it?.let(::toHabit) }

    override suspend fun getUsedCategoryTags(): List<String> = habitDao.getUsedCategoryTags()

    override suspend fun getNextSortOrder(): Int = habitDao.getNextSortOrder()

    override suspend fun createHabit(habit: Habit): Long = habitDao.insert(toEntity(habit))

    override suspend fun updateHabit(habit: Habit) = habitDao.update(toEntity(habit))

    override suspend fun archiveHabit(id: Long, archived: Boolean) = habitDao.setArchived(id, archived)

    override suspend fun deleteHabitPermanently(id: Long) {
        habitDao.getHabit(id)?.let { habitDao.delete(it) }
    }

    override suspend fun reorderHabits(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> habitDao.updateSortOrder(id, index) }
    }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate) {
        val current = logDao.getLog(habitId, date.toEpochDay())
        val habit = habitDao.getHabit(habitId) ?: return
        if (current == null) {
            val completed = isFullyCompletedBuildingFromScratch(habit)
            logDao.upsert(
                HabitLogEntity(
                    habitId = habitId,
                    epochDay = date.toEpochDay(),
                    isCompleted = completed,
                    amountValue = initialAmount(habit),
                    completedAtEpochMillis = if (completed) System.currentTimeMillis() else null,
                )
            )
        } else {
            val newCompleted = !current.isCompleted
            logDao.upsert(
                current.copy(
                    isCompleted = newCompleted,
                    amountValue = current.amountValue ?: initialAmount(habit),
                    completedAtEpochMillis = if (newCompleted) System.currentTimeMillis() else null,
                )
            )
        }
    }

    override suspend fun setAmountProgress(habitId: Long, date: LocalDate, value: Double) {
        val habit = habitDao.getHabit(habitId) ?: return
        val goal = (habit.amountGoal ?: 0.0)
        val completed = goal > 0 && value >= goal
        val current = logDao.getLog(habitId, date.toEpochDay())
        if (current == null) {
            logDao.upsert(
                HabitLogEntity(
                    habitId = habitId,
                    epochDay = date.toEpochDay(),
                    isCompleted = completed,
                    amountValue = value,
                    completedAtEpochMillis = if (completed) System.currentTimeMillis() else null,
                )
            )
        } else {
            logDao.upsert(
                current.copy(
                    isCompleted = completed,
                    amountValue = value,
                    completedAtEpochMillis = if (completed) (current.completedAtEpochMillis ?: System.currentTimeMillis()) else if (value >= goal) current.completedAtEpochMillis else null,
                )
            )
        }
    }

    override suspend fun setChecklistStep(habitId: Long, date: LocalDate, stepIndex: Int, done: Boolean) {
        val current = logDao.getLog(habitId, date.toEpochDay())
        val mask = (current?.checklistDoneMask ?: 0)
        val newMask = if (done) mask or (1 shl stepIndex) else mask and (1 shl stepIndex).inv()
        val habit = habitDao.getHabit(habitId)
        val totalSteps = habit?.checklistCount() ?: 0
        val completed = totalSteps > 0 && countBits(newMask) >= totalSteps
        if (current == null) {
            logDao.upsert(
                HabitLogEntity(
                    habitId = habitId,
                    epochDay = date.toEpochDay(),
                    isCompleted = completed,
                    checklistDoneMask = newMask,
                    completedAtEpochMillis = if (completed) System.currentTimeMillis() else null,
                )
            )
        } else {
            logDao.upsert(
                current.copy(
                    isCompleted = completed,
                    checklistDoneMask = newMask,
                    completedAtEpochMillis = if (completed) (current.completedAtEpochMillis ?: System.currentTimeMillis()) else if (completed) current.completedAtEpochMillis else null,
                )
            )
        }
    }

    override fun observeLogs(habitId: Long, range: ClosedRange<LocalDate>): Flow<List<HabitLog>> =
        logDao.observeLogsInRange(habitId, range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { list -> list.map(::toLog) }

    override fun observeAllLogsInRange(range: ClosedRange<LocalDate>): Flow<List<HabitLog>> =
        logDao.observeAllLogsInRange(range.start.toEpochDay(), range.endInclusive.toEpochDay())
            .map { list -> list.map(::toLog) }

    override fun observeNotes(habitId: Long): Flow<List<DayNote>> =
        noteDao.observeNotes(habitId).map { list -> list.map { toDayNote(it) } }

    override fun observeReminders(habitId: Long): Flow<List<Reminder>> =
        reminderDao.observeReminders(habitId).map { list -> list.map(::toReminder) }

    override suspend fun saveReminders(habitId: Long, reminders: List<Reminder>) {
        reminderDao.deleteForHabit(habitId)
        reminders.forEach { reminder ->
            reminderDao.upsert(
                com.htj.habitzy.data.local.db.entity.ReminderEntity(
                    habitId = habitId,
                    hour = reminder.hour,
                    minute = reminder.minute,
                    message = reminder.message,
                    isEnabled = reminder.isEnabled,
                )
            )
        }
    }

    override suspend fun setDayNote(habitId: Long, date: LocalDate, text: String, photoUri: String?) {
        val existing = noteDao.getNote(habitId, date.toEpochDay())
        if (existing != null) {
            noteDao.deleteNote(habitId, date.toEpochDay())
        }
        if (text.isNotBlank() || photoUri != null) {
            noteDao.insert(
                HabitNoteEntity(habitId = habitId, epochDay = date.toEpochDay(), text = text, photoUri = photoUri)
            )
        }
    }

    // --- Mapping helpers ---

    private fun toHabit(e: HabitEntity): Habit = Habit(
        id = e.id,
        name = e.name,
        description = e.description,
        icon = HabitIcon.fromKey(e.iconKey),
        color = e.colorSeedArgb,
        type = e.toHabitType(),
        schedule = e.toSchedule(),
        categoryTag = e.categoryTag,
        isArchived = e.isArchived,
        vacationRange = vacationRange(e),
        sortOrder = e.sortOrder,
        createdAtEpochDay = e.createdAtEpochDay,
    )

    private fun toEntity(h: Habit): HabitEntity = HabitEntity(
        id = h.id,
        name = h.name,
        description = h.description,
        iconKey = HabitIcon.toKey(h.icon),
        colorSeedArgb = h.color,
        type = when (h.type) {
            is HabitType.Binary -> "BINARY"
            is HabitType.Amount -> "AMOUNT"
            is HabitType.Checklist -> "CHECKLIST"
        },
        amountGoal = (h.type as? HabitType.Amount)?.goal,
        amountUnit = (h.type as? HabitType.Amount)?.unit,
        checklistItemsJson = (h.type as? HabitType.Checklist)?.steps?.let(::encodeList),
        scheduleType = when (h.schedule) {
            is HabitSchedule.Daily -> "DAILY"
            is HabitSchedule.SpecificWeekdays -> "SPECIFIC_WEEKDAYS"
            is HabitSchedule.TimesPerWeek -> "TIMES_PER_WEEK"
            is HabitSchedule.TimesPerMonth -> "TIMES_PER_MONTH"
            is HabitSchedule.EveryNDays -> "EVERY_N_DAYS"
        },
        scheduleWeekdaysMask = (h.schedule as? HabitSchedule.SpecificWeekdays)
            ?.weekdays?.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) } ?: 0,
        scheduleTimesTarget = when (h.schedule) {
            is HabitSchedule.TimesPerWeek -> h.schedule.target
            is HabitSchedule.TimesPerMonth -> h.schedule.target
            else -> 0
        },
        scheduleEveryNDays = (h.schedule as? HabitSchedule.EveryNDays)?.n ?: 0,
        categoryTag = h.categoryTag,
        sortOrder = h.sortOrder,
        createdAtEpochDay = h.createdAtEpochDay,
        isArchived = h.isArchived,
        vacationStartEpochDay = h.vacationRange?.start?.toEpochDay(),
        vacationEndEpochDay = h.vacationRange?.endInclusive?.toEpochDay(),
    )

    private fun HabitEntity.toHabitType(): HabitType = when (type) {
        "AMOUNT" -> HabitType.Amount(amountGoal ?: 0.0, amountUnit ?: "")
        "CHECKLIST" -> HabitType.Checklist(decodeList(checklistItemsJson))
        else -> HabitType.Binary
    }

    private fun HabitEntity.toSchedule(): HabitSchedule = when (scheduleType) {
        "SPECIFIC_WEEKDAYS" -> {
            val days = (0 until 7).mapNotNull { i ->
                if (scheduleWeekdaysMask and (1 shl i) != 0) DayOfWeek.of(i + 1) else null
            }.toSet()
            HabitSchedule.SpecificWeekdays(days)
        }
        "TIMES_PER_WEEK" -> HabitSchedule.TimesPerWeek(scheduleTimesTarget)
        "TIMES_PER_MONTH" -> HabitSchedule.TimesPerMonth(scheduleTimesTarget)
        "EVERY_N_DAYS" -> HabitSchedule.EveryNDays(scheduleEveryNDays)
        else -> HabitSchedule.Daily
    }

    private fun HabitEntity.checklistCount(): Int = decodeList(checklistItemsJson).size

    private fun vacationRange(e: HabitEntity): ClosedRange<LocalDate>? =
        if (e.vacationStartEpochDay != null && e.vacationEndEpochDay != null) {
            LocalDate.ofEpochDay(e.vacationStartEpochDay)..LocalDate.ofEpochDay(e.vacationEndEpochDay)
        } else null

    private fun toLog(e: HabitLogEntity): HabitLog = HabitLog(
        habitId = e.habitId,
        date = LocalDate.ofEpochDay(e.epochDay),
        isCompleted = e.isCompleted,
        amountValue = e.amountValue,
        checklistDoneMask = e.checklistDoneMask,
        completedAt = e.completedAtEpochMillis?.let(Instant::ofEpochMilli),
    )

    private fun toDayNote(e: com.htj.habitzy.data.local.db.entity.HabitNoteEntity): DayNote = DayNote(
        id = e.id,
        habitId = e.habitId,
        date = LocalDate.ofEpochDay(e.epochDay),
        text = e.text,
        photoUri = e.photoUri,
    )

    private fun toReminder(e: com.htj.habitzy.data.local.db.entity.ReminderEntity): Reminder = Reminder(
        id = e.id,
        habitId = e.habitId,
        hour = e.hour,
        minute = e.minute,
        message = e.message,
        isEnabled = e.isEnabled,
    )

    private fun isFullyCompletedBuildingFromScratch(entity: HabitEntity): Boolean {
        // Toggle on a fresh day: for Binary it's simply done.
        return true
    }

    private fun initialAmount(entity: HabitEntity): Double? =
        if (entity.type == "AMOUNT") 0.0 else null

    private fun countBits(mask: Int): Int = Integer.bitCount(mask)

    private fun encodeList(list: List<String>): String =
        list.joinToString("\u0001")

    private fun decodeList(json: String?): List<String> =
        json?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()
}
