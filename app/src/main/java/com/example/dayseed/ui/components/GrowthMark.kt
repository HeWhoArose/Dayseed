package com.example.dayseed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.data.GrowthMetrics
import com.example.dayseed.ui.theme.ExtraBranchColor
import com.example.dayseed.ui.theme.TinySproutColor
import com.example.dayseed.ui.theme.UsualLeafColor

@Composable
fun GrowthMarkBadge(
    metrics: GrowthMetrics,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("growth_mark_badge"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GrowthMarkCanvas(
                        activeDates = metrics.totalActiveDates,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Collected Moments",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${metrics.totalActiveDates} active ${if (metrics.totalActiveDates == 1) "day" else "days"} tended",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (onInfoClick != null) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.testTag("growth_mark_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Explain Growth Mark metric",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Effort mix pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EffortMixChip(
                    symbol = "🌱",
                    label = "Tiny",
                    count = metrics.tinyCount,
                    color = TinySproutColor,
                    modifier = Modifier.weight(1f)
                )
                EffortMixChip(
                    symbol = "🌿",
                    label = "Usual",
                    count = metrics.usualCount,
                    color = UsualLeafColor,
                    modifier = Modifier.weight(1f)
                )
                EffortMixChip(
                    symbol = "🌳",
                    label = "Extra",
                    count = metrics.extraCount,
                    color = ExtraBranchColor,
                    modifier = Modifier.weight(1f)
                )
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

        // Ground base arc
        drawArc(
            color = brandColor.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.7f),
            size = Size(w * 0.7f, h * 0.25f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Seed / Stem
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

        // Left Sprout Leaf (represents tiny & showing up)
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

        // Right Sprout Leaf
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
