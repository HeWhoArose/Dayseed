package com.example.dayseed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.dayseed.ui.theme.DayseedArtwork

/**
 * Per-daypart header artwork (D-210). The owner's reference sheet defines the visual identity
 * through the palette *and* the picture for each time of day: a fresh morning sprig, a warm
 * daylight branch, layered sunset hills in the evening, and a golden crescent moon with night
 * foliage.
 *
 * All four are drawn with Canvas paths — no bitmap assets, no new dependencies, and fully
 * static (no motion), which keeps reduced-motion behaviour safe. Colour choice is tied to the
 * resolved daypart through the provisional Soft Botanical tokens where meaningful and to fixed
 * decorative constants elsewhere.
 *
 * They are decoration only: semantics are cleared so TalkBack never stops on them.
 */

// Illustration-only decorative colours (not UI text)
private val ArtSage = Color(0xFF9CB08E)
private val ArtSageDeep = Color(0xFF4E7D5B)
private val ArtClaySoft = Color(0xFFC9744B)
private val ArtClayDeep = Color(0xFFA1502C)
private val ArtStoneTint = Color(0xFFE5D8C6)
private val ArtMossDark = Color(0xFF2E4335)
private val ArtMoonGold = Color(0xFFE2C48F)
private val ArtStar = Color(0xFFDFE8D6)

/** Draws a filled leaf between a base point and a tip, bowing perpendicular to the stem line. */
private fun leafPath(baseX: Float, baseY: Float, tipX: Float, tipY: Float, bulge: Float): Path {
    val dx = tipX - baseX
    val dy = tipY - baseY
    val px = -dy * bulge
    val py = dx * bulge
    return Path().apply {
        moveTo(baseX, baseY)
        quadraticBezierTo(baseX + dx / 2 + px, baseY + dy / 2 + py, tipX, tipY)
        quadraticBezierTo(baseX + dx / 2 - px, baseY + dy / 2 - py, baseX, baseY)
        close()
    }
}

