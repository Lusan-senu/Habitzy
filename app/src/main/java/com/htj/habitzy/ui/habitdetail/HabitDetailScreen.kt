package com.htj.habitzy.ui.habitdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.ui.components.ConfirmDialog
import com.htj.habitzy.ui.components.HabitCompletionControl
import com.htj.habitzy.ui.components.HabitIconChip
import com.htj.habitzy.ui.components.HabitzySnackbarHost
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.components.StreakBadge
import com.htj.habitzy.ui.components.rememberHabitzySnackbarHostState
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXL
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberHabitzySnackbarHostState()
    val habit = uiState.habit

    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showVacationPicker by remember { mutableStateOf(false) }
    var editingNoteDate by remember { mutableStateOf<LocalDate?>(null) }
    var pendingToggleDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (message.action) {
                    is DetailSnackbarMessage.Archive -> viewModel.onArchiveUndo()
                    is DetailSnackbarMessage.Unarchive -> viewModel.onUnarchiveUndo()
                    null -> Unit
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { HabitzySnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = habit?.name ?: "Habit", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (habit != null) {
                        Box(modifier = Modifier) {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                DropdownMenuItem(
                                    text = { Text(text = "Edit") },
                                    onClick = {
                                        showOverflow = false
                                        onEdit(habitId)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = if (habit.isArchived) "Unarchive" else "Archive") },
                                    onClick = {
                                        showOverflow = false
                                        if (habit.isArchived) viewModel.onUnarchive() else viewModel.onArchive()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = if (habit.vacationRange != null) "Edit vacation mode" else "Vacation mode")
                                    },
                                    onClick = {
                                        showOverflow = false
                                        showVacationPicker = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "Delete permanently") },
                                    onClick = {
                                        showOverflow = false
                                        showDeleteConfirm = true
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpaceL, vertical = SpaceS),
            verticalArrangement = Arrangement.spacedBy(SpaceXL),
        ) {
            if (habit == null && uiState.isLoading) {
                Text(text = "Loading…", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else if (habit != null) {
                if (habit.vacationRange != null) {
                    VacationBanner(
                        range = habit.vacationRange,
                        onClickEdit = { showVacationPicker = true },
                    )
                }

                HabitHeader(habit = habit, streakInfo = uiState.streakInfo)

                TodayActionRow(
                    habit = habit,
                    isCompletedToday = uiState.completionLogs[LocalDate.now()]?.isCompleted ?: false,
                    amountProgress = uiState.completionLogs[LocalDate.now()]?.amountValue ?: 0.0,
                    checklistDoneCount = todayChecklistDone(habit, uiState),
                    onToggle = { viewModel.onToggleDay(LocalDate.now()) },
                    onAmountStep = { delta ->
                        val current = uiState.completionLogs[LocalDate.now()]?.amountValue ?: 0.0
                        viewModel.onAmountStep(LocalDate.now(), current, delta)
                    },
                )

                RangeSelector(
                    selected = uiState.range,
                    onSelect = viewModel::onSelectRange,
                )

                val completedMap = remember(uiState.completionLogs) {
                    uiState.completionLogs.mapValues { it.value.isCompleted }
                }
                DetailRangeGrid(
                    range = uiState.range,
                    logs = completedMap,
                    vacationRange = habit.vacationRange,
                    weekStartDay = java.time.DayOfWeek.MONDAY,
                    onDayClick = { date ->
                        val inConfirmationWindow = date.isBefore(LocalDate.now().minusDays(30))
                        if (inConfirmationWindow) pendingToggleDate = date else viewModel.onToggleDay(date)
                    },
                    onDayLongPress = { editingNoteDate = it },
                )

                SectionHeader(title = "Stats")
                HabitStatsGrid(
                    streakInfo = uiState.streakInfo,
                    ratePeriod = uiState.ratePeriod,
                    onSelectRatePeriod = viewModel::onSelectRatePeriod,
                )

                SectionHeader(title = "Momentum")
                StreakOverTimeChart(weeklyCounts = uiState.weeklyCompletionCounts)

                if (uiState.notes.isNotEmpty()) {
                    SectionHeader(title = "Notes")
                    uiState.notes.forEach { note ->
                        NoteRow(
                            date = note.date,
                            text = note.text,
                            photoUri = note.photoUri,
                            onClick = { editingNoteDate = note.date },
                        )
                    }
                } else {
                    SectionHeader(title = "Notes")
                    Text(
                        text = "Long-press a day in the calendar to add a note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && habit != null) {
        ConfirmDialog(
            title = "Delete habit?",
            message = "This will permanently delete \"${habit.name}\" and all of its history. This cannot be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.onDeletePermanently()
                onBack()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (pendingToggleDate != null) {
        val date = pendingToggleDate!!
        ConfirmDialog(
            title = "Edit this past day?",
            message = "This is more than 30 days ago. Confirm you want to change its completion status.",
            confirmLabel = "Change",
            onConfirm = {
                viewModel.onToggleDay(date)
                pendingToggleDate = null
            },
            onDismiss = { pendingToggleDate = null },
        )
    }

    if (showVacationPicker && habit != null) {
        VacationPickerDialog(
            initialRange = habit.vacationRange,
            onApply = { start, end -> viewModel.onSetVacation(start, end) },
            onClear = { viewModel.onClearVacation() },
            onDismiss = { showVacationPicker = false },
        )
    }

    editingNoteDate?.let { date ->
        val existing = uiState.notes.firstOrNull { it.date == date }
        DayNoteEditor(
            date = date,
            initialText = existing?.text ?: "",
            initialPhotoUri = existing?.photoUri,
            onSave = { text, photoUri ->
                viewModel.onSaveDayNote(date, text, photoUri)
            },
            onDismiss = { editingNoteDate = null },
        )
    }
}

@Composable
private fun HabitHeader(
    habit: com.htj.habitzy.domain.model.Habit,
    streakInfo: com.htj.habitzy.domain.model.StreakInfo,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitIconChip(
                icon = habit.icon,
                color = Color(habit.color),
                size = 64,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = habit.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = habit.schedule.summaryText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
            StreakBadge(streak = streakInfo.currentStreak)
            if (streakInfo.bestStreak > streakInfo.currentStreak) {
                StreakBadge(streak = streakInfo.bestStreak)
            }
        }
    }
}

@Composable
private fun TodayActionRow(
    habit: com.htj.habitzy.domain.model.Habit,
    isCompletedToday: Boolean,
    amountProgress: Double,
    checklistDoneCount: Int,
    onToggle: () -> Unit,
    onAmountStep: (Double) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = com.htj.habitzy.ui.theme.HabitzyShapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(SpaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Today", style = MaterialTheme.typography.titleMedium)
                when (habit.type) {
                    is HabitType.Binary -> {
                        Text(
                            text = if (isCompletedToday) "Completed" else "Not yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is HabitType.Amount -> {
                        val goal = habit.type.goal
                        Text(
                            text = "%.1f / %.1f ${habit.type.unit}".trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is HabitType.Checklist -> {
                        Text(
                            text = "$checklistDoneCount / ${habit.type.steps.size} steps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when (habit.type) {
                is HabitType.Binary -> HabitCompletionControl(
                    habit = habit,
                    isCompleted = isCompletedToday,
                    onToggle = onToggle,
                )
                is HabitType.Amount -> {
                    Row {
                        IconButton(onClick = { onAmountStep(-1.0) }) { Text(text = "−", style = MaterialTheme.typography.titleLarge) }
                        HabitCompletionControl(
                            habit = habit,
                            isCompleted = isCompletedToday,
                            amountProgress = amountProgress,
                            onToggle = onToggle,
                        )
                        IconButton(onClick = { onAmountStep(1.0) }) { Text(text = "+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
                is HabitType.Checklist -> HabitCompletionControl(
                    habit = habit,
                    isCompleted = isCompletedToday,
                    checklistDoneCount = checklistDoneCount,
                    onToggle = onToggle,
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selected: DetailRange,
    onSelect: (DetailRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        DetailRange.values().forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, DetailRange.values().size),
            ) {
                Text(text = range.name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun VacationBanner(
    range: ClosedRange<LocalDate>,
    onClickEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = com.htj.habitzy.ui.theme.HabitzyShapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(SpaceM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Paused until ${range.endInclusive}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            androidx.compose.material3.TextButton(onClick = onClickEdit) {
                Text(text = "Edit")
            }
        }
    }
}

private fun todayChecklistDone(
    habit: com.htj.habitzy.domain.model.Habit,
    uiState: HabitDetailUiState,
): Int {
    if (habit.type !is HabitType.Checklist) return 0
    val log = uiState.completionLogs[LocalDate.now()]
    return log?.let { Integer.bitCount(it.checklistDoneMask) } ?: 0
}
