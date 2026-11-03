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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.GrowthMetrics
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CollectedMomentsSummaryCard(
    metrics: GrowthMetrics,
    modifier: Modifier = Modifier,
    referenceDate: LocalDate = LocalDate.now(),
    onOpenDetail: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    val formattedMonthDuration = DurationNormalizer.formatTotalMinutes(metrics.currentMonthDurationMinutes)
    val currentMonthSessions = metrics.currentMonthDateSessionCounts.values.sum()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("collected_moments_card")
            .then(
                if (onOpenDetail != null) {
                    Modifier.clickable { onOpenDetail() }
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = "Collected moments summary: ${metrics.totalActiveDates} active days tended, " +
                    "$formattedMonthDuration focused duration this month, $currentMonthSessions sessions this month"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("growth_mark_badge")
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
                    GrowthMarkCanvas(
                        activeDates = metrics.totalActiveDates,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Collected moments",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${metrics.totalActiveDates} active ${if (metrics.totalActiveDates == 1) "day" else "days"} tended",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This month: $formattedMonthDuration logged • $currentMonthSessions ${if (currentMonthSessions == 1) "session" else "sessions"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("growth_mark_g2_summary")
                        )
                    }
                }

                if (onInfoClick != null) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("growth_mark_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Explain Collected moments metric",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current-Month Session Dot Strip (up to 14 recent days of the current month)
            val yearMonth = YearMonth.from(referenceDate)
            val monthDays = (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }
            val recentMonthDays = monthDays.filter { !it.isAfter(referenceDate) }.takeLast(14)

            if (recentMonthDays.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("growth_mark_g3_dots"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (d in recentMonthDays) {
                        val sessions = metrics.currentMonthDateSessionCounts[d] ?: 0
                        val dotColor = when {
                            sessions >= 3 -> ExtraBranchColor
                            sessions == 2 -> UsualLeafColor
                            sessions == 1 -> TinySproutColor
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrowthMarkBadge(
    metrics: GrowthMetrics,
    modifier: Modifier = Modifier,
    referenceDate: LocalDate = LocalDate.now(),
    onOpenDetail: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    CollectedMomentsSummaryCard(
        metrics = metrics,
        modifier = modifier,
        referenceDate = referenceDate,
        onOpenDetail = onOpenDetail,
        onInfoClick = onInfoClick
    )
}

@Composable
fun CollectedMomentsDetailSheet(
    metrics: GrowthMetrics,
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedMonthDuration = DurationNormalizer.formatTotalMinutes(metrics.currentMonthDurationMinutes)
    val currentMonthSessions = metrics.currentMonthDateSessionCounts.values.sum()
    val monthLabel = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val activeMonthDays = metrics.currentMonthDateSessionCounts.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.key }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("collected_moments_detail_sheet"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        text = "Collected Moments — $monthLabel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cumulative time and sessions tended without streak penalties.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${metrics.totalActiveDates} Lifetime Active Days Tended",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Focused duration this month: $formattedMonthDuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Recorded sessions this month: $currentMonthSessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Planned rest days taken: ${metrics.plannedRestCount} • Reflections written: ${metrics.journalEntriesCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Days Tended in $monthLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (activeMonthDays.isEmpty()) {
            item {
                Text(
                    text = "No sessions recorded in $monthLabel yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(activeMonthDays, key = { it.key.toString() }) { (date, sessionCount) ->
                val isSelected = date == selectedDate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collected_moment_date_row_$date")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$sessionCount ${if (sessionCount == 1) "session" else "sessions"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EffortMixChip(
    symbol: String,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = symbol, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GrowthMarkCanvas(
    activeDates: Int,
    modifier: Modifier = Modifier
) {
    val brandColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawArc(
            color = brandColor.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.7f),
            size = Size(w * 0.7f, h * 0.25f),
            style = Stroke(width = 3.dp.toPx())
        )

        val stemPath = Path().apply {
            moveTo(w * 0.5f, h * 0.8f)
            cubicTo(
                w * 0.48f, h * 0.6f,
                w * 0.52f, h * 0.4f,
                w * 0.5f, h * 0.25f
            )
        }
        drawPath(
            path = stemPath,
            color = brandColor,
            style = Stroke(width = 3.dp.toPx())
        )

        val leftLeaf = Path().apply {
            moveTo(w * 0.5f, h * 0.42f)
            quadraticTo(w * 0.25f, h * 0.38f, w * 0.22f, h * 0.28f)
            quadraticTo(w * 0.38f, h * 0.26f, w * 0.5f, h * 0.42f)
        }
        drawPath(
            path = leftLeaf,
            color = brandColor,
            style = Fill
        )

        val rightLeaf = Path().apply {
            moveTo(w * 0.5f, h * 0.32f)
            quadraticTo(w * 0.75f, h * 0.28f, w * 0.78f, h * 0.18f)
            quadraticTo(w * 0.62f, h * 0.16f, w * 0.5f, h * 0.32f)
        }
        drawPath(
            path = rightLeaf,
            color = brandColor,
            style = Fill
        )
    }
}
