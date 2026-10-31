package com.example.dayseed.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.dayseed.ui.theme.DayseedArtwork
import com.example.dayseed.ui.theme.DayseedArtworkBlend
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Painterly botanical and landscape illustrations for each time of day (D-210),
 * faithful to the owner's 5-panel reference sheet:
 * - MORNING_LEAF ("Sage Modern"): lush overlapping watercolor sage branch with veined leaves
 *   sweeping in from the upper-right edge.
 * - DAY_BRANCH ("Field Journal"): naturalist olive & sage sprigs with blooming botanical flowers
 *   and warm clay buds on warm parchment wash.
 * - EVENING_HILLS ("Earth & Clay"): unboxed panoramic sunset hills with glowing low sun,
 *   evergreen pine silhouettes along the ridge, and a winding meadow trail.
 * - NIGHT_MOON ("Night Garden"): luminous golden crescent moon with warm halo and tall
 *   nocturnal sage-olive stems with golden night buds climbing the right edge.
 *
 * Supports continuous cross-dissolve blending (`DayseedArtworkBlend`) and animated transitions
 * so daypart changes shift gradually rather than snapping.
 */

// Morning ("Sage Modern") watercolor palette
private val MorningWashTint = Color(0xFFDCE8DA)
private val MorningMistLeaf = Color(0xFFB3C9B1)
private val MorningLeafLight = Color(0xFF97B898)
private val MorningLeafMid = Color(0xFF638E6C)
private val MorningLeafDeep = Color(0xFF3E6E4E)
private val MorningStemColor = Color(0xFF486F54)
private val MorningVeinColor = Color(0xFF2F543B)

// Midday ("Field Journal") watercolor palette
private val DayWashWarm = Color(0xFFE7E2D0)
private val DayWashClay = Color(0xFFEBD9C7)
private val DayLeafLight = Color(0xFFA5B592)
private val DayLeafOlive = Color(0xFF6B7F58)
private val DayLeafDeep = Color(0xFF475B3A)
private val DayStemBrown = Color(0xFF5D5A46)
private val DayFlowerPetalOuter = Color(0xFFE5B59B)
private val DayFlowerPetalInner = Color(0xFFC97856)
private val DayFlowerCenter = Color(0xFFE3C178)

// Evening ("Earth & Clay") watercolor landscape palette
private val EveningSkyGlow = Color(0xFFF5D4AE)
private val EveningSunCore = Color(0xFFDC8D59)
private val EveningSunHalo = Color(0xFFEEB686)
private val EveningFarRidgeTop = Color(0xFFE4C5A8)
private val EveningFarRidgeBottom = Color(0xFFD1A886)
private val EveningMidClayTop = Color(0xFFC9855E)
private val EveningMidClayBottom = Color(0xFFA85E3A)
private val EveningSageRidgeTop = Color(0xFF7D8C6D)
private val EveningSageRidgeBottom = Color(0xFF536448)
private val EveningNearEarthTop = Color(0xFFB3633D)
private val EveningNearEarthBottom = Color(0xFF8C4425)
private val EveningPineDark = Color(0xFF384A34)
private val EveningPineMid = Color(0xFF4B5F44)
private val EveningTrailLight = Color(0xFFEAD0B3)

// Night ("Night Garden") watercolor nocturnal palette
private val NightMoonBright = Color(0xFFF0D79E)
private val NightMoonGold = Color(0xFFDDB974)
private val NightStarWarm = Color(0xFFEBE4CE)
private val NightLeafLight = Color(0xFF6E9172)
private val NightLeafMid = Color(0xFF4C6D52)
private val NightLeafDeep = Color(0xFF314B37)
private val NightStemColor = Color(0xFF58795D)
private val NightBudGold = Color(0xFFDDA86E)
private val NightFlowerPetal = Color(0xFFD8B480)

/**
 * Draws a rich, painterly botanical leaf with petiole, asymmetric organic curvature,
 * watercolor linear gradient fill, central tapering midrib, and delicate lateral veins.
 */
