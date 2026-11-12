package com.example.dayseed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dayseed.ui.theme.DarkBrand
import com.example.dayseed.ui.theme.DarkTextPrimary
import com.example.dayseed.ui.theme.LightBrand
import com.example.dayseed.ui.theme.LightTextPrimary
import com.example.dayseed.ui.theme.NightGoldAccent
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.TinySproutColor

/**
 * Official Dayseed Logo Identity System.
 *
 * Official Product Motto: "Your progress is more than your streak."
 *
 * Implements the master 240x240 vector emblem (opening seed body, emerging asymmetric
 * two-leaf sprout, and restrained open horizon/day-cycle cradle arc) in three semantic
 * colour modes derived strictly from existing Dayseed tokens (`DESIGN.md` / `Color.kt`):
 * - [DayseedLogoColorMode.AUTO]: Automatically selects PRIMARY on light surfaces and REVERSED on dark surfaces.
 * - [DayseedLogoColorMode.PRIMARY]: Primary botanical & earth palette for warm paper/light surfaces (`#F7F5EE`).
 * - [DayseedLogoColorMode.REVERSED]: High-contrast botanical & warm-ivory palette for dark surfaces (`#121A15` / `#1D2820`).
 * - [DayseedLogoColorMode.MONOCHROME]: Single solid colour silhouette using positive/negative space separation.
 */
enum class DayseedLogoColorMode {
    AUTO,
    PRIMARY,
    REVERSED,
    MONOCHROME
}

const val DAYSEED_BRAND_NAME = "Dayseed"
const val DAYSEED_OFFICIAL_MOTTO = "Your progress is more than your streak."

private data class DayseedEmblemPalette(
    val horizonArc: Color,
    val seedBody: Color,
    val secondaryLeafLeft: Color,
    val primaryStemAndLeafRight: Color,
    val wordmark: Color
)

@Composable
private fun resolveEmblemPalette(
    mode: DayseedLogoColorMode,
    monochromeTint: Color = MaterialTheme.colorScheme.onSurface
): DayseedEmblemPalette {
    val isDark = isSystemInDarkTheme() ||
        MaterialTheme.colorScheme.background.red < 0.25f
    val effectiveMode = when (mode) {
        DayseedLogoColorMode.AUTO ->
            if (isDark) DayseedLogoColorMode.REVERSED else DayseedLogoColorMode.PRIMARY
        else -> mode
    }
    return when (effectiveMode) {
        DayseedLogoColorMode.PRIMARY, DayseedLogoColorMode.AUTO -> DayseedEmblemPalette(
            horizonArc = SoftBotanicalThemeConfig.EveningBrand, // #AC5530 warm terracotta-earth horizon
            seedBody = LightTextPrimary,                // #24352B deep forest-earth seed coat
            secondaryLeafLeft = TinySproutColor,        // #558B6E fresh sage young leaf
            primaryStemAndLeafRight = LightBrand,       // #286143 primary botanical green
            wordmark = LightTextPrimary                 // #24352B
        )
        DayseedLogoColorMode.REVERSED -> DayseedEmblemPalette(
            horizonArc = NightGoldAccent,               // #D8B984 warm night gold horizon
            seedBody = DarkTextPrimary,                 // #F3F2E8 warm ivory seed coat
            secondaryLeafLeft = DarkBrand,              // #88B38C soft sage leaf
            primaryStemAndLeafRight = Color(0xFF9CCB9C),// #9CCB9C luminous botanical green (DESIGN.md)
            wordmark = DarkTextPrimary                  // #F3F2E8
        )
        DayseedLogoColorMode.MONOCHROME -> DayseedEmblemPalette(
            horizonArc = monochromeTint,
            seedBody = monochromeTint,
            secondaryLeafLeft = monochromeTint,
            primaryStemAndLeafRight = monochromeTint,
            wordmark = monochromeTint
        )
    }
}

private class DayseedMasterVectorPaths {
    // 1. Open Horizon / Day-Cycle Cradle Arc (240x240 master coordinate space)
    val horizonArc = Path().apply {
        moveTo(50f, 172f)
        cubicTo(47f, 169f, 51f, 164f, 56f, 167f)
        cubicTo(78f, 184f, 98f, 192f, 120f, 192f)
        cubicTo(142f, 192f, 162f, 184f, 184f, 167f)
        cubicTo(189f, 164f, 193f, 169f, 190f, 172f)
        cubicTo(168f, 195f, 145f, 206f, 120f, 206f)
        cubicTo(95f, 206f, 72f, 195f, 50f, 172f)
        close()
    }

