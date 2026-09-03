package com.htj.habitzy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htj.habitzy.domain.model.Habit
import com.htj.habitzy.domain.model.HabitIcon
import com.htj.habitzy.domain.model.HabitSchedule
import com.htj.habitzy.domain.model.HabitType
import com.htj.habitzy.ui.theme.HabitSwatch1
import com.htj.habitzy.ui.theme.HabitSwatch3
import com.htj.habitzy.ui.theme.HabitSwatch4
import com.htj.habitzy.ui.theme.HabitSwatch5
import com.htj.habitzy.ui.theme.HabitzyShapes
import com.htj.habitzy.ui.theme.SpaceL
import com.htj.habitzy.ui.theme.SpaceM
import com.htj.habitzy.ui.theme.SpaceS
import com.htj.habitzy.ui.theme.SpaceXL
import com.htj.habitzy.ui.theme.ShapeFull
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComponentsCatalogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var segmentedIndex by remember { mutableIntStateOf(0) }
    var switchState by remember { mutableStateOf(true) }
    var textInput by remember { mutableStateOf("") }

    val sampleHabit = Habit(
        id = 1,
        name = "Morning run",
        description = "Run 5 kilometers every morning",
        icon = HabitIcon.Emoji("\uD83C\uDFC3"),
        color = HabitSwatch1.toArgb(),
        type = HabitType.Binary,
        schedule = HabitSchedule.Daily,
        categoryTag = "Health",
        isArchived = false,
        vacationRange = null,
        sortOrder = 0,
        createdAtEpochDay = LocalDate.now().toEpochDay(),
    )

    val sampleAmountHabit = Habit(
        id = 2,
        name = "Drink water",
        description = null,
        icon = HabitIcon.Emoji("\uD83D\uDCA7"),
        color = HabitSwatch3.toArgb(),
        type = HabitType.Amount(goal = 8.0, unit = "glasses"),
        schedule = HabitSchedule.Daily,
        categoryTag = "Health",
        isArchived = false,
        vacationRange = null,
        sortOrder = 1,
        createdAtEpochDay = LocalDate.now().toEpochDay(),
    )

    val sampleChecklistHabit = Habit(
        id = 3,
        name = "Morning routine",
        description = null,
        icon = HabitIcon.Emoji("\uD83E\uDDD0"),
        color = HabitSwatch5.toArgb(),
        type = HabitType.Checklist(steps = listOf("Stretch", "Meditate", "Journal")),
        schedule = HabitSchedule.SpecificWeekdays(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)),
        categoryTag = "Mind",
        isArchived = false,
        vacationRange = null,
        sortOrder = 2,
        createdAtEpochDay = LocalDate.now().toEpochDay(),
    )

    Scaffold(
        snackbarHost = { HabitzySnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(SpaceL),
            verticalArrangement = Arrangement.spacedBy(SpaceXL),
        ) {
            Text(
                text = "Components catalog",
                style = MaterialTheme.typography.headlineMedium,
            )

            // Section: HabitzyTopBar (large)
            CatalogSection("HabitzyTopBar (large)") {
                HabitzyTopBar(title = "Habits", large = true, actions = {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                    )
                })
            }

            // Section: HabitzyTopBar (centered)
            CatalogSection("HabitzyTopBar (centered)") {
                HabitzyTopBar(
                    title = "Habit detail",
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                        )
                    },
                )
            }

            // Section: SectionHeader
            CatalogSection("SectionHeader") {
                SectionHeader(title = "Health habits", actionText = "Show all") {}
            }

            // Section: StreakBadge
            CatalogSection("StreakBadge") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    StreakBadge(streak = 0)
                    StreakBadge(streak = 5)
                    StreakBadge(streak = 42)
                }
            }

            // Section: ProgressRing
            CatalogSection("ProgressRing") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceM), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = 0f, size = 40.dp)
                    ProgressRing(progress = 0.35f, size = 40.dp, color = HabitSwatch3)
                    ProgressRing(progress = 0.75f, size = 40.dp, color = HabitSwatch5)
                    ProgressRing(progress = 1f, size = 40.dp)
                }
            }

            // Section: HabitIconChip
            CatalogSection("HabitIconChip") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    HabitIconChip(icon = HabitIcon.Emoji("\uD83C\uDFC3"), color = HabitSwatch1, size = 48)
                    HabitIconChip(icon = HabitIcon.Emoji("\uD83D\uDCA7"), color = HabitSwatch4, size = 48)
                    HabitIconChip(icon = HabitIcon.Bundled("fitness_run"), color = HabitSwatch5, size = 48)
                }
            }

            // Section: HabitCard
            CatalogSection("HabitCard") {
                HabitCard(
                    habitWithLog = HabitWithTodayLog(habit = sampleHabit, isCompletedToday = false),
                    onBodyClick = {},
                    onToggle = {},
                )
                Spacer(modifier = Modifier.height(SpaceS))
                HabitCard(
                    habitWithLog = HabitWithTodayLog(habit = sampleHabit, isCompletedToday = true),
                    onBodyClick = {},
                    onToggle = {},
                )
                Spacer(modifier = Modifier.height(SpaceS))
                HabitCard(
                    habitWithLog = HabitWithTodayLog(
                        habit = sampleAmountHabit,
                        isCompletedToday = false,
                        amountProgress = 5.0,
                    ),
                    onBodyClick = {},
                    onToggle = {},
                )
                Spacer(modifier = Modifier.height(SpaceS))
                HabitCard(
                    habitWithLog = HabitWithTodayLog(
                        habit = sampleChecklistHabit,
                        isCompletedToday = false,
                        checklistDoneCount = 2,
                    ),
                    onBodyClick = {},
                    onToggle = {},
                )
            }

            // Section: ActivityGrid
            CatalogSection("ActivityGrid") {
                val sampleLogs = remember {
                    val today = LocalDate.now()
                    (0..60).filter { it % 3 != 0 }.associate { daysAgo ->
                        today.minusDays(daysAgo.toLong()) to (0.3f + Math.random() * 0.7f).toFloat()
                    }
                }
                ActivityGrid(
                    logs = sampleLogs,
                    onDayClick = {},
                )
            }

            // Section: EmptyState
            CatalogSection("EmptyState") {
                EmptyState(
                    icon = Icons.Rounded.Favorite,
                    title = "No habits yet",
                    subtitle = "Create your first habit to start tracking",
                    ctaText = "Create habit",
                    onCtaClick = {},
                    modifier = Modifier.height(200.dp),
                )
            }

            // Section: ConfirmDialog
            CatalogSection("ConfirmDialog (click to show)") {
                var showDialog by remember { mutableStateOf(false) }
                Button(onClick = { showDialog = true }) {
                    Text("Show dialog")
                }
                if (showDialog) {
                    ConfirmDialog(
                        title = "Delete habit?",
                        message = "This will permanently delete 'Morning run' and all its history. This cannot be undone.",
                        confirmLabel = "Delete",
                        onConfirm = { showDialog = false },
                        onDismiss = { showDialog = false },
                        isDestructive = true,
                    )
                }
            }

            // Section: IconOrEmojiPicker
            CatalogSection("IconOrEmojiPicker") {
                var selected by remember { mutableStateOf("\uD83C\uDFC3") }
                IconOrEmojiPicker(
                    selectedEmoji = selected,
                    onEmojiSelected = { selected = it },
                )
            }

            // Section: ColorSwatchPicker
            CatalogSection("ColorSwatchPicker") {
                var selected by remember { mutableStateOf(HabitSwatch1) }
                ColorSwatchPicker(
                    selectedColor = selected,
                    onColorSelected = { selected = it },
                )
            }

            // Section: Buttons
            CatalogSection("Buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    Button(onClick = {}) { Text("Filled") }
                    FilledTonalButton(onClick = {}) { Text("Tonal") }
                    OutlinedButton(onClick = {}) { Text("Outlined") }
                    TextButton(onClick = {}) { Text("Text") }
                }
            }

            // Section: Segmented Buttons
            CatalogSection("Segmented buttons") {
                val options = listOf("Binary", "Amount", "Checklist")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            onClick = { segmentedIndex = index },
                            selected = segmentedIndex == index,
                            label = { Text(label) },
                        )
                    }
                }
            }

            // Section: Switch
            CatalogSection("Switch") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark mode")
                    Spacer(modifier = Modifier.width(SpaceS))
                    Switch(checked = switchState, onCheckedChange = { switchState = it })
                }
            }

            // Section: Text field
            CatalogSection("OutlinedTextField") {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Habit name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Section: Cards
            CatalogSection("Cards") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = HabitzyShapes.extraLarge,
                    ) {
                        Column(modifier = Modifier.padding(SpaceL)) {
                            Text("Elevated card", style = MaterialTheme.typography.titleMedium)
                            Text("Surface container", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = HabitzyShapes.extraLarge,
                    ) {
                        Column(modifier = Modifier.padding(SpaceL)) {
                            Text("Elevated card", style = MaterialTheme.typography.titleMedium)
                            Text("With shadow", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Section: Surface colors
            CatalogSection("Surface container hierarchy") {
                Row(horizontalArrangement = Arrangement.spacedBy(SpaceS)) {
                    listOf(
                        "Lowest" to MaterialTheme.colorScheme.surfaceContainerLowest,
                        "Low" to MaterialTheme.colorScheme.surfaceContainerLow,
                        "Default" to MaterialTheme.colorScheme.surfaceContainer,
                        "High" to MaterialTheme.colorScheme.surfaceContainerHigh,
                        "Highest" to MaterialTheme.colorScheme.surfaceContainerHighest,
                    ).forEach { (label, color) ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(color, ShapeFull),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Section: Typography
            CatalogSection("Typography") {
                Text("Display Large", style = MaterialTheme.typography.displayLarge)
                Text("Display Medium", style = MaterialTheme.typography.displayMedium)
                Text("Headline Large", style = MaterialTheme.typography.headlineLarge)
                Text("Headline Medium", style = MaterialTheme.typography.headlineMedium)
                Text("Title Large", style = MaterialTheme.typography.titleLarge)
                Text("Title Medium", style = MaterialTheme.typography.titleMedium)
                Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
                Text("Label Large", style = MaterialTheme.typography.labelLarge)
            }

            // Section: Emphasized typography
            CatalogSection("Emphasized typography") {
                Text("Display Large Emphasized", style = com.htj.habitzy.ui.theme.DisplayLargeEmphasized)
                Text("Display Medium Emphasized", style = com.htj.habitzy.ui.theme.DisplayMediumEmphasized)
                Text("Display Small Emphasized", style = com.htj.habitzy.ui.theme.DisplaySmallEmphasized)
                Text("Headline Large Emphasized", style = com.htj.habitzy.ui.theme.HeadlineLargeEmphasized)
                Text("Headline Medium Emphasized", style = com.htj.habitzy.ui.theme.HeadlineMediumEmphasized)
            }

            // Snackbar demo
            CatalogSection("Snackbar") {
                Button(onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Habit completed! Undo?",
                            actionLabel = "Undo",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }) {
                    Text("Show snackbar")
                }
            }

            Spacer(modifier = Modifier.height(SpaceXL))
        }
    }
}

@Composable
private fun CatalogSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SpaceS)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
}
