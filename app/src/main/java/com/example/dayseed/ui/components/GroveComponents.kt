package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.GroveTerminalReason
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitStreakRun
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.LongestActiveStreakSelection
import com.example.dayseed.data.StreakUnit

@Composable
fun TodayLongestStreakIndicator(
    longestSelection: LongestActiveStreakSelection?,
    onOpenHabitDetail: (String) -> Unit,
    onOpenLivingGrove: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val desc = if (longestSelection != null) {
        "Longest active streak: ${longestSelection.formatIndicatorText()}. Tap to view all individual streaks."
    } else {
        "Longest active streak: No active streak yet. Every small step plants a seed."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_main_streak_indicator")
            .semantics { contentDescription = desc }
            .clickable {
                onOpenLivingGrove(null)
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val treeState = longestSelection?.activeRun?.treeState ?: GroveTreeState.SAPLING_ACTIVE
                Icon(
                    imageVector = when (treeState) {
                        GroveTreeState.GROWING_TREE -> Icons.Default.Forest
                        GroveTreeState.DORMANT_TREE -> Icons.Default.Eco
                        else -> Icons.Default.Spa
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Longest Active Rhythm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (longestSelection != null) {
                        Text(
                            text = longestSelection.formatIndicatorText(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("today_longest_streak_text")
                        )
                    } else {
                        Text(
                            text = "No active streak yet — every small step plants a seed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("today_longest_streak_empty_text")
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (longestSelection != null) {
                    TextButton(
                        onClick = { onOpenHabitDetail(longestSelection.habit.habitId) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("today_longest_streak_habit_btn")
                    ) {
                        Text("Habit", style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(
                    onClick = { onOpenLivingGrove(null) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("today_open_grove_btn")
                ) {
                    Text("Grove", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitBranchCanvas(
    habitName: String,
    activeRun: HabitStreakRun?,
    groveRuns: List<HabitStreakRun>,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val activeState = activeRun?.treeState
    val stateLabel = when (activeState) {
        GroveTreeState.GROWING_TREE -> "Growing Tree (7+ active span days)"
        GroveTreeState.SAPLING_ACTIVE -> "Active Sapling (1–6 active span days)"
        else -> if (groveRuns.isNotEmpty()) {
            "Dormant Grove (${groveRuns.size} preserved ${if (groveRuns.size == 1) "tree" else "trees"})"
        } else {
            "Ready to sprout"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
            .testTag("habit_branch_visual")
            .semantics {
                contentDescription = "$habitName botanical glade: $stateLabel, ${groveRuns.size} permanent Grove trees"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (reducedMotion) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Static view",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            drawHabitGladeVignette(
                habitName = habitName,
                activeRun = activeRun,
                groveRuns = groveRuns,
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Sapling (1–6d)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Growing Tree (7d+)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Dormant Tree (Preserved)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DormantTransitionBanner(
    run: HabitStreakRun,
    habitName: String,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reasonExplanation = formatTerminalReasonExplanation(run.terminalReason, run.brokenOnLocalDate)
    val unitWord = if (run.streakUnit == StreakUnit.WEEKS) {
        if (run.completedStreakCount == 1) "week" else "weeks"
    } else {
        if (run.completedStreakCount == 1) "day" else "days"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dormant_transition_banner_${run.runId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Tree Preserved in Your Living Grove",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$habitName (${run.startLocalDate} – ${run.endLocalDate}, ${run.completedStreakCount} $unitWord, ${run.elapsedActiveSpanCount}d active span) is now a permanent Dormant Tree. $reasonExplanation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onAcknowledge(run.runId) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("ack_dormant_run_${run.runId}")
                ) {
                    Text("Keep in Grove", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun formatTerminalReasonLabel(reason: GroveTerminalReason?): String = when (reason) {
    GroveTerminalReason.MISSED -> "Missed scheduled opportunity"
    GroveTerminalReason.PAUSED -> "Closed when habit was paused"
    GroveTerminalReason.ARCHIVED -> "Closed when habit was archived"
    GroveTerminalReason.COMPLETED -> "Closed when habit was marked completed"
    null -> "Active run"
}

fun formatTerminalReasonExplanation(reason: GroveTerminalReason?, brokenOnDate: String?): String = when (reason) {
    GroveTerminalReason.MISSED ->
        "This run ended on ${brokenOnDate ?: "a missed opportunity"}, and its tree remains permanently in your Grove."
    GroveTerminalReason.PAUSED ->
        "Pausing a habit closes the current streak run cleanly and preserves its tree in your Grove."
    GroveTerminalReason.ARCHIVED ->
        "Archiving a habit closes the active run and keeps your tree in the Grove."
    GroveTerminalReason.COMPLETED ->
        "Completing a habit celebrates and preserves this tree in your Grove."
    null -> "Currently growing."
}

@Composable
fun LivingGroveScreen(
    viewModel: DayseedViewModel,
    initialHabitId: String? = null,
    highlightedRunId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val summaries by viewModel.habitSummaries.collectAsState()
    val allRuns by viewModel.habitStreakRuns.collectAsState()
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val touchedPlantId by viewModel.touchedGrovePlantId.collectAsState()

    var selectedHabitFilter by remember(initialHabitId) { mutableStateOf(initialHabitId) }

    val reducedMotion = remember(appSettings) { viewModel.isReducedMotionEnabled() }
    val unacknowledgedDormant = remember(allRuns, appSettings, selectedHabitFilter) {
        viewModel.unacknowledgedDormantRuns(selectedHabitFilter)
    }

    val filteredHabits = remember(habits, selectedHabitFilter) {
        if (selectedHabitFilter == null) habits else habits.filter { it.habitId == selectedHabitFilter }
    }

    val qualifiedGroveRuns = remember(allRuns, selectedHabitFilter) {
        allRuns.filter { run ->
            run.qualifiedForGrove &&
                (selectedHabitFilter == null || run.habitId == selectedHabitFilter)
        }.reversed()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("living_grove_screen"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Heading: "Your Grove" + Concise Description
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("secondary_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Grove",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Every streak reaching 7 active span days roots a permanent tree in your landscape.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Three Balanced, Non-Cramped Summary Statistics Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroveStatCard(
                    icon = Icons.Default.Spa,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Active Saplings",
                    subtitle = "1–6d span",
                    count = growthMetrics.activeSaplingsCount,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("grove_stat_saplings")
                )
                GroveStatCard(
                    icon = Icons.Default.Forest,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Growing Trees",
                    subtitle = "7d+ active",
                    count = growthMetrics.growingTreesCount,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("grove_stat_growing")
                )
                GroveStatCard(
                    icon = Icons.Default.Eco,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Dormant Trees",
                    subtitle = "Preserved",
                    count = growthMetrics.dormantTreesCount,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("grove_stat_dormant")
                )
            }
        }

        // 3. Dominant Evolving Botanical Landscape Painting (Controls appear ONLY when a leaf is touched)
        item {
            LivingGroveLandscapeSection(
                habits = habits,
                summaries = summaries,
                allRuns = allRuns,
                selectedHabitFilter = selectedHabitFilter,
                touchedPlantId = touchedPlantId,
                onTouchPlant = { viewModel.setTouchedGrovePlantId(it) },
                onSelectHabitFilter = { selectedHabitFilter = it },
                onInspectHabit = { habitId -> viewModel.openHabitDetail(habitId) }
            )
        }

        // 4. One-shot Dormant Transition Notices
        items(unacknowledgedDormant, key = { "dormant_notice_${it.runId}" }) { run ->
            val hName = habits.find { it.habitId == run.habitId }?.name ?: "Habit"
            DormantTransitionBanner(
                run = run,
                habitName = hName,
                onAcknowledge = { viewModel.acknowledgeDormantRun(it) }
            )
        }

        // 5. Compact Unified Permanent Grove Summary Section (below the painting)
        item {
            PermanentGroveSummaryCard(
                qualifiedRuns = qualifiedGroveRuns,
                habits = habits
            )
        }

        // 6. Per-Habit Grove Records
        items(filteredHabits, key = { it.habitId }) { habit ->
            val summary = summaries[habit.habitId]
            HabitGroveCard(
                habit = habit,
                summary = summary,
                highlightedRunId = highlightedRunId,
                reducedMotion = reducedMotion,
                onOpenHabitDetail = { viewModel.openHabitDetail(habit.habitId) }
            )
        }
    }
}

@Composable
private fun GroveStatCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PermanentGroveSummaryCard(
    qualifiedRuns: List<HabitStreakRun>,
    habits: List<Habit>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grove_permanent_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permanent Grove (${qualifiedRuns.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "7+ active span days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (qualifiedRuns.isEmpty()) {
                Text(
                    text = "When any habit reaches 7 elapsed active span days, its tree takes permanent root in your Grove—even if a streak later rests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val growing = qualifiedRuns.count { it.isActive }
                val dormant = qualifiedRuns.count { !it.isActive }
                val habitNames = qualifiedRuns
                    .mapNotNull { r -> habits.find { it.habitId == r.habitId }?.name }
                    .distinct()
                    .joinToString(", ")
                Text(
                    text = "$growing active growing ${if (growing == 1) "tree" else "trees"} and $dormant preserved autumn ${if (dormant == 1) "tree" else "trees"} across $habitNames.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitGroveCard(
    habit: Habit,
    summary: HabitSummary?,
    highlightedRunId: String?,
    reducedMotion: Boolean,
    onOpenHabitDetail: () -> Unit
) {
    val activeRun = summary?.activeRun
    val qualifiedGroveRuns = summary?.groveRuns.orEmpty()
    val shortUnqualifiedRuns = summary?.allStreakRuns.orEmpty().filter { !it.isActive && !it.qualifiedForGrove }
    val streakUnitWord = if (summary?.streakUnit == StreakUnit.WEEKS) "weeks" else "days"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grove_habit_card_${habit.habitId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val activeRunSummary = if (activeRun != null) {
                        "Active streak: ${activeRun.completedStreakCount} $streakUnitWord (${activeRun.elapsedActiveSpanCount} active span days)"
                    } else {
                        "No active streak run right now"
                    }
                    Text(
                        text = activeRunSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onOpenHabitDetail,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("grove_open_detail_${habit.habitId}")
                ) {
                    Text("Inspect Habit")
                }
            }

            HabitBranchCanvas(
                habitName = habit.name,
                activeRun = activeRun,
                groveRuns = qualifiedGroveRuns,
                reducedMotion = reducedMotion
            )

            // Permanent Qualified Grove Trees (>= 7 active span days)
            Text(
                text = "Permanent Grove Trees (${qualifiedGroveRuns.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (qualifiedGroveRuns.isEmpty()) {
                Text(
                    text = "Reaching 7 elapsed active span days grows a permanent tree here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (run in qualifiedGroveRuns.reversed()) {
                        val isHighlighted = run.runId == highlightedRunId
                        val unitLabel = if (run.streakUnit == StreakUnit.WEEKS) {
                            if (run.completedStreakCount == 1) "week" else "weeks"
                        } else {
                            if (run.completedStreakCount == 1) "day" else "days"
                        }
                        val stateBadge = when (run.treeState) {
                            GroveTreeState.GROWING_TREE -> "Growing Tree"
                            GroveTreeState.DORMANT_TREE -> "Dormant Tree"
                            GroveTreeState.SAPLING_ACTIVE -> "Sapling"
                            GroveTreeState.UNQUALIFIED_HISTORY -> "Short run"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isHighlighted) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grove_tree_row_${run.runId}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$stateBadge • ${run.startLocalDate} to ${run.endLocalDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${run.completedStreakCount} $unitLabel (${run.elapsedActiveSpanCount}d span)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = formatTerminalReasonLabel(run.terminalReason),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Short Unqualified Runs (< 7 active span days) kept strictly separate from Grove trees
            if (shortUnqualifiedRuns.isNotEmpty()) {
                Text(
                    text = "Earlier Short Runs (< 7 active span days — not Grove trees): ${shortUnqualifiedRuns.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("short_unqualified_runs_header_${habit.habitId}")
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (shortRun in shortUnqualifiedRuns.reversed()) {
                        val unitLabel = if (shortRun.streakUnit == StreakUnit.WEEKS) "weeks" else "days"
                        Text(
                            text = "• ${shortRun.startLocalDate} – ${shortRun.endLocalDate}: ${shortRun.completedStreakCount} $unitLabel (${shortRun.elapsedActiveSpanCount}d span, ${formatTerminalReasonLabel(shortRun.terminalReason)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("short_run_row_${shortRun.runId}")
                        )
                    }
                }
            }
        }
    }
}
