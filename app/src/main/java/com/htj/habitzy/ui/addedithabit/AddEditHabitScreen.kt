package com.htj.habitzy.ui.addedithabit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.ui.components.HabitIconChip
import com.htj.habitzy.ui.components.HabitzySnackbarHost
import com.htj.habitzy.ui.components.rememberHabitzySnackbarHostState
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val UNIT_SUGGESTIONS = listOf("steps", "glasses", "minutes", "pages", "km")

private val WEEKDAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitScreen(
    habitId: Long?,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditHabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberHabitzySnackbarHostState()

    var showEmojiPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var editingReminderIndex by remember { mutableIntStateOf(-1) }

    val nameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (habitId == null) nameFocusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { isEdit ->
            snackbarHostState.showSnackbar(
                message = if (isEdit) "Habit updated" else "Habit created",
                duration = SnackbarDuration.Short,
            )
            delay(200)
            onDone()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { HabitzySnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if (habitId == null) "New habit" else "Edit habit") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(SpaceL),
                    horizontalArrangement = Arrangement.spacedBy(SpaceM, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text(text = "Cancel", textAlign = TextAlign.Center) }
                    Button(
                        onClick = viewModel::onSave,
                        enabled = uiState.canSave && !uiState.isLoading,
                        modifier = Modifier.weight(1f),
                        shape = com.htj.habitzy.ui.theme.ShapeFull,
                    ) { Text(text = if (habitId == null) "Create" else "Save") }
                }
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Loading…")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SpaceL, vertical = SpaceS),
                verticalArrangement = Arrangement.spacedBy(SpaceXL),
            ) {
                // 1. Name
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(text = "Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                )

                // 2. Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitIconChip(
                        icon = HabitIcon.Emoji(uiState.emoji ?: "\u2B50"),
                        color = Color(uiState.colorArgb),
                        size = 56,
                    )
                    OutlinedButton(onClick = { showEmojiPicker = true }) {
                        Text(text = "Choose icon")
                    }
                }

                // 3. Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(56.dp)
                            .background(
                                Color(uiState.colorArgb),
                                shape = com.htj.habitzy.ui.theme.ShapeFull,
                            ),
                    )
                    OutlinedButton(onClick = { showColorPicker = true }) {
                        Text(text = "Choose color")
                    }
                }

                // 4. Habit type
                FormSection(title = "Type") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        FormHabitType.values().forEachIndexed { index, t ->
                            SegmentedButton(
                                selected = uiState.type == t,
                                onClick = { viewModel.onTypeChange(t) },
                                shape = SegmentedButtonDefaults.itemShape(index, FormHabitType.values().size),
                            ) {
                                Text(text = t.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    when (uiState.type) {
                        FormHabitType.BINARY -> Unit
                        FormHabitType.AMOUNT -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(SpaceM)) {
                                OutlinedTextField(
                                    value = uiState.amountGoalText,
                                    onValueChange = viewModel::onAmountGoalChange,
                                    label = { Text(text = "Goal") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = uiState.amountUnit,
                                    onValueChange = viewModel::onAmountUnitChange,
                                    label = { Text(text = "Unit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                                UNIT_SUGGESTIONS.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { viewModel.onAmountUnitChange(suggestion) },
                                        label = { Text(text = suggestion) },
                                    )
                                }
                            }
                        }
                        FormHabitType.CHECKLIST -> ChecklistEditor(
                            steps = uiState.checklistSteps,
                            onStepChange = viewModel::onChecklistStepChange,
                            onAdd = viewModel::addChecklistStep,
                            onRemove = viewModel::removeChecklistStep,
                            onMove = viewModel::moveChecklistStep,
                        )
                    }
                }

                // 5. Schedule
                FormSection(title = "Schedule") {
                    ScheduleField(
                        scheduleKind = uiState.scheduleKind,
                        onKindChange = viewModel::onScheduleKindChange,
                        selectedWeekdays = uiState.selectedWeekdays,
                        onToggleWeekday = viewModel::toggleWeekday,
                        timesPerWeekText = uiState.timesPerWeekText,
                        onTimesPerWeekChange = viewModel::onTimesPerWeekChange,
                        timesPerMonthText = uiState.timesPerMonthText,
                        onTimesPerMonthChange = viewModel::onTimesPerMonthChange,
                        everyNDaysText = uiState.everyNDaysText,
                        onEveryNDaysChange = viewModel::onEveryNDaysChange,
                    )
                }

                // 6. Reminders
                FormSection(title = "Reminders") {
                    uiState.reminders.forEachIndexed { index, reminder ->
                        ReminderRow(
                            reminder = reminder,
                            onEnabledChange = { viewModel.updateReminder(index, reminder.copy(enabled = it)) },
                            onMessageChange = { viewModel.updateReminder(index, reminder.copy(message = it)) },
                            onTimeClick = { editingReminderIndex = index },
                            onDelete = { viewModel.removeReminder(index) },
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::addReminder,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(SpaceS))
                        Text(text = "Add reminder")
                    }
                }

                // 7. Category tag
                FormSection(title = "Category") {
                    CategoryTagField(
                        value = uiState.categoryTag,
                        usedTags = uiState.usedCategoryTags,
                        onValueChange = viewModel::onCategoryTagChange,
                    )
                }

                // 8. Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(text = "Description (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(SpaceM))
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPickerSheet(
            selectedEmoji = uiState.emoji,
            onSelected = viewModel::onEmojiChange,
            onDismiss = { showEmojiPicker = false },
        )
    }

    if (showColorPicker) {
        ColorPickerSheet(
            selectedColor = Color(uiState.colorArgb),
            onSelected = { viewModel.onColorChange(it.toArgb()) },
            onDismiss = { showColorPicker = false },
        )
    }

    if (editingReminderIndex >= 0 && editingReminderIndex < uiState.reminders.size) {
        val reminder = uiState.reminders[editingReminderIndex]
        ReminderTimePickerDialog(
            initialHour = reminder.hour,
            initialMinute = reminder.minute,
            onConfirm = { time: LocalTime ->
                viewModel.updateReminder(editingReminderIndex, reminder.copy(hour = time.hour, minute = time.minute))
                editingReminderIndex = -1
            },
            onDismiss = { editingReminderIndex = -1 },
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpaceS),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun ChecklistEditor(
    steps: List<String>,
    onStepChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    if (steps.isEmpty()) return
    val lazyListState = rememberLazyListState()
    val scope = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index != to.index) {
            onMove(from.index, to.index)
        }
    }
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .height((steps.size * 72).dp),
        verticalArrangement = Arrangement.spacedBy(SpaceS),
    ) {
        itemsIndexed(steps, key = { index, _ -> index }) { index, step ->
            ReorderableItem(state = scope, key = index) { isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggableHandle(),
                    horizontalArrangement = Arrangement.spacedBy(SpaceS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = HabitzyShapes.medium,
                        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { onStepChange(index, it) },
                            placeholder = { Text(text = "Step ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (steps.size > 2) {
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove step")
                        }
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(), enabled = steps.size < 8) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(SpaceS))
        Text(text = "Add step")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleField(
    scheduleKind: ScheduleKind,
    onKindChange: (ScheduleKind) -> Unit,
    selectedWeekdays: Set<Int>,
    onToggleWeekday: (Int) -> Unit,
    timesPerWeekText: String,
    onTimesPerWeekChange: (String) -> Unit,
    timesPerMonthText: String,
    onTimesPerMonthChange: (String) -> Unit,
    everyNDaysText: String,
    onEveryNDaysChange: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
        ScheduleKind.values().forEach { kind ->
            FilterChip(
                selected = scheduleKind == kind,
                onClick = { onKindChange(kind) },
                label = { Text(text = kind.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
    Spacer(modifier = Modifier.height(SpaceS))
    when (scheduleKind) {
        ScheduleKind.DAILY -> Text(text = "Every day", style = MaterialTheme.typography.bodyMedium)
        ScheduleKind.SPECIFIC_WEEKDAYS -> {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceS)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    (1..7).forEach { dayValue ->
                        FilterChip(
                            selected = dayValue in selectedWeekdays,
                            onClick = { onToggleWeekday(dayValue) },
                            label = { Text(text = WEEKDAY_LABELS[dayValue - 1]) },
                        )
                    }
                }
            }
        }
        ScheduleKind.TIMES_PER_WEEK -> NumberStepper(
            label = "Times per week",
            value = timesPerWeekText,
            onValueChange = onTimesPerWeekChange,
            min = 1,
        )
        ScheduleKind.TIMES_PER_MONTH -> NumberStepper(
            label = "Times per month",
            value = timesPerMonthText,
            onValueChange = onTimesPerMonthChange,
            min = 1,
        )
        ScheduleKind.EVERY_N_DAYS -> NumberStepper(
            label = "Every N days",
            value = everyNDaysText,
            onValueChange = onEveryNDaysChange,
            min = 1,
        )
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    min: Int,
) {
    val intValue = value.toIntOrNull() ?: min
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange((intValue - 1).coerceAtLeast(min).toString()) }) {
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }
            Text(text = value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            IconButton(onClick = { onValueChange((intValue + 1).toString()) }) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderDraft,
    onEnabledChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onTimeClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = HabitzyShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(SpaceM), verticalArrangement = Arrangement.spacedBy(SpaceS)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onTimeClick, enabled = reminder.enabled) {
                    Text(
                        text = LocalTime.of(reminder.hour, reminder.minute)
                            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete reminder")
                    }
                }
            }
            OutlinedTextField(
                value = reminder.message,
                onValueChange = onMessageChange,
                label = { Text(text = "Message (optional)") },
                singleLine = true,
                enabled = reminder.enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTagField(
    value: String,
    usedTags: List<String>,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SpaceS)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = "Category tag") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val matches = usedTags.filter { it.contains(value.trim(), ignoreCase = true) && it != value.trim() }.take(6)
        if (matches.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                matches.forEach { tag ->
                    AssistChip(
                        onClick = { onValueChange(tag) },
                        label = { Text(text = tag) },
                    )
                }
            }
        }
    }
}

private val FormHabitType.label: String
    get() = when (this) {
        FormHabitType.BINARY -> "Binary"
        FormHabitType.AMOUNT -> "Amount"
        FormHabitType.CHECKLIST -> "Checklist"
    }

private val ScheduleKind.label: String
    get() = when (this) {
        ScheduleKind.DAILY -> "Daily"
        ScheduleKind.SPECIFIC_WEEKDAYS -> "Weekdays"
        ScheduleKind.TIMES_PER_WEEK -> "Per week"
        ScheduleKind.TIMES_PER_MONTH -> "Per month"
        ScheduleKind.EVERY_N_DAYS -> "Every N"
    }
