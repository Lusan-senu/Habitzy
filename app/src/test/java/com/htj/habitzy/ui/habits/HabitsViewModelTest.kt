package com.htj.habitzy.ui.habits

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.domain.usecase.ComputeStreakUseCase
import com.htj.habitzy.testing.FakeHabitRepository
import com.htj.habitzy.testing.FakeSettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var habitRepository: FakeHabitRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var viewModel: HabitsViewModel

    private val today: LocalDate = LocalDate.now()

    private fun habit(
        id: Long,
        name: String,
        schedule: HabitSchedule = HabitSchedule.Daily,
        categoryTag: String? = null,
        createdAt: LocalDate = today.minusDays(30),
    ) = Habit(
        id = id,
        name = name,
        description = null,
        icon = HabitIcon.Emoji("⭐"),
        color = 0xFF00A896.toInt(),
        type = HabitType.Binary,
        schedule = schedule,
        categoryTag = categoryTag,
        isArchived = false,
        vacationRange = null,
        sortOrder = id.toInt(),
        createdAtEpochDay = createdAt.toEpochDay(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = FakeHabitRepository()
        settingsRepository = FakeSettingsRepository()
        viewModel = HabitsViewModel(
            habitRepository = habitRepository,
            settingsRepository = settingsRepository,
            computeStreakUseCase = ComputeStreakUseCase(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filters habits by selected date schedule`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(1, "Daily habit"),
            habit(2, "Mondays", schedule = HabitSchedule.SpecificWeekdays(setOf(java.time.DayOfWeek.MONDAY))),
        )
        runCurrent()

        // Find the next Monday from today and select it.
        var date = today
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) date = date.plusDays(1)
        viewModel.onSelectDate(date)
        runCurrent()

        val state = viewModel.uiState.first { !it.isLoading }
        assertThat(state.habits.map { it.habit.name }).containsExactly("Daily habit", "Mondays")

        // Then a Sunday: only the daily habit remains.
        var sunday = date
        while (sunday.dayOfWeek != java.time.DayOfWeek.SUNDAY) sunday = sunday.plusDays(1)
        viewModel.onSelectDate(sunday)
        runCurrent()

        val sundayState = viewModel.uiState.first { !it.isLoading && it.selectedDate == sunday }
        assertThat(sundayState.habits.map { it.habit.name }).containsExactly("Daily habit")
    }
    @Test
    fun `times-per-week habit appears every day regardless of weekday`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(1, "3x week", schedule = HabitSchedule.TimesPerWeek(3)),
        )
        runCurrent()

        val state = viewModel.uiState.first { !it.isLoading }
        assertThat(state.habits).hasSize(1)
    }

    @Test
    fun `vacation range hides habit on vacation days`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(1, "On holiday").let { it.copy(vacationRange = today..today.plusDays(5)) },
        )
        runCurrent()

        val state = viewModel.uiState.first { !it.isLoading }
        assertThat(state.habits).isEmpty()
        assertThat(state.hasAnyHabits).isTrue()
    }

    @Test
    fun `category filter narrows visible habits`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(1, "Meditate", categoryTag = "mind"),
            habit(2, "Run", categoryTag = "health"),
        )
        runCurrent()

        viewModel.onToggleCategoryFilter("mind")
        runCurrent()

        val state = viewModel.uiState.first { !it.isLoading && it.activeCategoryFilters.isNotEmpty() }
        assertThat(state.habits.map { it.habit.name }).containsExactly("Meditate")
    }

    @Test
    fun `sort by name orders habits alphabetically`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(1, "Zinc"),
            habit(2, "Alpha"),
            habit(3, "Middle"),
        )
        runCurrent()

        viewModel.onSelectSortMode(SortMode.NAME)
        runCurrent()

        val state = viewModel.uiState.first { !it.isLoading && it.sortMode == SortMode.NAME }
        assertThat(state.habits.map { it.habit.name })
            .containsExactly("Alpha", "Middle", "Zinc").inOrder()
    }

    @Test
    fun `toggle completion sends undo snackbar message`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(habit(1, "Run"))
        runCurrent()

        viewModel.onToggleCompletion(1, today)
        runCurrent()

        assertThat(habitRepository.logs.value.first().isCompleted).isTrue()

        val message = viewModel.snackbarMessages.first()
        assertThat(message.actionLabel).isEqualTo("Undo")
    }

    @Test
    fun `undo restores previous completion state`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(habit(1, "Run"))
        runCurrent()

        viewModel.onToggleCompletion(1, today)
        runCurrent()
        viewModel.onUndoToggle(1, today)
        runCurrent()

        assertThat(habitRepository.logs.value.first().isCompleted).isFalse()
    }

    @Test
    fun `all habits completed today fires celebration once per day`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(habit(1, "Run"))
        runCurrent()

        // uiState uses WhileSubscribed — collect it like the screen does, plus the
        // conflated celebration channel, before toggling.
        val uiStates = mutableListOf<HabitsUiState>()
        val uiCollector = launch(testDispatcher) { viewModel.uiState.collect { uiStates.add(it) } }
        val celebrated = mutableListOf<Boolean>()
        val celebrationCollector = launch(testDispatcher) { viewModel.celebrationEvents.collect { celebrated.add(it) } }
        runCurrent()

        viewModel.onToggleCompletion(1, today)
        runCurrent()
        advanceUntilIdle()

        assertThat(celebrated).containsExactly(true)
        assertThat(settingsRepository.prefs.value["celebrated"]).isEqualTo(today.toEpochDay())

        // Toggle off and back on the same day: the persisted guard must prevent a re-fire.
        viewModel.onToggleCompletion(1, today)
        runCurrent()
        viewModel.onToggleCompletion(1, today)
        runCurrent()
        advanceUntilIdle()

        assertThat(celebrated).hasSize(1)

        uiCollector.cancel()
        celebrationCollector.cancel()
    }

    @Test
    fun `reorder persists new sort order`() = runTest(testDispatcher) {
        habitRepository.habits.value = listOf(
            habit(3, "Third"),
            habit(1, "First"),
            habit(2, "Second"),
        )
        runCurrent()

        viewModel.onReorder(listOf(2L, 3L, 1L))
        runCurrent()

        val byId = habitRepository.habits.value.associateBy { it.id }
        assertThat(byId.getValue(2L).sortOrder).isEqualTo(0)
        assertThat(byId.getValue(3L).sortOrder).isEqualTo(1)
        assertThat(byId.getValue(1L).sortOrder).isEqualTo(2)
    }
}