private fun DrawScope.drawWatercolorLeaf(
    stemAttach: Offset,
    leafBase: Offset,
    leafTip: Offset,
    widthFactor: Float,
    bowBias: Float,
    baseColor: Color,
    midColor: Color,
    tipColor: Color,
    veinColor: Color,
    stemColor: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    val dx = leafTip.x - leafBase.x
    val dy = leafTip.y - leafBase.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val halfW = len * widthFactor

    // Petiole (delicate stalk connecting stem to leaf base)
    val petiole = Path().apply {
        moveTo(stemAttach.x, stemAttach.y)
        quadraticBezierTo(
            (stemAttach.x + leafBase.x) * 0.5f,
            (stemAttach.y + leafBase.y) * 0.5f,
            leafBase.x,
            leafBase.y
        )
    }
    drawPath(
        path = petiole,
        color = stemColor.copy(alpha = (stemColor.alpha * alpha).coerceIn(0f, 1f)),
        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Asymmetric organic leaf blade path
    val upperBulge = halfW * (1f + bowBias)
    val lowerBulge = halfW * (1f - bowBias * 0.5f)
    val blade = Path().apply {
        moveTo(leafBase.x, leafBase.y)
        cubicTo(
            leafBase.x + dx * 0.25f + nx * upperBulge * 1.15f,
            leafBase.y + dy * 0.25f + ny * upperBulge * 1.15f,
            leafBase.x + dx * 0.68f + nx * upperBulge * 0.80f,
            leafBase.y + dy * 0.68f + ny * upperBulge * 0.80f,
            leafTip.x,
            leafTip.y
        )
        cubicTo(
            leafBase.x + dx * 0.70f - nx * lowerBulge * 0.78f,
            leafBase.y + dy * 0.70f - ny * lowerBulge * 0.78f,
            leafBase.x + dx * 0.26f - nx * lowerBulge * 1.12f,
            leafBase.y + dy * 0.26f - ny * lowerBulge * 1.12f,
            leafBase.x,
            leafBase.y
        )
        close()
    }

    val leafBrush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = (baseColor.alpha * alpha).coerceIn(0f, 1f)),
            midColor.copy(alpha = (midColor.alpha * alpha).coerceIn(0f, 1f)),
            tipColor.copy(alpha = (tipColor.alpha * alpha).coerceIn(0f, 1f))
        ),
        start = leafBase,
        end = leafTip
    )
    drawPath(path = blade, brush = leafBrush)

    // Subtle watercolor rim wash on one side for depth
    drawPath(
        path = blade,
        color = veinColor.copy(alpha = 0.14f * alpha),
        style = Stroke(width = 0.9.dp.toPx())
    )

    // Central midrib vein (stops slightly before tip)
    val midribEnd = Offset(
        leafBase.x + dx * 0.88f + nx * (halfW * bowBias * 0.18f),
        leafBase.y + dy * 0.88f + ny * (halfW * bowBias * 0.18f)
    )
    val midrib = Path().apply {
        moveTo(leafBase.x, leafBase.y)
        quadraticBezierTo(
            leafBase.x + dx * 0.45f + nx * (halfW * bowBias * 0.32f),
            leafBase.y + dy * 0.45f + ny * (halfW * bowBias * 0.32f),
            midribEnd.x,
            midribEnd.y
        )
    }
    drawPath(
        path = midrib,
        color = veinColor.copy(alpha = 0.34f * alpha),
        style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
    )

    // Delicate lateral veins
    for (frac in listOf(0.28f, 0.50f, 0.70f)) {
        val vx = leafBase.x + dx * frac + nx * (halfW * bowBias * 0.22f)
        val vy = leafBase.y + dy * frac + ny * (halfW * bowBias * 0.22f)
        val span = (1f - frac * 0.65f) * halfW * 0.65f
        val leftVein = Path().apply {
            moveTo(vx, vy)
            quadraticBezierTo(
                vx + dx * 0.08f + nx * span * 0.55f,
                vy + dy * 0.08f + ny * span * 0.55f,
                vx + dx * 0.15f + nx * span,
                vy + dy * 0.15f + ny * span
            )
        }
        val rightVein = Path().apply {
            moveTo(vx, vy)
            quadraticBezierTo(
                vx + dx * 0.08f - nx * span * 0.55f,
                vy + dy * 0.08f - ny * span * 0.55f,
                vx + dx * 0.15f - nx * span,
                vy + dy * 0.15f - ny * span
            )
        }
        drawPath(
            path = leftVein,
            color = veinColor.copy(alpha = 0.20f * alpha),
            style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = rightVein,
            color = veinColor.copy(alpha = 0.20f * alpha),
            style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws a soft, multi-petal watercolor botanical flower blossom with calyx and stamen center.
 */
private fun DrawScope.drawBotanicalFlower(
    center: Offset,
    radius: Float,
    petalOuter: Color,
    petalInner: Color,
    stamenColor: Color,
    sepalColor: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    val petalCount = 5
    for (i in 0 until petalCount) {
        val angle = (i * 72f - 18f) * (Math.PI / 180.0)
        val tipX = center.x + cos(angle).toFloat() * radius
        val tipY = center.y + sin(angle).toFloat() * radius
        val perpX = -sin(angle).toFloat() * radius * 0.52f
        val perpY = cos(angle).toFloat() * radius * 0.52f

        val petal = Path().apply {
            moveTo(center.x, center.y)
            cubicTo(
                center.x + cos(angle).toFloat() * radius * 0.45f + perpX,
                center.y + sin(angle).toFloat() * radius * 0.45f + perpY,
                tipX + perpX * 0.55f,
                tipY + perpY * 0.55f,
                tipX,
                tipY
            )
            cubicTo(
                tipX - perpX * 0.55f,
                tipY - perpY * 0.55f,
                center.x + cos(angle).toFloat() * radius * 0.45f - perpX,
                center.y + sin(angle).toFloat() * radius * 0.45f - perpY,
                center.x,
                center.y
            )
            close()
        }
        drawPath(
            path = petal,
            brush = Brush.radialGradient(
                colors = listOf(
                    petalInner.copy(alpha = 0.90f * alpha),
                    petalOuter.copy(alpha = 0.72f * alpha)
                ),
                center = center,
                radius = radius
            )
        )
    }
    // Warm stamen center and tiny pollen dots
    drawCircle(
        color = sepalColor.copy(alpha = 0.45f * alpha),
        radius = radius * 0.30f,
        center = center
    )
    drawCircle(
        color = stamenColor.copy(alpha = 0.92f * alpha),
        radius = radius * 0.20f,
        center = center
    )
}

/**
 * Draws an unopened botanical flower bud with a green sepal cup at its base.
 */
private fun DrawScope.drawBotanicalBud(
    base: Offset,
    tip: Offset,
    budColor: Color,
    highlightColor: Color,
    sepalColor: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val w = len * 0.36f

    val bud = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            base.x + dx * 0.35f + nx * w,
            base.y + dy * 0.35f + ny * w,
            base.x + dx * 0.78f + nx * w * 0.65f,
            base.y + dy * 0.78f + ny * w * 0.65f,
            tip.x,
            tip.y
        )
        cubicTo(
            base.x + dx * 0.78f - nx * w * 0.65f,
            base.y + dy * 0.78f - ny * w * 0.65f,
            base.x + dx * 0.35f - nx * w,
            base.y + dy * 0.35f - ny * w,
            base.x,
            base.y
        )
        close()
    }
    drawPath(
        path = bud,
        brush = Brush.linearGradient(
            colors = listOf(
                budColor.copy(alpha = 0.92f * alpha),
                highlightColor.copy(alpha = 0.85f * alpha)
            ),
            start = base,
            end = tip
        )
    )

    // Calyx / green sepal cup wrapping the base of the bud
    val calyx = Path().apply {
        moveTo(base.x, base.y)
        quadraticBezierTo(
            base.x + dx * 0.22f + nx * w * 0.95f,
            base.y + dy * 0.22f + ny * w * 0.95f,
            base.x + dx * 0.42f + nx * w * 0.35f,
            base.y + dy * 0.42f + ny * w * 0.35f
        )
        lineTo(base.x + dx * 0.28f, base.y + dy * 0.28f)
        lineTo(
            base.x + dx * 0.42f - nx * w * 0.35f,
            base.y + dy * 0.42f - ny * w * 0.35f
        )
        quadraticBezierTo(
            base.x + dx * 0.22f - nx * w * 0.95f,
            base.y + dy * 0.22f - ny * w * 0.95f,
            base.x,
            base.y
        )
        close()
    }
    drawPath(path = calyx, color = sepalColor.copy(alpha = 0.85f * alpha))
}