    // 2. Cohesive Opening Seed Body
    val openingSeedBody = Path().apply {
        moveTo(120f, 184f)
        cubicTo(90f, 184f, 72f, 165f, 72f, 140f)
        cubicTo(72f, 124f, 82f, 110f, 96f, 108f)
        cubicTo(103f, 107f, 108f, 112f, 109f, 120f)
        cubicTo(111f, 134f, 114f, 148f, 120f, 148f)
        cubicTo(126f, 148f, 129f, 134f, 131f, 120f)
        cubicTo(132f, 112f, 137f, 107f, 144f, 108f)
        cubicTo(158f, 110f, 168f, 124f, 168f, 140f)
        cubicTo(168f, 165f, 150f, 184f, 120f, 184f)
        close()
    }

    // 3. Secondary Young Leaf (Left Unfurling Leaf)
    val sproutLeafLeft = Path().apply {
        moveTo(108f, 101f)
        cubicTo(86f, 101f, 68f, 88f, 60f, 68f)
        cubicTo(58f, 64f, 61f, 60f, 66f, 61f)
        cubicTo(88f, 63f, 104f, 76f, 110f, 95f)
        cubicTo(111f, 99f, 110f, 101f, 108f, 101f)
        close()
    }

    // 4. Primary Rising Sprout Stem & Right Leaf
    val sproutStemAndPrimaryLeaf = Path().apply {
        moveTo(115f, 138f)
        cubicTo(115f, 112f, 117f, 88f, 125f, 68f)
        cubicTo(135f, 45f, 156f, 34f, 178f, 34f)
        cubicTo(182f, 34f, 184f, 37f, 183f, 41f)
        cubicTo(177f, 70f, 158f, 92f, 133f, 99f)
        cubicTo(127f, 101f, 125f, 106f, 125f, 114f)
        lineTo(125f, 138f)
        cubicTo(125f, 143f, 115f, 143f, 115f, 138f)
        close()
    }
}

/**
 * Deliverable A / D: Symbol-only Dayseed Emblem.
 * Scales cleanly from 24dp/32dp header icons up to full splash-screen hero marks.
 */
@Composable
fun DayseedLogoSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    colorMode: DayseedLogoColorMode = DayseedLogoColorMode.AUTO,
    monochromeTint: Color = MaterialTheme.colorScheme.onSurface,
    contentDesc: String? = "Dayseed emblem"
) {
    val palette = resolveEmblemPalette(colorMode, monochromeTint)
    val paths = remember { DayseedMasterVectorPaths() }

    Canvas(
        modifier = modifier
            .size(size)
            .let { mod ->
                if (contentDesc != null) {
                    mod.semantics { contentDescription = contentDesc }
                } else {
                    mod
                }
            }
            .testTag("dayseed_logo_symbol")
    ) {
        val scaleFactor = this.size.minDimension / 240f
        scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path = paths.horizonArc, color = palette.horizonArc)
            drawPath(path = paths.openingSeedBody, color = palette.seedBody)
            drawPath(path = paths.sproutLeafLeft, color = palette.secondaryLeafLeft)
            drawPath(path = paths.sproutStemAndPrimaryLeaf, color = palette.primaryStemAndLeafRight)
        }
    }
}

/**
 * Deliverable B / E: Horizontal Dayseed Lockup (Emblem + independent Wordmark).
 */
@Composable
fun DayseedHorizontalLockup(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 40.dp,
    colorMode: DayseedLogoColorMode = DayseedLogoColorMode.AUTO
) {
    val palette = resolveEmblemPalette(colorMode)
    Row(
        modifier = modifier.testTag("dayseed_horizontal_lockup"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DayseedLogoSymbol(
            size = emblemSize,
            colorMode = colorMode,
            contentDesc = null
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = DAYSEED_BRAND_NAME,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = palette.wordmark
        )
    }
}

/**
 * Deliverable C & Section 10: Stacked Opening / Splash Brand Lockup.
 *
 * Displays:
 * 1. The Dayseed emblem
 * 2. The wordmark "Dayseed"
 * 3. Below the wordmark, in smaller, calmer supporting text:
 *    "Your progress is more than your streak." (with no second tagline).
 */
@Composable
fun DayseedOpeningBrandLockup(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 72.dp,
    showMotto: Boolean = true,
    colorMode: DayseedLogoColorMode = DayseedLogoColorMode.AUTO
) {
    val palette = resolveEmblemPalette(colorMode)
    Column(
        modifier = modifier.testTag("dayseed_opening_brand_lockup"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DayseedLogoSymbol(
            size = emblemSize,
            colorMode = colorMode,
            contentDesc = "Dayseed seed and emerging sprout emblem"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = DAYSEED_BRAND_NAME,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.25.sp
            ),
            color = palette.wordmark,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("dayseed_brand_wordmark")
        )
        if (showMotto) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = DAYSEED_OFFICIAL_MOTTO,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("dayseed_brand_motto")
            )
        }
    }
}
