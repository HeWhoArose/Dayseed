package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.GroveTerminalReason
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitStreakRun
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.LongestActiveStreakSelection
import com.example.dayseed.data.StreakUnit
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.RestColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor

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

@Composable
fun HabitBranchCanvas(
    habitName: String,
    activeRun: HabitStreakRun?,
    groveRuns: List<HabitStreakRun>,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
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

    val branchColor = MaterialTheme.colorScheme.primary
    val leafColor = UsualLeafColor
    val dormantColor = RestColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
            .testTag("habit_branch_visual")
            .semantics {
                contentDescription = "$habitName botanical branch: $stateLabel, ${groveRuns.size} permanent Grove trees"
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (reducedMotion) {
                Text(
                    text = "Static view",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        ) {
            val w = size.width
            val h = size.height

            // Main horizontal botanical branch
            val branchPath = Path().apply {
                moveTo(w * 0.06f, h * 0.72f)
                cubicTo(
                    w * 0.35f, h * 0.64f,
                    w * 0.65f, h * 0.68f,
                    w * 0.94f, h * 0.56f
                )
            }
            drawPath(
                path = branchPath,
                color = branchColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw up to 6 preserved Grove trees along the left/middle of the branch
            val displayedGrove = groveRuns.takeLast(6)
            displayedGrove.forEachIndexed { idx, run ->
                val x = w * (0.16f + idx * 0.11f)
                val y = h * 0.66f
                val isDormant = !run.isActive
                val nodeColor = if (isDormant) dormantColor else ExtraBranchColor

                // Trunk
                drawLine(
                    color = branchColor,
                    start = Offset(x, y),
                    end = Offset(x, y - 22.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Canopy: full circle for Growing Tree, double ring for Dormant Tree (non-colour distinction)
                drawCircle(
                    color = nodeColor.copy(alpha = if (isDormant) 0.55f else 0.85f),
                    radius = 9.dp.toPx(),
                    center = Offset(x, y - 26.dp.toPx()),
                    style = Fill
                )
                if (isDormant) {
                    drawCircle(
                        color = branchColor,
                        radius = 11.dp.toPx(),
                        center = Offset(x, y - 26.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Draw active sapling or active growing shoot at the tip of the branch
            if (activeRun != null && activeRun.isActive) {
                val tipX = w * 0.86f
                val tipY = h * 0.58f
                drawLine(
                    color = leafColor,
                    start = Offset(tipX, tipY),
                    end = Offset(tipX, tipY - 18.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Two sprout leaves
                val leftLeaf = Path().apply {
                    moveTo(tipX, tipY - 12.dp.toPx())
                    quadraticTo(tipX - 10.dp.toPx(), tipY - 20.dp.toPx(), tipX - 4.dp.toPx(), tipY - 24.dp.toPx())
                    quadraticTo(tipX, tipY - 18.dp.toPx(), tipX, tipY - 12.dp.toPx())
                }
                drawPath(leftLeaf, color = TinySproutColor, style = Fill)
                if (activeRun.qualifiedForGrove) {
                    drawCircle(
                        color = leafColor,
                        radius = 8.dp.toPx(),
                        center = Offset(tipX + 4.dp.toPx(), tipY - 22.dp.toPx()),
                        style = Fill
                    )
                }
            }
        }

        // Legend with non-colour symbols and labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌱 Sapling (1–6d)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "🌳 Growing Tree (7d+)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "🍂 Dormant Tree (Preserved)",
                style = MaterialTheme.typography.bodySmall,
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
                text = "🍂 Tree Preserved in Your Living Grove",
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

    var selectedHabitFilter by remember(initialHabitId) { mutableStateOf(initialHabitId) }
    val reducedMotion = remember(appSettings) { viewModel.isReducedMotionEnabled() }
    val unacknowledgedDormant = remember(allRuns, appSettings, selectedHabitFilter) {
        viewModel.unacknowledgedDormantRuns(selectedHabitFilter)
    }

    val filteredHabits = remember(habits, selectedHabitFilter) {
        if (selectedHabitFilter == null) habits else habits.filter { it.habitId == selectedHabitFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("living_grove_screen"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Navigation
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
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Living Grove & Individual Streaks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Every streak reaching 7 active span days becomes a permanent tree in your Grove.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Grove Summary Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroveStatPill(
                    icon = "🌱",
                    title = "Active Saplings",
                    subtitle = "1–6d span",
                    count = growthMetrics.activeSaplingsCount,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grove_stat_saplings")
                )
                GroveStatPill(
                    icon = "🌳",
                    title = "Growing Trees",
                    subtitle = "7d+ active",
                    count = growthMetrics.growingTreesCount,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grove_stat_growing")
                )
                GroveStatPill(
                    icon = "🍂",
                    title = "Dormant Trees",
                    subtitle = "Preserved",
                    count = growthMetrics.dormantTreesCount,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grove_stat_dormant")
                )
            }
        }

        // One-shot Dormant Transition Notices
        items(unacknowledgedDormant, key = { "dormant_notice_${it.runId}" }) { run ->
            val hName = habits.find { it.habitId == run.habitId }?.name ?: "Habit"
            DormantTransitionBanner(
                run = run,
                habitName = hName,
                onAcknowledge = { viewModel.acknowledgeDormantRun(it) }
            )
        }

        // Habit Filter Chips
        if (habits.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedHabitFilter == null,
                            onClick = { selectedHabitFilter = null },
                            label = { Text("All Habits") },
                            modifier = Modifier.testTag("grove_filter_all")
                        )
                    }
                    items(habits, key = { "filter_${it.habitId}" }) { habit ->
                        FilterChip(
                            selected = selectedHabitFilter == habit.habitId,
                            onClick = {
                                selectedHabitFilter = if (selectedHabitFilter == habit.habitId) null else habit.habitId
                            },
                            label = { Text(habit.name) },
                            modifier = Modifier.testTag("grove_filter_${habit.habitId}")
                        )
                    }
                }
            }
        }

        // Per-Habit Branch & Grove History Cards
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
private fun GroveStatPill(
    icon: String,
    title: String,
    subtitle: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            GroveTreeState.GROWING_TREE -> "🌳 Growing Tree"
                            GroveTreeState.DORMANT_TREE -> "🍂 Dormant Tree"
                            GroveTreeState.SAPLING_ACTIVE -> "🌱 Sapling"
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
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
