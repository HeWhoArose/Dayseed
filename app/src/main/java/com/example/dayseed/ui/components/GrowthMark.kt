package com.example.dayseed.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.data.CollectedMomentsMonthDetail
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.GrowthMetrics
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * G2 — Collected Moments Summary Card (D-032, D-201).
 * Summarizes normalized DURATION habit entries for the current month alongside habit-active days.
 */
@Composable
fun CollectedMomentsSummaryCard(
    metrics: GrowthMetrics,
    referenceDate: LocalDate,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onOpenDetail: () -> Unit = {},
    onRetryLoad: () -> Unit = {},
    onInfoClick: (() -> Unit)? = null
) {
    GrowthMarkSummaryCard(
        metrics = metrics,
        currentMonth = YearMonth.from(referenceDate),
        modifier = modifier,
        reducedMotion = reducedMotion,
        onOpenDetail = onOpenDetail,
        onRetryLoad = onRetryLoad,
        onInfoClick = onInfoClick
    )
}

@Composable
fun GrowthMarkSummaryCard(
    metrics: GrowthMetrics,
    currentMonth: YearMonth,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onOpenDetail: () -> Unit = {},
    onRetryLoad: () -> Unit = {},
    onInfoClick: (() -> Unit)? = null
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val formattedDuration = DurationNormalizer.formatCollectedMomentsDuration(metrics.currentMonthDurationMinutes)
    val durationSessionCount = metrics.currentMonthDurationPositiveSessionsCount

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                if (!metrics.loadError.isNullOrBlank()) {
                    onRetryLoad()
                } else {
                    onOpenDetail()
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Collected moments summary: ${metrics.habitActiveDatesCount} active days tended, " +
                    "$formattedDuration collected in ${currentMonth.format(monthFormatter)}. Tap for monthly detail."
            }
            .testTag("growth_mark_summary_card")
    ) {
        if (metrics.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .testTag("growth_mark_loading_state")
            ) {
                Text(
                    text = "Loading Collected Moments...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (!metrics.loadError.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .testTag("growth_mark_error_state")
            ) {
                Text(
                    text = "Unable to load — tap to retry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            BotanicalGrowthCanvas(
                                activeDates = metrics.habitActiveDatesCount,
                                reducedMotion = reducedMotion,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "COLLECTED MOMENTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formattedDuration,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("growth_mark_duration_value")
                            )
                            val subtitle = when {
                                metrics.currentMonthDurationMinutes > 0L ->
                                    "Collected moments · $formattedDuration this month (${currentMonth.format(monthFormatter)}) • $durationSessionCount ${if (durationSessionCount == 1) "session" else "sessions"}"
                                metrics.currentMonthDurationZeroSessionsCount > 0 ->
                                    "Collected moments · 0 min this month (0 min recorded)"
                                metrics.habitActiveDatesCount > 0 ->
                                    "Collected moments · No duration recorded this month (${metrics.habitActiveDatesCount} habit-active ${if (metrics.habitActiveDatesCount == 1) "day" else "days"})"
                                else ->
                                    "Collected moments · No duration recorded this month"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("growth_mark_subtitle")
                            )
                            if (metrics.currentMonthUnsupportedDurationUnits.isNotEmpty()) {
                                Text(
                                    text = "Unsupported duration unit excluded: ${metrics.currentMonthUnsupportedDurationUnits.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("growth_mark_unsupported_unit_notice")
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onInfoClick != null) {
                            TextButton(
                                onClick = onInfoClick,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("growth_mark_info_btn")
                            ) {
                                Text("Info", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GrowthMetricPill(label = "Habit Days", value = metrics.habitActiveDatesCount.toString())
                    GrowthMetricPill(label = "Tiny Steps", value = metrics.tinyCount.toString())
                    GrowthMetricPill(label = "Usual", value = metrics.usualCount.toString())
                    GrowthMetricPill(label = "Extra", value = metrics.extraCount.toString())
                }
            }
        }
    }
}

@Composable
private fun GrowthMetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * G3 — Collected Moments Detail View (`SecondaryRoute.CollectedMomentsDetail`).
 * Displays the monthly normalized duration total, month switcher, per-date breakdown,
 * and individual duration sessions (distinguishing positive sessions from explicit `0` check-ins).
 */
@Composable
fun CollectedMomentsDetailSheet(
    metrics: GrowthMetrics,
    yearMonth: YearMonth,
    referenceToday: LocalDate,
    selectedDate: LocalDate?,
    monthDetail: CollectedMomentsMonthDetail? = null,
    onSelectMonth: (YearMonth, LocalDate?) -> Unit = { _, _ -> },
    onEditSession: (habitId: String, date: LocalDate, entryId: String) -> Unit = { _, _, _ -> },
    onInspectDayDetail: (LocalDate) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

    val resolvedTotalMinutes = monthDetail?.totalMinutes ?: metrics.currentMonthDurationMinutes
    val formattedDuration = DurationNormalizer.formatCollectedMomentsDuration(resolvedTotalMinutes)
    val positiveSessionsCount = monthDetail?.positiveSessionsCount
        ?: metrics.currentMonthDurationPositiveSessionsCount
    val explicitZeroCount = monthDetail?.explicitZeroSessionsCount
        ?: metrics.currentMonthDurationZeroSessionsCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("collected_moments_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("collected_moments_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Collected Moments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = yearMonth.format(monthFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("collected_moments_month_label")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (yearMonth != YearMonth.from(referenceToday)) {
                        TextButton(
                            onClick = { onSelectMonth(YearMonth.from(referenceToday), null) },
                            modifier = Modifier.testTag("g3_jump_current_month_btn")
                        ) {
                            Text("Current Month")
                        }
                    }
                    IconButton(
                        onClick = { onSelectMonth(yearMonth.minusMonths(1), null) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("g3_prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    IconButton(
                        onClick = { onSelectMonth(yearMonth.plusMonths(1), null) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("g3_next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }
        }

        // Monthly Total Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collected_moments_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BotanicalGrowthCanvas(
                        activeDates = metrics.habitActiveDatesCount,
                        reducedMotion = true,
                        modifier = Modifier.size(92.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("collected_moments_total_duration")
                    )
                    Text(
                        text = "$positiveSessionsCount positive duration ${if (positiveSessionsCount == 1) "session" else "sessions"} in ${yearMonth.format(monthFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (explicitZeroCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$explicitZeroCount explicit 0-minute check-in ${if (explicitZeroCount == 1) "session" else "sessions"} (contributed 0m to total)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("collected_moments_zero_sessions_badge")
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${metrics.habitActiveDatesCount} Habit-Active Days Tended",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (monthDetail != null && monthDetail.unsupportedUnitCodes.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collected_moments_unsupported_unit_banner")
                ) {
                    Text(
                        text = "Unsupported duration unit excluded from totals: ${monthDetail.unsupportedUnitCodes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDate != null) {
                        "Sessions on ${selectedDate.format(dateFormatter)}"
                    } else {
                        "Dates & Duration Sessions in ${yearMonth.format(monthFormatter)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedDate != null) {
                    TextButton(
                        onClick = { onSelectMonth(yearMonth, null) },
                        modifier = Modifier.testTag("collected_moments_show_all_dates")
                    ) {
                        Text("All Month Dates")
                    }
                }
            }
        }

        val summariesList = if (monthDetail != null) {
            val all = monthDetail.dateSummaries.values.toList()
            if (selectedDate != null) all.filter { it.date == selectedDate } else all
        } else {
            emptyList()
        }

        if (monthDetail != null) {
            if (summariesList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collected_moments_empty_state")
                    ) {
                        Text(
                            text = if (selectedDate != null) {
                                "No duration sessions recorded on ${selectedDate.format(dateFormatter)}."
                            } else {
                                "No duration habit sessions recorded in ${yearMonth.format(monthFormatter)} yet. Check-off and quantity habits are tracked in Habits and Overview."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(summariesList, key = { it.date.toString() }) { dateSummary ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collected_moments_date_${dateSummary.date}")
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
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onInspectDayDetail(dateSummary.date) }
                                ) {
                                    Text(
                                        text = dateSummary.date.format(dateFormatter),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val sub = buildString {
                                        append("${dateSummary.positiveSessionsCount} positive ${if (dateSummary.positiveSessionsCount == 1) "session" else "sessions"}")
                                        if (dateSummary.explicitZeroSessionsCount > 0) {
                                            append(" • ${dateSummary.explicitZeroSessionsCount} explicit 0 check-in")
                                        }
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = dateSummary.formattedTotalDuration,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            dateSummary.sessions.forEach { session ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onEditSession(session.habitId, session.localDate, session.entryId)
                                        }
                                        .testTag("collected_moments_session_${session.entryId}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (session.isExplicitZero) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.habitName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!session.entryNote.isNullOrBlank()) {
                                                Text(
                                                    text = session.entryNote,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = session.formattedDuration,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (session.isExplicitZero) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback when monthDetail is not passed
            val sortedDates = metrics.currentMonthDurationDateSessionCounts.entries
                .ifEmpty { metrics.currentMonthDateSessionCounts.entries }
                .sortedByDescending { it.key }
            if (sortedDates.isEmpty()) {
                item {
                    Text(
                        text = "No sessions recorded in ${yearMonth.format(monthFormatter)} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("collected_moments_empty_state")
                    )
                }
            } else {
                items(sortedDates, key = { it.key.toString() }) { (date, count) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collected_moments_date_$date")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date.format(dateFormatter),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$count ${if (count == 1) "session" else "sessions"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BotanicalGrowthCanvas(
    activeDates: Int,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val stage = when {
        activeDates <= 0 -> 0
        activeDates in 1..2 -> 1
        activeDates in 3..6 -> 2
        activeDates in 7..13 -> 3
        activeDates in 14..29 -> 4
        else -> 5
    }

    val targetScale = if (stage == 0) 0.5f else 1f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (reducedMotion) tween(0) else tween(600, easing = FastOutSlowInEasing),
        label = "growth_scale"
    )

    val stemColor = Color(0xFF286143)
    val leafColor = Color(0xFF3E7B59)
    val bloomColor = Color(0xFFC88022)
    val soilColor = Color(0xFF8C7A6B)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val bottomY = h * 0.85f

        drawLine(
            color = soilColor,
            start = Offset(w * 0.25f, bottomY),
            end = Offset(w * 0.75f, bottomY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        if (stage == 0) {
            drawCircle(
                color = soilColor,
                radius = min(w, h) * 0.08f,
                center = Offset(cx, bottomY - 6f)
            )
            return@Canvas
        }

        val stemHeight = h * (0.2f + stage * 0.1f) * animatedScale
        val topY = bottomY - stemHeight

        drawLine(
            color = stemColor,
            start = Offset(cx, bottomY),
            end = Offset(cx, topY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        val leafPairs = min(stage, 4)
        for (i in 1..leafPairs) {
            val progress = i.toFloat() / (leafPairs + 1)
            val ly = bottomY - stemHeight * progress
            val leafLen = w * 0.18f * animatedScale

            val leftPath = Path().apply {
                moveTo(cx, ly)
                quadraticTo(cx - leafLen, ly - leafLen * 0.6f, cx - leafLen * 1.1f, ly - leafLen * 0.1f)
                quadraticTo(cx - leafLen * 0.5f, ly + leafLen * 0.3f, cx, ly)
                close()
            }
            drawPath(leftPath, color = leafColor)

            if (stage >= 2) {
                val rightPath = Path().apply {
                    moveTo(cx, ly)
                    quadraticTo(cx + leafLen, ly - leafLen * 0.6f, cx + leafLen * 1.1f, ly - leafLen * 0.1f)
                    quadraticTo(cx + leafLen * 0.5f, ly + leafLen * 0.3f, cx, ly)
                    close()
                }
                drawPath(rightPath, color = leafColor)
            }
        }

        if (stage >= 4) {
            val petalRadius = min(w, h) * 0.07f * animatedScale
            val petalCount = if (stage >= 5) 6 else 4
            for (p in 0 until petalCount) {
                val angle = (p * (360f / petalCount)) * (Math.PI / 180.0)
                val px = cx + (cos(angle) * petalRadius * 1.3f).toFloat()
                val py = topY + (sin(angle) * petalRadius * 1.3f).toFloat()
                drawCircle(
                    color = bloomColor.copy(alpha = 0.85f),
                    radius = petalRadius,
                    center = Offset(px, py)
                )
            }
            drawCircle(
                color = stemColor,
                radius = petalRadius * 0.6f,
                center = Offset(cx, topY)
            )
        }
    }
}