/**
 * Draws a layered evergreen pine/cypress silhouette for the Evening ("Earth & Clay") ridge.
 */
private fun DrawScope.drawPineTreeSilhouette(
    baseCenter: Offset,
    treeHeight: Float,
    treeWidth: Float,
    color: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    for (tier in 0..2) {
        val tierTopY = baseCenter.y - treeHeight * (1f - tier * 0.22f)
        val tierBottomY = baseCenter.y - treeHeight * (0.52f - tier * 0.24f)
        val halfW = treeWidth * (0.36f + tier * 0.16f)
        val tierPath = Path().apply {
            moveTo(baseCenter.x, tierTopY)
            quadraticBezierTo(
                baseCenter.x - halfW * 0.45f,
                (tierTopY + tierBottomY) * 0.5f,
                baseCenter.x - halfW,
                tierBottomY
            )
            lineTo(baseCenter.x + halfW, tierBottomY)
            quadraticBezierTo(
                baseCenter.x + halfW * 0.45f,
                (tierTopY + tierBottomY) * 0.5f,
                baseCenter.x,
                tierTopY
            )
            close()
        }
        drawPath(path = tierPath, color = color.copy(alpha = alpha))
    }
}

private fun DrawScope.drawPainterlyDaypartScene(
    artwork: DayseedArtwork,
    canvasColor: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    val w = size.width
    val h = size.height

    when (artwork) {
        DayseedArtwork.MORNING_LEAF -> {
            // 1. Airy morning watercolor wash in the upper right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MorningWashTint.copy(alpha = 0.65f * alpha),
                        MorningWashTint.copy(alpha = 0.22f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.72f, h * 0.34f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.72f, h * 0.34f)
            )

            // 2. Soft out-of-focus background watercolor sage branch (sweeping from top-right)
            val bgStem = Path().apply {
                moveTo(w * 0.98f, h * 0.04f)
                cubicTo(w * 0.78f, h * 0.20f, w * 0.56f, h * 0.42f, w * 0.30f, h * 0.80f)
            }
            drawPath(
                path = bgStem,
                color = MorningMistLeaf.copy(alpha = 0.45f * alpha),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.80f, h * 0.18f),
                leafBase = Offset(w * 0.76f, h * 0.16f),
                leafTip = Offset(w * 0.40f, h * 0.06f),
                widthFactor = 0.26f,
                bowBias = 0.12f,
                baseColor = MorningMistLeaf,
                midColor = MorningMistLeaf.copy(alpha = 0.75f),
                tipColor = MorningWashTint,
                veinColor = MorningLeafMid,
                stemColor = MorningMistLeaf,
                alpha = 0.48f * alpha
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.64f, h * 0.36f),
                leafBase = Offset(w * 0.68f, h * 0.40f),
                leafTip = Offset(w * 0.96f, h * 0.64f),
                widthFactor = 0.28f,
                bowBias = -0.10f,
                baseColor = MorningMistLeaf,
                midColor = MorningMistLeaf.copy(alpha = 0.72f),
                tipColor = MorningWashTint,
                veinColor = MorningLeafMid,
                stemColor = MorningMistLeaf,
                alpha = 0.45f * alpha
            )

            // 3. Foreground lush "Sage Modern" branch with 7 overlapping veined watercolor leaves
            val mainStem = Path().apply {
                moveTo(w * 0.95f, h * 0.88f)
                cubicTo(w * 0.78f, h * 0.66f, w * 0.62f, h * 0.40f, w * 0.24f, h * 0.10f)
            }
            drawPath(
                path = mainStem,
                color = MorningStemColor.copy(alpha = 0.85f * alpha),
                style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
            )

            // Lower pair of broad sage leaves
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.82f, h * 0.72f),
                leafBase = Offset(w * 0.77f, h * 0.74f),
                leafTip = Offset(w * 0.38f, h * 0.86f),
                widthFactor = 0.27f,
                bowBias = -0.14f,
                baseColor = MorningLeafDeep,
                midColor = MorningLeafMid,
                tipColor = MorningLeafLight,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.88f * alpha
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.76f, h * 0.63f),
                leafBase = Offset(w * 0.80f, h * 0.58f),
                leafTip = Offset(w * 0.99f, h * 0.28f),
                widthFactor = 0.26f,
                bowBias = 0.14f,
                baseColor = MorningLeafDeep,
                midColor = MorningLeafMid,
                tipColor = MorningLeafLight,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.84f * alpha
            )

            // Middle pair of overlapping sage leaves
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.64f, h * 0.46f),
                leafBase = Offset(w * 0.59f, h * 0.49f),
                leafTip = Offset(w * 0.16f, h * 0.56f),
                widthFactor = 0.26f,
                bowBias = -0.12f,
                baseColor = MorningLeafDeep,
                midColor = MorningLeafMid,
                tipColor = MorningLeafLight,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.94f * alpha
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.56f, h * 0.36f),
                leafBase = Offset(w * 0.60f, h * 0.31f),
                leafTip = Offset(w * 0.84f, h * 0.06f),
                widthFactor = 0.25f,
                bowBias = 0.12f,
                baseColor = MorningLeafDeep,
                midColor = MorningLeafMid,
                tipColor = MorningLeafLight,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.90f * alpha
            )

            // Upper pair + terminal leaf at crown
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.44f, h * 0.24f),
                leafBase = Offset(w * 0.39f, h * 0.27f),
                leafTip = Offset(w * 0.08f, h * 0.30f),
                widthFactor = 0.24f,
                bowBias = -0.10f,
                baseColor = MorningLeafMid,
                midColor = MorningLeafLight,
                tipColor = MorningMistLeaf,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.92f * alpha
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.36f, h * 0.18f),
                leafBase = Offset(w * 0.38f, h * 0.14f),
                leafTip = Offset(w * 0.54f, h * 0.01f),
                widthFactor = 0.23f,
                bowBias = 0.10f,
                baseColor = MorningLeafMid,
                midColor = MorningLeafLight,
                tipColor = MorningMistLeaf,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.88f * alpha
            )
            drawWatercolorLeaf(
                stemAttach = Offset(w * 0.24f, h * 0.10f),
                leafBase = Offset(w * 0.23f, h * 0.09f),
                leafTip = Offset(w * 0.05f, h * 0.03f),
                widthFactor = 0.22f,
                bowBias = 0.06f,
                baseColor = MorningLeafDeep,
                midColor = MorningLeafMid,
                tipColor = MorningLeafLight,
                veinColor = MorningVeinColor,
                stemColor = MorningStemColor,
                alpha = 0.95f * alpha
            )
        }

        DayseedArtwork.DAY_BRANCH -> {
            // 1. Warm Field Journal parchment & sepia-clay watercolor wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DayWashWarm.copy(alpha = 0.72f * alpha),
                        DayWashClay.copy(alpha = 0.30f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.68f, h * 0.38f),
                    radius = w * 0.66f
                ),
                radius = w * 0.66f,
                center = Offset(w * 0.68f, h * 0.38f)
            )

            // 2. Secondary botanical sprig in warm sepia/olive
            val secondaryStem = Path().apply {
                moveTo(w * 0.92f, h * 0.78f)
                cubicTo(w * 0.76f, h * 0.56f, w * 0.68f, h * 0.34f, w * 0.52f, h * 0.08f)
            }
            drawPath(
                path = secondaryStem,
                color = DayStemBrown.copy(alpha = 0.60f * alpha),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Main arching Field Journal botanical stem
            val primaryStem = Path().apply {
                moveTo(w * 0.88f, h * 0.94f)
                cubicTo(w * 0.68f, h * 0.68f, w * 0.48f, h * 0.42f, w * 0.14f, h * 0.14f)
            }
            drawPath(
                path = primaryStem,
                color = DayStemBrown.copy(alpha = 0.88f * alpha),
                style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
            )

            // Slender olive & sage leaves along the primary stem
            val leaves = listOf(
                Triple(Offset(w * 0.76f, h * 0.78f), Offset(w * 0.71f, h * 0.80f), Offset(w * 0.34f, h * 0.88f)),
                Triple(Offset(w * 0.70f, h * 0.70f), Offset(w * 0.74f, h * 0.65f), Offset(w * 0.98f, h * 0.44f)),
                Triple(Offset(w * 0.58f, h * 0.56f), Offset(w * 0.52f, h * 0.58f), Offset(w * 0.16f, h * 0.64f)),
                Triple(Offset(w * 0.50f, h * 0.46f), Offset(w * 0.54f, h * 0.40f), Offset(w * 0.84f, h * 0.22f)),
                Triple(Offset(w * 0.36f, h * 0.32f), Offset(w * 0.31f, h * 0.35f), Offset(w * 0.04f, h * 0.40f)),
                Triple(Offset(w * 0.28f, h * 0.24f), Offset(w * 0.31f, h * 0.19f), Offset(w * 0.56f, h * 0.04f)),
                Triple(Offset(w * 0.14f, h * 0.14f), Offset(w * 0.12f, h * 0.13f), Offset(w * 0.02f, h * 0.05f))
            )
            leaves.forEachIndexed { idx, (attach, base, tip) ->
                val isUpper = idx % 2 == 1
                drawWatercolorLeaf(
                    stemAttach = attach,
                    leafBase = base,
                    leafTip = tip,
                    widthFactor = 0.22f,
                    bowBias = if (isUpper) 0.11f else -0.11f,
                    baseColor = DayLeafDeep,
                    midColor = DayLeafOlive,
                    tipColor = DayLeafLight,
                    veinColor = DayLeafDeep,
                    stemColor = DayStemBrown,
                    alpha = 0.90f * alpha
                )
            }

            // Blooming botanical flowers and warm terracotta-clay wildflower buds (Field Journal motif)
            drawBotanicalFlower(
                center = Offset(w * 0.66f, h * 0.26f),
                radius = w * 0.11f,
                petalOuter = DayFlowerPetalOuter,
                petalInner = DayFlowerPetalInner,
                stamenColor = DayFlowerCenter,
                sepalColor = DayLeafOlive,
                alpha = 0.92f * alpha
            )
            drawBotanicalFlower(
                center = Offset(w * 0.36f, h * 0.50f),
                radius = w * 0.09f,
                petalOuter = DayFlowerPetalOuter,
                petalInner = DayFlowerPetalInner,
                stamenColor = DayFlowerCenter,
                sepalColor = DayLeafOlive,
                alpha = 0.86f * alpha
            )
            drawBotanicalBud(
                base = Offset(w * 0.52f, h * 0.08f),
                tip = Offset(w * 0.46f, h * 0.01f),
                budColor = DayFlowerPetalInner,
                highlightColor = DayFlowerPetalOuter,
                sepalColor = DayLeafOlive,
                alpha = 0.90f * alpha
            )
            drawBotanicalBud(
                base = Offset(w * 0.78f, h * 0.44f),
                tip = Offset(w * 0.88f, h * 0.34f),
                budColor = DayFlowerPetalInner,
                highlightColor = DayFlowerCenter,
                sepalColor = DayLeafOlive,
                alpha = 0.88f * alpha
            )
        }

        DayseedArtwork.EVENING_HILLS -> {
            // 1. Warm sunset sky radial glow blending smoothly into the warm stone canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        EveningSkyGlow.copy(alpha = 0.85f * alpha),
                        EveningSunHalo.copy(alpha = 0.38f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.64f, h * 0.34f),
                    radius = w * 0.64f
                ),
                radius = w * 0.64f,
                center = Offset(w * 0.64f, h * 0.34f)
            )

            // 2. Softly glowing low setting sun with layered halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        EveningSunHalo.copy(alpha = 0.55f * alpha),
                        EveningSunHalo.copy(alpha = 0.18f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.65f, h * 0.32f),
                    radius = w * 0.24f
                ),
                radius = w * 0.24f,
                center = Offset(w * 0.65f, h * 0.32f)
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        EveningSunHalo.copy(alpha = 0.95f * alpha),
                        EveningSunCore.copy(alpha = 0.92f * alpha)
                    ),
                    start = Offset(w * 0.65f, h * 0.22f),
                    end = Offset(w * 0.65f, h * 0.42f)
                ),
                radius = w * 0.105f,
                center = Offset(w * 0.65f, h * 0.32f)
            )

            // 3. Far hazy mountain ridge
            val farRidge = Path().apply {
                moveTo(w * 0.04f, h * 0.72f)
                cubicTo(w * 0.26f, h * 0.44f, w * 0.48f, h * 0.58f, w * 0.74f, h * 0.40f)
                quadraticBezierTo(w * 0.90f, h * 0.32f, w * 1.0f, h * 0.38f)
                lineTo(w * 1.0f, h * 0.88f)
                lineTo(w * 0.04f, h * 0.88f)
                close()
            }
            drawPath(
                path = farRidge,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EveningFarRidgeTop.copy(alpha = 0.88f * alpha),
                        EveningFarRidgeBottom.copy(alpha = 0.65f * alpha),
                        canvasColor.copy(alpha = 0.0f)
                    ),
                    startY = h * 0.32f,
                    endY = h * 0.90f
                )
            )

            // 4. Mid-ground warm terracotta hill sloping from the right
            val midClayHill = Path().apply {
                moveTo(w * 0.10f, h * 0.82f)
                cubicTo(w * 0.38f, h * 0.66f, w * 0.64f, h * 0.50f, w * 1.0f, h * 0.44f)
                lineTo(w * 1.0f, h * 0.92f)
                lineTo(w * 0.10f, h * 0.92f)
                close()
            }
            drawPath(
                path = midClayHill,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EveningMidClayTop.copy(alpha = 0.88f * alpha),
                        EveningMidClayBottom.copy(alpha = 0.78f * alpha),
                        canvasColor.copy(alpha = 0.0f)
                    ),
                    startY = h * 0.44f,
                    endY = h * 0.94f
                )
            )

            // 5. Dusky olive-sage ridge on the right with evergreen pine silhouettes (Panel C signature detail)
            val sageHill = Path().apply {
                moveTo(w * 0.26f, h * 0.88f)
                quadraticBezierTo(w * 0.62f, h * 0.64f, w * 1.0f, h * 0.60f)
                lineTo(w * 1.0f, h * 0.94f)
                lineTo(w * 0.26f, h * 0.94f)
                close()
            }
            drawPath(
                path = sageHill,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EveningSageRidgeTop.copy(alpha = 0.86f * alpha),
                        EveningSageRidgeBottom.copy(alpha = 0.80f * alpha),
                        canvasColor.copy(alpha = 0.0f)
                    ),
                    startY = h * 0.60f,
                    endY = h * 0.95f
                )
            )

            // Pine & cypress silhouettes along the right ridge
            drawPineTreeSilhouette(Offset(w * 0.93f, h * 0.66f), h * 0.18f, w * 0.09f, EveningPineDark, 0.88f * alpha)
            drawPineTreeSilhouette(Offset(w * 0.85f, h * 0.68f), h * 0.15f, w * 0.08f, EveningPineMid, 0.84f * alpha)
            drawPineTreeSilhouette(Offset(w * 0.77f, h * 0.71f), h * 0.13f, w * 0.07f, EveningPineDark, 0.82f * alpha)
            drawPineTreeSilhouette(Offset(w * 0.70f, h * 0.74f), h * 0.10f, w * 0.06f, EveningPineMid, 0.78f * alpha)
            drawPineTreeSilhouette(Offset(w * 0.63f, h * 0.77f), h * 0.08f, w * 0.05f, EveningPineDark, 0.72f * alpha)

            // 6. Near warm clay foreground slope + gentle winding trail
            val nearEarth = Path().apply {
                moveTo(0f, h * 0.86f)
                quadraticBezierTo(w * 0.36f, h * 0.74f, w * 0.78f, h * 0.88f)
                quadraticBezierTo(w * 0.90f, h * 0.92f, w * 1.0f, h * 0.90f)
                lineTo(w * 1.0f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = nearEarth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EveningNearEarthTop.copy(alpha = 0.86f * alpha),
                        EveningNearEarthBottom.copy(alpha = 0.65f * alpha),
                        canvasColor.copy(alpha = 0.0f)
                    ),
                    startY = h * 0.74f,
                    endY = h
                )
            )

            // Winding meadow path Ribbon
            val trail = Path().apply {
                moveTo(w * 0.54f, h * 0.74f)
                quadraticBezierTo(w * 0.44f, h * 0.82f, w * 0.48f, h * 0.94f)
                lineTo(w * 0.36f, h * 0.94f)
                quadraticBezierTo(w * 0.36f, h * 0.82f, w * 0.50f, h * 0.74f)
                close()
            }
            drawPath(
                path = trail,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EveningTrailLight.copy(alpha = 0.55f * alpha),
                        Color.Transparent
                    ),
                    startY = h * 0.74f,
                    endY = h * 0.95f
                )
            )
        }

        DayseedArtwork.NIGHT_MOON -> {
            // 1. Multi-layered warm golden moon halo
            val moonCenter = Offset(w * 0.66f, h * 0.28f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NightMoonGold.copy(alpha = 0.28f * alpha),
                        NightMoonGold.copy(alpha = 0.10f * alpha),
                        Color.Transparent
                    ),
                    center = moonCenter,
                    radius = w * 0.38f
                ),
                radius = w * 0.38f,
                center = moonCenter
            )

            // Soft starlight stippling
            val stars = listOf(
                Triple(0.18f, 0.16f, 1.5f),
                Triple(0.36f, 0.10f, 1.1f),
                Triple(0.46f, 0.24f, 1.3f),
                Triple(0.88f, 0.14f, 1.4f),
                Triple(0.24f, 0.38f, 1.0f),
                Triple(0.84f, 0.44f, 1.2f)
            )
            for ((sx, sy, sr) in stars) {
                drawCircle(
                    color = NightStarWarm.copy(alpha = 0.55f * alpha),
                    radius = sr.dp.toPx(),
                    center = Offset(w * sx, h * sy)
                )
            }

            // 2. Luminous golden crescent moon (carved cleanly with warm gradient)
            val moonRadius = w * 0.145f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NightMoonBright.copy(alpha = alpha),
                        NightMoonGold.copy(alpha = alpha)
                    ),
                    start = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
                    end = Offset(moonCenter.x + moonRadius, moonCenter.y + moonRadius)
                ),
                radius = moonRadius,
                center = moonCenter
            )
            // Crescent cutout using the resolved dark pine canvas color
            drawCircle(
                color = canvasColor,
                radius = moonRadius * 0.90f,
                center = Offset(moonCenter.x - moonRadius * 0.38f, moonCenter.y - moonRadius * 0.26f)
            )

            // 3. Tall, visible Night Garden botanical stems climbing up the right side
            val nightStemMain = Path().apply {
                moveTo(w * 0.88f, h * 0.98f)
                cubicTo(w * 0.78f, h * 0.74f, w * 0.84f, h * 0.52f, w * 0.70f, h * 0.34f)
            }
            drawPath(
                path = nightStemMain,
                color = NightStemColor.copy(alpha = 0.88f * alpha),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            val nightStemLeft = Path().apply {
                moveTo(w * 0.64f, h * 0.98f)
                cubicTo(w * 0.52f, h * 0.80f, w * 0.42f, h * 0.66f, w * 0.22f, h * 0.54f)
            }
            drawPath(
                path = nightStemLeft,
                color = NightStemColor.copy(alpha = 0.75f * alpha),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Luminous nocturnal olive-sage leaves (clearly legible on dark pine background)
            val nightLeaves = listOf(
                Triple(Offset(w * 0.84f, h * 0.84f), Offset(w * 0.80f, h * 0.82f), Offset(w * 0.52f, h * 0.72f)),
                Triple(Offset(w * 0.82f, h * 0.70f), Offset(w * 0.85f, h * 0.66f), Offset(w * 0.99f, h * 0.50f)),
                Triple(Offset(w * 0.80f, h * 0.58f), Offset(w * 0.75f, h * 0.56f), Offset(w * 0.46f, h * 0.46f)),
                Triple(Offset(w * 0.74f, h * 0.42f), Offset(w * 0.78f, h * 0.38f), Offset(w * 0.96f, h * 0.24f)),
                Triple(Offset(w * 0.52f, h * 0.80f), Offset(w * 0.47f, h * 0.82f), Offset(w * 0.16f, h * 0.86f)),
                Triple(Offset(w * 0.38f, h * 0.66f), Offset(w * 0.34f, h * 0.68f), Offset(w * 0.08f, h * 0.70f))
            )
            nightLeaves.forEachIndexed { idx, (attach, base, tip) ->
                drawWatercolorLeaf(
                    stemAttach = attach,
                    leafBase = base,
                    leafTip = tip,
                    widthFactor = 0.24f,
                    bowBias = if (idx % 2 == 0) -0.10f else 0.10f,
                    baseColor = NightLeafDeep,
                    midColor = NightLeafMid,
                    tipColor = NightLeafLight,
                    veinColor = NightLeafLight,
                    stemColor = NightStemColor,
                    alpha = 0.92f * alpha
                )
            }

            // Warm golden-clay night flower blossom & buds
            drawBotanicalFlower(
                center = Offset(w * 0.22f, h * 0.54f),
                radius = w * 0.095f,
                petalOuter = NightFlowerPetal,
                petalInner = NightBudGold,
                stamenColor = NightMoonBright,
                sepalColor = NightLeafMid,
                alpha = 0.88f * alpha
            )
            drawBotanicalBud(
                base = Offset(w * 0.70f, h * 0.34f),
                tip = Offset(w * 0.62f, h * 0.24f),
                budColor = NightBudGold,
                highlightColor = NightMoonBright,
                sepalColor = NightLeafMid,
                alpha = 0.90f * alpha
            )
        }
    }
}

