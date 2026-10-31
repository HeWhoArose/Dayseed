package com.example.dayseed.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.ui.components.GrowthMarkCanvas
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.RestColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor

@Composable
fun InsightsScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val outcomes by viewModel.habitDayOutcomes.collectAsState()
    val entries by viewModel.habitEntries.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    var moodPeriodKind by remember { mutableStateOf("WEEK") }
    var compareWithPrevious by remember { mutableStateOf(false) }
    var selectedMoodFilter by remember { mutableStateOf<String?>(null) }

    val today = viewModel.repository.clock.todayLocal()
    val past30Days = remember(today) {
        (0..29).map { today.minusDays(it.toLong()) }.reversed()
    }

    val moodInsights = remember(journals, appSettings, moodPeriodKind, selectedMoodFilter, today) {
        viewModel.repository.computeMoodInsights(
            periodKind = moodPeriodKind,
            referenceDate = today,
            selectedMoodFilter = selectedMoodFilter
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_screen"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        item {
            Column {
                Text(
                    text = "Growth & Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Evidence of your effort, not an arbitrary score or moral judgment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Big Growth Mark Hero Card (G2 + G3)
        item {
            val formattedDuration = DurationNormalizer.formatTotalMinutes(growthMetrics.currentMonthDurationMinutes)
            val totalMonthSessions = growthMetrics.currentMonthDateSessionCounts.values.sum()
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GrowthMarkCanvas(
                        activeDates = growthMetrics.totalActiveDates,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${growthMetrics.totalActiveDates} Active Dates",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Monthly Collected moments: $formattedDuration • $totalMonthSessions sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every day you showed up, took a tiny step, or reflected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Mood Insights Card (D-096..D-102, AC-009)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_insights_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mood Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = moodInsights.periodLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = moodPeriodKind == "WEEK",
                                onClick = { moodPeriodKind = "WEEK" },
                                label = { Text("This Week") },
                                modifier = Modifier.testTag("mood_period_week_chip")
                            )
                            FilterChip(
                                selected = moodPeriodKind == "MONTH",
                                onClick = { moodPeriodKind = "MONTH" },
                                label = { Text("This Month") },
                                modifier = Modifier.testTag("mood_period_month_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compare with previous period (${moodInsights.previousPeriodTotalEntries} entries)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = compareWithPrevious,
                            onCheckedChange = { compareWithPrevious = it },
                            modifier = Modifier.testTag("mood_compare_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (moodInsights.distribution.isEmpty()) {
                        Text(
                            text = "No mood-tagged journal entries in this period yet. Add a primary mood when writing a reflection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (item in moodInsights.distribution) {
                                val isSelected = selectedMoodFilter == item.moodCode
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            selectedMoodFilter = if (isSelected) null else item.moodCode
                                        }
                                        .padding(6.dp)
                                        .testTag("mood_dist_row_${item.moodCode.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.symbol} ${item.label}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val prevSuffix = if (compareWithPrevious) {
                                            " (prev: ${item.previousPeriodCount})"
                                        } else {
                                            ""
                                        }
                                        Text(
                                            text = "${item.count} (${item.percentage}%)$prevSuffix",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }

                        if (selectedMoodFilter != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Matching entries for $selectedMoodFilter (${moodInsights.matchingEntries.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            for (entry in moodInsights.matchingEntries) {
                                Text(
                                    text = "• ${entry.localDate}: ${entry.title ?: entry.body.take(60)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 30-Day Activity Mosaic
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "30-Day Rhythm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "A growing timeline without punishing breaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (week in 0..3) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (dayIndex in 0..6) {
                                    val itemIdx = week * 7 + dayIndex
                                    if (itemIdx < past30Days.size) {
                                        val d = past30Days[itemIdx]
                                        val dStr = d.toString()
                                        val hasCompleted = outcomes.any { it.localDate == dStr } ||
                                            entries.any { it.localDate == dStr && it.valueUnits > 0 }
                                        val hasJournal = journals.any { it.localDate == dStr }

                                        val dotColor = when {
                                            hasCompleted -> MaterialTheme.colorScheme.primary
                                            hasJournal -> TinySproutColor
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(dotColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasCompleted) {
                                                Text("🌱", fontSize = 10.sp)
                                            } else if (hasJournal) {
                                                Text("✍️", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Habit Logged")
                        LegendItem(color = TinySproutColor, label = "Journaled")
                        LegendItem(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), label = "Quiet Day")
                    }
                }
            }
        }

        // Effort Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Effort Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tiny steps carry the same weight as stretch goals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val totalEfforts = (growthMetrics.tinyCount + growthMetrics.usualCount + growthMetrics.extraCount).coerceAtLeast(1)

                    EffortBar(
                        symbol = "🌱",
                        label = "Tiny Actions",
                        count = growthMetrics.tinyCount,
                        ratio = growthMetrics.tinyCount.toFloat() / totalEfforts,
                        color = TinySproutColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    EffortBar(
                        symbol = "🌿",
                        label = "Usual Actions",
                        count = growthMetrics.usualCount,
                        ratio = growthMetrics.usualCount.toFloat() / totalEfforts,
                        color = UsualLeafColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    EffortBar(
                        symbol = "🌳",
                        label = "Extra Stretch Actions",
                        count = growthMetrics.extraCount,
                        ratio = growthMetrics.extraCount.toFloat() / totalEfforts,
                        color = ExtraBranchColor
                    )
                }
            }
        }

        // Principles & Transparency
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dayseed Quality Principles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. No streak resets. Taking a break is natural human life.\n" +
                            "2. Zero cloud surveillance. All metrics computed locally.\n" +
                            "3. No artificial gamification, badges, or moral pressure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EffortBar(
    symbol: String,
    label: String,
    count: Int,
    ratio: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$symbol $label", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("$count recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
