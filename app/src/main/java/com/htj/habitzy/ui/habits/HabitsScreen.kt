package com.htj.habitzy.ui.habits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htj.habitzy.R
import com.htj.habitzy.ui.components.EmptyState
import com.htj.habitzy.ui.components.HabitCard
import com.htj.habitzy.ui.components.HabitWithTodayLog
import com.htj.habitzy.ui.components.HabitzySnackbarHost
import com.htj.habitzy.ui.components.HabitzyTopActionPill
import com.htj.habitzy.ui.components.SectionHeader
import com.htj.habitzy.ui.components.rememberHabitzySnackbarHostState
import com.htj.habitzy.ui.navigation.FloatingNavClusterContentClearance
import com.htj.habitzy.ui.theme.ShapeFull
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val BottomPadding = FloatingNavClusterContentClearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddEdit: (Long?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberHabitzySnackbarHostState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topBarState)

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.action?.let {
                    when (it) {
                        is HabitsSnackbarAction.UndoToggle ->
                            viewModel.onUndoToggle(it.habitId, it.date)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.celebrationEvents.collect {
            showCelebration = true
            delay(1500)
            showCelebration = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { HabitzySnackbarHost(snackbarHostState) },
        topBar = {
            HabitsTopBar(
                scrollBehavior = scrollBehavior,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings,
                onFilterClick = { showFilterSheet = true },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HabitsContent(
                uiState = uiState,
                onSelectDate = viewModel::onSelectDate,
                onToggleCompletion = viewModel::onToggleCompletion,
                onAmountStep = viewModel::onAmountStep,
                onChecklistStep = viewModel::onChecklistStep,
                onBodyClick = onNavigateToDetail,
                onEdit = onNavigateToAddEdit,
                onArchive = viewModel::onArchive,
                onReorder = viewModel::onReorder,
                onNavigateToAddEdit = onNavigateToAddEdit,
                reorderMode = reorderMode,
                onToggleReorderMode = { reorderMode = !reorderMode },
            )

            CelebrationConfetti(visible = showCelebration)
        }
    }

    if (showFilterSheet) {
        FilterAndSortSheet(
            showArchived = uiState.showArchived,
            onToggleShowArchived = viewModel::onToggleShowArchived,
            availableTags = uiState.availableCategoryTags,
            activeFilters = uiState.activeCategoryFilters,
            onToggleFilter = viewModel::onToggleCategoryFilter,
            sortMode = uiState.sortMode,
            onSelectSortMode = viewModel::onSelectSortMode,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitsTopBar(
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LargeTopAppBar(
        title = { Text(text = "Habits", style = MaterialTheme.typography.headlineMedium) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(),
        navigationIcon = {},
        actions = {
            HabitzyTopActionPill(
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
                filterSortAvailable = true,
                onFilterSortClick = onFilterClick,
            )
        },
    )
}

@Composable
private fun HabitsContent(
    uiState: HabitsUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCompletion: (Long, LocalDate) -> Unit,
    onAmountStep: (Long, LocalDate, Double, Double) -> Unit,
    onChecklistStep: (Long, LocalDate, Int, Boolean) -> Unit,
    onBodyClick: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onNavigateToAddEdit: (Long?) -> Unit,
    reorderMode: Boolean,
    onToggleReorderMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DateStrip(
            weekDates = uiState.weekDates,
            selectedDate = uiState.selectedDate,
            onSelectDate = onSelectDate,
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize())
            }

            !uiState.hasAnyHabits -> {
                EmptyState(
                    icon = Icons.Filled.Check,
                    title = "Create your first habit",
                    subtitle = "Start building better routines, one habit at a time.",
                    ctaText = "Create a habit",
                    onCtaClick = { onNavigateToAddEdit(null) },
                )
            }

            uiState.habits.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Check,
                    title = "Nothing scheduled",
                    subtitle = "No habits are scheduled for this day.",
                )
            }

            else -> {
                val reorderAllowed =
                    uiState.sortMode == SortMode.MANUAL && !uiState.showArchived
                if (reorderMode && reorderAllowed) {
                    ReorderableHabitList(
                        items = uiState.habits,
                        onReorder = onReorder,
                        onExitReorder = onToggleReorderMode,
                    )
                } else {
                    GroupedHabitList(
                        grouped = uiState.groupedHabits,
                        onToggleCompletion = onToggleCompletion,
                        onAmountStep = onAmountStep,
                        onChecklistStep = onChecklistStep,
                        onBodyClick = onBodyClick,
                        onEdit = onEdit,
                        onArchive = onArchive,
                        selectedDate = uiState.selectedDate,
                        reorderAllowed = reorderAllowed,
                        onToggleReorderMode = onToggleReorderMode,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateStrip(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpaceL, vertical = SpaceS)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(SpaceS),
    ) {
        weekDates.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val background = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            val content = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Column(
                modifier = Modifier
                    .clip(ShapeFull)
                    .background(background)
                    .combinedClickable(onClick = { onSelectDate(date) })
                    .padding(horizontal = SpaceM, vertical = SpaceS),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = content,
                )
                Text(
                    text = if (isToday) "Today" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun GroupedHabitList(
    grouped: List<HabitGroup>,
    selectedDate: LocalDate,
    onToggleCompletion: (Long, LocalDate) -> Unit,
    onAmountStep: (Long, LocalDate, Double, Double) -> Unit,
    onChecklistStep: (Long, LocalDate, Int, Boolean) -> Unit,
    onBodyClick: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    reorderAllowed: Boolean,
    onToggleReorderMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hasTags = grouped.any { it.categoryTag != null }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SpaceL,
            end = SpaceL,
            top = SpaceS,
            bottom = BottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(SpaceM),
    ) {
        grouped.forEach { group ->
            if (hasTags) {
                item(key = "header_${group.categoryTag ?: "ungrouped"}") {
                    SectionHeader(title = group.categoryTag ?: "Habits")
                }
            }
            items(group.habits, key = { "habit_${it.habit.id}" }) { habitWithLog ->
                val habitId = habitWithLog.habit.id
                HabitCard(
                    habitWithLog = habitWithLog,
                    onBodyClick = {
                        if (reorderAllowed) onToggleReorderMode() else onBodyClick(habitId)
                    },
                    onToggle = { onToggleCompletion(habitId, selectedDate) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReorderableHabitList(
    items: List<HabitWithTodayLog>,
    onReorder: (List<Long>) -> Unit,
    onExitReorder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val list = items.toMutableList()
        val moved = list.removeAt(from.index)
        list.add(to.index, moved)
        onReorder(list.map { it.habit.id })
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SpaceL,
            end = SpaceL,
            top = SpaceS,
            bottom = BottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(SpaceM),
    ) {
        items(items, key = { "habit_${it.habit.id}" }) { habitWithLog ->
            ReorderableItem(reorderableState, key = "habit_${habitWithLog.habit.id}") { isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitCard(
                        habitWithLog = habitWithLog,
                        onBodyClick = onExitReorder,
                        onToggle = {},
                        modifier = Modifier
                            .weight(1f)
                            .scale(if (isDragging) 1.02f else 1f),
                    )
                    IconButton(onClick = onExitReorder) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_drag_handle),
                            contentDescription = "Drag to reorder",
                            modifier = Modifier
                                .size(24.dp)
                                .draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}
