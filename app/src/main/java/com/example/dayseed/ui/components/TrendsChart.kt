package com.example.dayseed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One day of the chart: how many of that day's scheduled habits were fully met, partly done, or not done. */
data class TrendDay(val date: LocalDate, val met: Int, val partial: Int, val scheduled: Int)

/**
 * "Last 14 days" bar chart for Insights (D-250). Each bar is the share of that day's scheduled habits that were met
 * (solid) or partly done (light). A day with nothing scheduled shows a small dot, never an empty or failed bar.
 * Tapping a day opens its Day Detail.
 */
@Composable
fun TrendsChartCard(
    days: List<TrendDay>,
    today: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalScheduled = days.sumOf { it.scheduled }
    val totalMet = days.sumOf { it.met }
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth().testTag("insights_trends_chart")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Last ${days.size} days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.dayseedHeading()
            )
            Text(
                text = if (totalScheduled == 0) "Nothing was scheduled in this period."
                else "$totalMet of $totalScheduled habit-days met",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("insights_trends_summary")
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                days.forEach { day ->
                    val label = "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${day.date.dayOfMonth}: " +
                        if (day.scheduled == 0) "nothing scheduled" else "${day.met} of ${day.scheduled} met" +
                            if (day.partial > 0) ", ${day.partial} partly done" else ""
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onSelectDay(day.date) }
                            .semantics { contentDescription = "$label. Opens that day." }
                            .testTag("trend_day_${day.date}")
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 2.dp)) {
                            val barW = size.width
                            val r = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            if (day.scheduled == 0) {
                                drawCircle(track, radius = 3.dp.toPx(), center = Offset(barW / 2f, size.height - 3.dp.toPx()))
                            } else {
                                drawRoundRect(track, topLeft = Offset(0f, 0f), size = Size(barW, size.height), cornerRadius = r)
                                val metH = size.height * day.met / day.scheduled
                                val partH = size.height * day.partial / day.scheduled
                                if (partH > 0f) {
                                    drawRoundRect(primary.copy(alpha = 0.42f), topLeft = Offset(0f, size.height - metH - partH), size = Size(barW, partH), cornerRadius = r)
                                }
                                if (metH > 0f) {
                                    drawRoundRect(primary, topLeft = Offset(0f, size.height - metH), size = Size(barW, metH), cornerRadius = r)
                                }
                            }
                        }
                        Text(
                            text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.date == today) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