private fun DrawScope.drawArtwork(artwork: DayseedArtwork, canvasColor: Color, brandColor: Color) {
    val w = size.width
    val h = size.height

    when (artwork) {
        DayseedArtwork.MORNING_LEAF -> {
            // Ascending sprig: three leaves climbing a gentle S stem.
            val stem = Path().apply {
                moveTo(w * 0.50f, h * 0.92f)
                cubicTo(w * 0.44f, h * 0.70f, w * 0.56f, h * 0.52f, w * 0.50f, h * 0.18f)
            }
            drawPath(stem, ArtSageDeep, style = Stroke(width = 2.2.dp.toPx()))
            drawPath(leafPath(w * 0.50f, h * 0.66f, w * 0.16f, h * 0.54f, 0.16f), ArtSage)
            drawPath(leafPath(w * 0.50f, h * 0.60f, w * 0.84f, h * 0.46f, 0.16f), ArtSage)
            drawPath(leafPath(w * 0.50f, h * 0.34f, w * 0.22f, h * 0.16f, 0.15f), ArtSageDeep)
        }

        DayseedArtwork.DAY_BRANCH -> {
            // Warm leafy branch reaching across the header; brand strokes follow the theme.
            val stem = Path().apply {
                moveTo(w * 0.20f, h * 0.94f)
                cubicTo(w * 0.34f, h * 0.62f, w * 0.44f, h * 0.40f, w * 0.72f, h * 0.10f)
            }
            drawPath(stem, brandColor, style = Stroke(width = 2.4.dp.toPx()))
            drawPath(leafPath(w * 0.30f, h * 0.74f, w * 0.02f, h * 0.62f, 0.15f), ArtSage)
            drawPath(leafPath(w * 0.38f, h * 0.66f, w * 0.68f, h * 0.62f, 0.15f), brandColor)
            drawPath(leafPath(w * 0.44f, h * 0.46f, w * 0.16f, h * 0.36f, 0.14f), ArtSage)
            drawPath(leafPath(w * 0.52f, h * 0.36f, w * 0.82f, h * 0.32f, 0.14f), brandColor)
            drawPath(leafPath(w * 0.66f, h * 0.16f, w * 0.98f, h * 0.06f, 0.13f), ArtSage)
        }

        DayseedArtwork.EVENING_HILLS -> {
            // Layered sunset hills under a low sun ("Earth & Clay"). Sun first, hills overlap it.
            drawCircle(
                ArtMoonGold.copy(alpha = 0.28f),
                radius = w * 0.20f,
                center = Offset(w * 0.66f, h * 0.34f)
            )
            drawCircle(ArtClaySoft, radius = w * 0.085f, center = Offset(w * 0.66f, h * 0.34f))

            val farHill = Path().apply {
                moveTo(0f, h * 0.72f)
                quadraticBezierTo(w * 0.30f, h * 0.42f, w * 0.55f, h * 0.62f)
                quadraticBezierTo(w * 0.80f, h * 0.78f, w * 1.02f, h * 0.58f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(farHill, ArtStoneTint.copy(alpha = 0.9f))

            val midHill = Path().apply {
                moveTo(0f, h * 0.88f)
                quadraticBezierTo(w * 0.36f, h * 0.60f, w * 0.68f, h * 0.78f)
                quadraticBezierTo(w * 0.88f, h * 0.88f, w * 1.02f, h * 0.74f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(midHill, ArtSageDeep.copy(alpha = 0.55f))

            val nearHill = Path().apply {
                moveTo(0f, h)
                quadraticBezierTo(w * 0.28f, h * 0.74f, w * 0.62f, h * 0.92f)
                quadraticBezierTo(w * 0.84f, h * 1.00f, w * 1.02f, h * 0.92f)
                lineTo(w, h)
                close()
            }
            drawPath(nearHill, ArtClayDeep.copy(alpha = 0.85f))
        }

        DayseedArtwork.NIGHT_MOON -> {
            // Night Garden: glowing crescent moon with dark foliage and soft stars.
            drawCircle(ArtStar.copy(alpha = 0.55f), 1.6.dp.toPx(), Offset(w * 0.16f, h * 0.20f))
            drawCircle(ArtStar.copy(alpha = 0.40f), 1.3.dp.toPx(), Offset(w * 0.34f, h * 0.10f))
            drawCircle(ArtStar.copy(alpha = 0.45f), 1.8.dp.toPx(), Offset(w * 0.86f, h * 0.26f))
            drawCircle(ArtStar.copy(alpha = 0.35f), 1.2.dp.toPx(), Offset(w * 0.70f, h * 0.64f))

            // Glow halo, then the moon disc, then the crescent cut (background circle).
            drawCircle(
                ArtMoonGold.copy(alpha = 0.22f),
                w * 0.30f,
                Offset(w * 0.62f, h * 0.32f)
            )
            drawCircle(ArtMoonGold, w * 0.17f, Offset(w * 0.62f, h * 0.32f))
            drawCircle(
                canvasColor,
                w * 0.155f,
                Offset(w * 0.565f, h * 0.27f)
            )

            // Night foliage silhouettes framing the moon.
            drawPath(leafPath(w * 0.06f, h * 0.90f, w * 0.34f, h * 0.66f, 0.17f), ArtMossDark)
            drawPath(
                leafPath(w * 0.10f, h * 0.94f, w * 0.42f, h * 0.84f, 0.14f),
                ArtMossDark.copy(alpha = 0.85f)
            )
            drawPath(leafPath(w * 0.94f, h * 0.86f, w * 0.66f, h * 0.62f, 0.16f), ArtMossDark)
        }
    }
}

@Composable
fun DaypartArtwork(
    artwork: DayseedArtwork,
    modifier: Modifier = Modifier
) {
    val canvasColor = MaterialTheme.colorScheme.background
    val brandColor = MaterialTheme.colorScheme.primary

    val tag = when (artwork) {
        DayseedArtwork.MORNING_LEAF -> "daypart_artwork_morning"
        DayseedArtwork.DAY_BRANCH -> "daypart_artwork_day"
        DayseedArtwork.EVENING_HILLS -> "daypart_artwork_evening"
        DayseedArtwork.NIGHT_MOON -> "daypart_artwork_night"
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .testTag(tag)
            // Decoration only: never occupies a TalkBack stop.
            .clearAndSetSemantics {}
    ) {
        drawArtwork(artwork, canvasColor, brandColor)
    }
}
