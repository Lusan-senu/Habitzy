package com.htj.habitzy.ui.addedithabit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.htj.habitzy.data.local.db.HabitzyDatabase
import com.htj.habitzy.notifications.NotificationScheduler
import com.htj.habitzy.testing.FakeHabitRepository
import com.htj.habitzy.testing.FakeSettingsRepository
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs on Robolectric so the real NotificationScheduler has an Android context. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHabitViewModelValidationTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(habitId: Long? = null): AddEditHabitViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context,
            HabitzyDatabase::class.java,
        ).allowMainThreadQueries().build()
        val scheduler = NotificationScheduler(
            context,
            db.habitDao(),
            db.reminderDao(),
        )
        return AddEditHabitViewModel(
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habitId)),
            habitRepository = FakeHabitRepository(),
            notificationScheduler = scheduler,
            settingsRepository = FakeSettingsRepository(),
        )
    }

    @Test
    fun `binary daily habit with blank name cannot save`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("   ")
        assertThat(vm.uiState.value.canSave).isFalse()
    }

    @Test
    fun `binary daily habit with name can save`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("Meditate")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun `amount habit requires positive goal`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("Drink water")
        vm.onTypeChange(FormHabitType.AMOUNT)
        assertThat(vm.uiState.value.canSave).isFalse() // empty goal

        vm.onAmountGoalChange("0")
        assertThat(vm.uiState.value.canSave).isFalse() // zero goal

        vm.onAmountGoalChange("8")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun `checklist habit requires at least two non-blank steps`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("Evening routine")
        vm.onTypeChange(FormHabitType.CHECKLIST)
        assertThat(vm.uiState.value.canSave).isFalse() // two blank rows

        vm.onChecklistStepChange(0, "Floss")
        assertThat(vm.uiState.value.canSave).isFalse() // only one non-blank

        vm.onChecklistStepChange(1, "Stretch")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun `specific weekdays schedule requires at least one selected day`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("Gym")
        vm.onScheduleKindChange(ScheduleKind.SPECIFIC_WEEKDAYS)
        assertThat(vm.uiState.value.canSave).isFalse()

        vm.toggleWeekday(java.time.DayOfWeek.MONDAY.value)
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun `times per week schedule requires positive target`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.onNameChange("Stretch")
        vm.onScheduleKindChange(ScheduleKind.TIMES_PER_WEEK)
        vm.onTimesPerWeekChange("0")
        assertThat(vm.uiState.value.canSave).isFalse()

        vm.onTimesPerWeekChange("3")
        assertThat(vm.uiState.value.canSave).isTrue()
    }

    @Test
    fun `checklist steps cannot exceed eight`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        repeat(10) { vm.addChecklistStep() }
        assertThat(vm.uiState.value.checklistSteps.size).isEqualTo(8)
    }

    @Test
    fun `checklist steps cannot go below two`() = runTest(testDispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.removeChecklistStep(0)
        vm.removeChecklistStep(0)
        assertThat(vm.uiState.value.checklistSteps.size).isEqualTo(2)
    }
}