/**
 * Full-screen atmospheric background layer that draws subtle edge-bleeding botanical foliage
 * (or nocturnal climbing stems at night, or warm horizon wash in the evening) directly on the
 * screen background canvas so the illustration feels integrated into the page like the
 * reference sheet rather than isolated in a box.
 */
@Composable
fun DaypartBackgroundLayer(
    artworkBlend: DayseedArtworkBlend,
    modifier: Modifier = Modifier
) {
    val canvasColor = MaterialTheme.colorScheme.background
    val smoothFraction by animateFloatAsState(
        targetValue = artworkBlend.blendFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "daypart_bg_blend"
    )

    Canvas(
        modifier = modifier.clearAndSetSemantics {}
    ) {
        val w = size.width
        val h = size.height

        fun drawAmbientEdgeFoliage(art: DayseedArtwork, alpha: Float) {
            if (alpha <= 0.01f) return
            when (art) {
                DayseedArtwork.MORNING_LEAF -> {
                    // Subtle top-right watercolor wash & trailing right-edge sage leaves
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MorningWashTint.copy(alpha = 0.50f * alpha),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.92f, h * 0.10f),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.92f, h * 0.10f)
                    )
                }
                DayseedArtwork.DAY_BRANCH -> {
                    // Subtle warm field-journal corner wash & faint lower-right botanical sprig
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DayWashWarm.copy(alpha = 0.45f * alpha),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.90f, h * 0.12f),
                            radius = w * 0.58f
                        ),
                        radius = w * 0.58f,
                        center = Offset(w * 0.90f, h * 0.12f)
                    )
                    drawWatercolorLeaf(
                        stemAttach = Offset(w * 0.99f, h * 0.68f),
                        leafBase = Offset(w * 0.97f, h * 0.66f),
                        leafTip = Offset(w * 0.85f, h * 0.58f),
                        widthFactor = 0.24f,
                        bowBias = 0.10f,
                        baseColor = DayLeafOlive,
                        midColor = DayLeafLight,
                        tipColor = DayWashWarm,
                        veinColor = DayLeafDeep,
                        stemColor = DayStemBrown,
                        alpha = 0.28f * alpha
                    )
                }
                DayseedArtwork.EVENING_HILLS -> {
                    // Warm golden-hour atmospheric glow across upper-right sky
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EveningSkyGlow.copy(alpha = 0.45f * alpha),
                                EveningSunHalo.copy(alpha = 0.16f * alpha),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.82f, h * 0.12f),
                            radius = w * 0.68f
                        ),
                        radius = w * 0.68f,
                        center = Offset(w * 0.82f, h * 0.12f)
                    )
                }
                DayseedArtwork.NIGHT_MOON -> {
                    // Tall nocturnal botanical stem climbing along the lower-right margin (Panel E)
                    val climbingStem = Path().apply {
                        moveTo(w * 0.98f, h * 0.92f)
                        cubicTo(w * 0.92f, h * 0.74f, w * 0.96f, h * 0.56f, w * 0.90f, h * 0.38f)
                    }
                    drawPath(
                        path = climbingStem,
                        color = NightStemColor.copy(alpha = 0.38f * alpha),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawWatercolorLeaf(
                        stemAttach = Offset(w * 0.95f, h * 0.76f),
                        leafBase = Offset(w * 0.93f, h * 0.74f),
                        leafTip = Offset(w * 0.82f, h * 0.67f),
                        widthFactor = 0.24f,
                        bowBias = -0.10f,
                        baseColor = NightLeafDeep,
                        midColor = NightLeafMid,
                        tipColor = NightLeafLight,
                        veinColor = NightLeafLight,
                        stemColor = NightStemColor,
                        alpha = 0.42f * alpha
                    )
                    drawWatercolorLeaf(
                        stemAttach = Offset(w * 0.94f, h * 0.58f),
                        leafBase = Offset(w * 0.92f, h * 0.56f),
                        leafTip = Offset(w * 0.81f, h * 0.49f),
                        widthFactor = 0.24f,
                        bowBias = -0.10f,
                        baseColor = NightLeafDeep,
                        midColor = NightLeafMid,
                        tipColor = NightLeafLight,
                        veinColor = NightLeafLight,
                        stemColor = NightStemColor,
                        alpha = 0.40f * alpha
                    )
                }
            }
        }

        val primaryAlpha = 1f - smoothFraction
        val secondaryAlpha = smoothFraction
        drawAmbientEdgeFoliage(artworkBlend.primaryArtwork, primaryAlpha)
        if (artworkBlend.secondaryArtwork != artworkBlend.primaryArtwork && secondaryAlpha > 0.01f) {
            drawAmbientEdgeFoliage(artworkBlend.secondaryArtwork, secondaryAlpha)
        }
    }
}

