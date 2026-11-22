package com.example.dayseed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dayseed.ui.theme.DarkProgressExtra
import com.example.dayseed.ui.theme.LightProgressExtra
import java.math.BigInteger

/** How the bar is drawn for a progress fraction (1.0 = the target). */
data class BarLayers(
    /** First lap: share of the bar filled in the normal colour, 0..1. */
    val base: Float,
    /** Second lap drawn over the first in the extra colour, 0..1 (full only at 200% or more). */
    val overflow: Float,
    /** True when progress is beyond the target. */
    val isOver: Boolean
)

/** Splits a progress fraction into the first lap and the overflow lap. Negative or NaN input is treated as 0. */
fun barLayers(fraction: Float): BarLayers {
    val f = if (fraction.isNaN() || fraction < 0f) 0f else fraction
    return BarLayers(
        base = f.coerceAtMost(1f),
        overflow = (f - 1f).coerceIn(0f, 1f),
        isOver = f > 1f
    )
}

/** Progress as a fraction of the target (can exceed 1.0). A zero or negative target gives 0. */
fun progressFraction(current: BigInteger, target: BigInteger): Float {
    if (target.signum() <= 0) return 0f
    return (current.toDouble() / target.toDouble()).toFloat()
}

/** The colour of the second lap. Gold in both themes; chosen per theme so it stays visible. */
@Composable
fun progressExtraColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkProgressExtra else LightProgressExtra

/**
 * Progress bar that keeps going past the target: the first lap fills in the normal colour, and anything beyond
 * 100% is drawn from the start again, on top, in a second colour, with a small gap so the two laps are
 * distinguishable without relying on colour alone. Described to screen readers in words.
 */
@Composable
fun ProgressBarWithOverflow(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    description: String? = null
) {
    val layers = barLayers(fraction)
    val fill = MaterialTheme.colorScheme.primary
    val track = fill.copy(alpha = 0.14f)
    val extra = progressExtraColor()
    val gap = MaterialTheme.colorScheme.surface
    val percent = (if (fraction.isNaN()) 0f else fraction.coerceAtLeast(0f)) * 100f
    val spoken = description ?: if (layers.isOver) {
        "Progress ${percent.toInt()} percent, ${(percent - 100f).toInt()} percent over the target"
    } else {
        "Progress ${percent.toInt()} percent"
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = spoken }
    ) {
        val r = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(track, size = size, cornerRadius = r)
        if (layers.base > 0f) {
            drawRoundRect(fill, size = Size(size.width * layers.base, size.height), cornerRadius = r)
        }
        if (layers.overflow > 0f) {
            val w = size.width * layers.overflow
            drawRoundRect(extra, size = Size(w, size.height), cornerRadius = r)
            // A thin gap at the end of the second lap separates it from the first lap beneath it.
            if (layers.overflow < 1f) {
                val gapW = 2.dp.toPx()
                drawRect(gap, topLeft = Offset((w - gapW / 2f).coerceAtLeast(0f), 0f), size = Size(gapW, size.height))
            }
        }
    }
}
