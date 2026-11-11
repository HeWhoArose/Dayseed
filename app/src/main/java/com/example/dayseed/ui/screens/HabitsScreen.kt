package com.example.dayseed.ui.screens

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.HabitsFilterState
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.data.TrackingMode

@Composable
fun HabitsScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val summaries by viewModel.habitSummaries.collectAsState()
    val filterState by viewModel.habitsFilterState.collectAsState()
    val searchQuery by viewModel.habitsSearchQuery.collectAsState()
    val today = viewModel.repository.clock.todayLocal()

    val lifecycleFiltered = remember(habits, filterState) {
        if (filterState.lifecycleState == null) {
            habits
        } else {
            habits.filter { it.lifecycleState == filterState.lifecycleState }
        }
    }

    val displayedHabits = remember(lifecycleFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            lifecycleFiltered
        } else {
            val q = searchQuery.trim()
            lifecycleFiltered.filter { h ->
                h.name.contains(q, ignoreCase = true) ||
                    h.description.contains(q, ignoreCase = true) ||
                    (h.category?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("habits_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateEditHabit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("habits_add_habit_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new habit")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("habits_directory_list"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habits Directory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Manage schedules, targets, order, and lifecycle states.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.openLivingGrove(null) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("habits_open_grove_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Living Grove")
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setHabitsSearchQuery(it) },
                    placeholder = { Text("Search habits by name, note, or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habits_search_input")
                )
            }

            // Lifecycle Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(HabitsFilterState.values().size) { idx ->
                        val f = HabitsFilterState.values()[idx]
                        val count = if (f.lifecycleState == null) {
                            habits.size
                        } else {
                            habits.count { it.lifecycleState == f.lifecycleState }
                        }
                        FilterChip(
                            selected = filterState == f,
                            onClick = { viewModel.setHabitsFilter(f) },
                            label = { Text("${f.label} ($count)") },
                            modifier = Modifier.testTag("habits_filter_${f.name.lowercase()}")
                        )
                    }
                }
            }

            // Empty States vs Directory List
            if (habits.isEmpty()) {
                item {
                    HabitsEmptyStateCard(
                        title = "No habits created yet",
                        message = "Plant your first habit seed to start tracking at your own pace.",
                        actionLabel = "Create First Habit",
                        onAction = { viewModel.openCreateEditHabit(null) },
                        testTag = "habits_empty_all"
                    )
                }
            } else if (lifecycleFiltered.isEmpty()) {
                item {
                    HabitsEmptyStateCard(
                        title = "No ${filterState.label.lowercase()} habits",
                        message = "Switch filters to view habits in other lifecycle states.",
                        actionLabel = "Show All Habits",
                        onAction = { viewModel.setHabitsFilter(HabitsFilterState.ALL) },
                        testTag = "habits_empty_filter"
                    )
                }
            } else if (displayedHabits.isEmpty()) {
                item {
                    HabitsEmptyStateCard(
                        title = "No habits match \"$searchQuery\"",
                        message = "Try a different keyword or clear your search filter.",
                        actionLabel = "Clear Search",
                        onAction = { viewModel.setHabitsSearchQuery("") },
                        testTag = "habits_empty_search"
                    )
                }
            } else {
                itemsIndexed(displayedHabits, key = { _, h -> h.habitId }) { index, habit ->
                    val summary = summaries[habit.habitId]
                    val globalIndex = habits.indexOfFirst { it.habitId == habit.habitId }
                    val canMoveUp = globalIndex > 0
                    val canMoveDown = globalIndex in 0 until (habits.size - 1)

                    HabitDirectoryCard(
                        habit = habit,
                        summary = summary,
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onOpenDetail = { viewModel.openHabitDetail(habit.habitId, today) },
                        onEditHabit = { viewModel.openCreateEditHabit(habit.habitId) },
                        onMoveUp = { viewModel.reorderHabit(habit.habitId, moveUp = true) },
                        onMoveDown = { viewModel.reorderHabit(habit.habitId, moveUp = false) },
                        onUpdateLifecycle = { newState ->
                            viewModel.updateHabitLifecycle(habit.habitId, newState)
                        },
                        indexInDisplayed = index
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun HabitDirectoryCard(
    habit: Habit,
    summary: HabitSummary?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpenDetail: () -> Unit,
    onEditHabit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdateLifecycle: (HabitLifecycleState) -> Unit,
    indexInDisplayed: Int
) {
    var showMenu by remember { mutableStateOf(false) }
    val eff = summary?.effectiveVersion
    val mode = (eff?.trackingMode ?: habit.trackingMode).canonical()
    val modeLabel = when (mode) {
        TrackingMode.QUANTITY -> "Quantity"
        TrackingMode.DURATION -> "Duration"
        TrackingMode.COMPLETION_ONLY -> "Simple check-off"
        TrackingMode.EFFORT_AWARE, TrackingMode.EFFORT_LEVELS -> "Effort-Aware (Legacy)"
    }
    val targetSummary = summary?.formatTargetAndPeriodSummary()
        ?: habit.targetValue?.let { "$it ${habit.unitCode ?: "units"}" }
        ?: "Daily check-off"
    val scheduleSummary = summary?.formatScheduleSummary() ?: habit.scheduleKind.name

    val streakUnitWord = if (summary?.streakUnit == StreakUnit.WEEKS) {
        if (summary.currentCompletedStreak == 1) "week" else "weeks"
    } else {
        if (summary?.currentCompletedStreak == 1) "day" else "days"
    }
    val groveBadge = when (summary?.activeRun?.treeState) {
        GroveTreeState.GROWING_TREE -> "🌳 Growing Tree"
        GroveTreeState.SAPLING_ACTIVE -> "🌱 Active Sapling"
        else -> if ((summary?.groveRuns?.size ?: 0) > 0) {
            "🍂 ${summary?.groveRuns?.size} Grove ${if (summary?.groveRuns?.size == 1) "tree" else "trees"}"
        } else {
            "No Grove tree yet"
        }
    }
    val lifetimeText = if (summary?.lifetimeGoalDays != null) {
        "${summary.lifetimeActiveDays} / ${summary.lifetimeGoalDays} active days"
    } else {
        "${summary?.lifetimeActiveDays ?: 0} active days"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
            .testTag("habits_directory_card_${habit.habitId}")
            .semantics {
                contentDescription = "${habit.name}, ${habit.lifecycleState.name}, $modeLabel, $targetSummary, $scheduleSummary"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                try {
                                    Color(habit.colorHex.toColorInt())
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (habit.lifecycleState) {
                                    HabitLifecycleState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                    HabitLifecycleState.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
                                    HabitLifecycleState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                                    HabitLifecycleState.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = habit.lifecycleState.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!habit.category.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = habit.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Reorder & Lifecycle Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reorder_up_${habit.habitId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move ${habit.name} up"
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reorder_down_${habit.habitId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move ${habit.name} down"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("habits_card_menu_${habit.habitId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Lifecycle options for ${habit.name}"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Habit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditHabit()
                                },
                                modifier = Modifier.testTag("habits_menu_edit_${habit.habitId}")
                            )
                            if (habit.lifecycleState != HabitLifecycleState.PAUSED) {
                                DropdownMenuItem(
                                    text = { Text("Pause Habit") },
                                    leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateLifecycle(HabitLifecycleState.PAUSED)
                                    },
                                    modifier = Modifier.testTag("habits_menu_pause_${habit.habitId}")
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Resume Habit") },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateLifecycle(HabitLifecycleState.ACTIVE)
                                    },
                                    modifier = Modifier.testTag("habits_menu_resume_${habit.habitId}")
                                )
                            }
                            if (habit.lifecycleState != HabitLifecycleState.COMPLETED) {
                                DropdownMenuItem(
                                    text = { Text("Mark Completed") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateLifecycle(HabitLifecycleState.COMPLETED)
                                    },
                                    modifier = Modifier.testTag("habits_menu_complete_${habit.habitId}")
                                )
                            }
                            if (habit.lifecycleState != HabitLifecycleState.ARCHIVED) {
                                DropdownMenuItem(
                                    text = { Text("Archive Habit") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateLifecycle(HabitLifecycleState.ARCHIVED)
                                    },
                                    modifier = Modifier.testTag("habits_menu_archive_${habit.habitId}")
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Unarchive Habit") },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateLifecycle(HabitLifecycleState.ACTIVE)
                                    },
                                    modifier = Modifier.testTag("habits_menu_unarchive_${habit.habitId}")
                                )
                            }
                        }
                    }
                }
            }

            // Metadata Summary Row
            Text(
                text = "$modeLabel • $targetSummary • $scheduleSummary",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("habits_meta_${habit.habitId}")
            )

            // Streak, Grove & Lifetime Active Days Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary?.currentCompletedStreak ?: 0} $streakUnitWord streak • $groveBadge",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("habits_streak_${habit.habitId}")
                )
                Text(
                    text = lifetimeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("habits_lifetime_${habit.habitId}")
                )
            }
        }
    }
}

@Composable
private fun HabitsEmptyStateCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}