@Composable
fun DaypartArtwork(
    artwork: DayseedArtwork,
    modifier: Modifier = Modifier,
    secondaryArtwork: DayseedArtwork = artwork,
    blendFraction: Float = 0f
) {
    val canvasColor = MaterialTheme.colorScheme.background
    val animatedBlend by animateFloatAsState(
        targetValue = blendFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "daypart_artwork_blend"
    )

    val dominantArtwork = if (animatedBlend >= 0.5f) secondaryArtwork else artwork
    val tag = when (dominantArtwork) {
        DayseedArtwork.MORNING_LEAF -> "daypart_artwork_morning"
        DayseedArtwork.DAY_BRANCH -> "daypart_artwork_day"
        DayseedArtwork.EVENING_HILLS -> "daypart_artwork_evening"
        DayseedArtwork.NIGHT_MOON -> "daypart_artwork_night"
    }

    Canvas(
        modifier = modifier
            .testTag(tag)
            // Decoration only: never occupies a TalkBack stop.
            .clearAndSetSemantics {}
    ) {
        val primaryAlpha = 1f - animatedBlend
        val secondaryAlpha = animatedBlend

        drawPainterlyDaypartScene(
            artwork = artwork,
            canvasColor = canvasColor,
            alpha = primaryAlpha
        )
        if (secondaryArtwork != artwork && secondaryAlpha > 0.01f) {
            drawPainterlyDaypartScene(
                artwork = secondaryArtwork,
                canvasColor = canvasColor,
                alpha = secondaryAlpha
            )
        }
    }
}
