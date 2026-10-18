package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.ui.components.GrowthMarkCanvas
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.RestColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val growthMetrics by viewModel.growthMetrics.collectAsState()
    val records by viewModel.records.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()

    val today = LocalDate.now()
    val past30Days = remember(today) {
        (0..29).map { today.minusDays(it.toLong()) }.reversed()
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

        // Big Growth Mark Hero Card
        item {
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
                        text = "Every day you showed up, took a tiny step, or rested.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                    // Grid of 30 days
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
                                        val dayRecords = records.filter { it.date == d }
                                        val hasCompleted = dayRecords.any { it.outcome == DailyOutcome.COMPLETED }
                                        val hasRest = dayRecords.any { it.outcome == DailyOutcome.PLANNED_REST }
                                        val hasJournal = journals.any { it.date == d }

                                        val dotColor = when {
                                            hasCompleted -> MaterialTheme.colorScheme.primary
                                            hasRest -> RestColor
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
                                            } else if (hasRest) {
                                                Text("🌿", fontSize = 10.sp)
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
                        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Completed")
                        LegendItem(color = RestColor, label = "Rest Day")
                        LegendItem(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), label = "Unrecorded")
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
