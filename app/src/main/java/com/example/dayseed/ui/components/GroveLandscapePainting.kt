package com.example.dayseed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitStreakRun
import com.example.dayseed.data.HabitSummary
import com.example.dayseed.data.StreakUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Visual stage in the evolving botanical storybook painting.
 * Mapped deterministically from StreakAndGroveEngine outputs without altering business rules.
 */
enum class BotanicalPaintingStage {
    SEED_MOUND,          // Stage 1: Habit created, 0 active streak days so far
    EARLY_SAPLING,       // Stage 2: SAPLING_ACTIVE (1..6 elapsedActiveSpanCount)
    ESTABLISHED_TREE,    // Stage 3: GROWING_TREE (7..13 elapsedActiveSpanCount)
    MATURE_WOODLAND_TREE,// Stage 4: GROWING_TREE (14+ elapsedActiveSpanCount)
    DORMANT_AUTUMN_TREE  // Stage 5: DORMANT_TREE (closed run with >= 7 elapsedActiveSpanCount)
}

data class PaintedBotanicalPlant(
    val id: String,
    val habitId: String,
    val habitName: String,
    val habitColor: Color,
    val runId: String?,
    val stage: BotanicalPaintingStage,
    val treeState: GroveTreeState,
    val completedStreakCount: Int,
    val elapsedActiveSpanCount: Int,
    val streakUnit: StreakUnit,
    val startLocalDate: String?,
    val endLocalDate: String?,
    val rootNormX: Float,
    val rootNormY: Float,
    val leafCenterNormX: Float,
    val leafCenterNormY: Float,
    val leafHitRadiusNorm: Float,
    val scaleFactor: Float,
    val depthLayer: Int, // 0 = distant ridge, 1 = mid hillside, 2 = foreground glade
    val seedHash: Int
)

/**
 * Deterministically maps real habits, summaries, and streak runs into botanical painting specimens
 * positioned across the rolling hills and foreground meadow glade.
 */
fun buildBotanicalLandscapeSpecimens(
    habits: List<Habit>,
    summaries: Map<String, HabitSummary>,
    allRuns: List<HabitStreakRun>
): List<PaintedBotanicalPlant> {
    val plants = mutableListOf<PaintedBotanicalPlant>()

    // 1. Active habits (or habits with an active streak run / newly planted habits)
    val visibleHabits = habits.filter { it.lifecycleState != HabitLifecycleState.ARCHIVED || summaries[it.habitId]?.activeRun != null }

    // Golden-ratio horizontal distribution slots flanking the central winding footpath
    val foregroundSlotsX = listOf(0.62f, 0.31f, 0.77f, 0.20f, 0.42f, 0.85f, 0.14f, 0.54f, 0.69f)

    visibleHabits.forEachIndexed { index, habit ->
        val summary = summaries[habit.habitId]
        val activeRun = summary?.activeRun
        val parsedColor = parseHabitBotanicalTint(habit.colorHex)
        val slotX = foregroundSlotsX[index % foregroundSlotsX.size] +
            ((index / foregroundSlotsX.size) * 0.04f * if (index % 2 == 0) 1f else -1f)
        val clampedX = slotX.coerceIn(0.14f, 0.86f)

        val seedHash = habit.habitId.fold(17) { acc, c -> acc * 31 + c.code }

        if (activeRun != null && activeRun.isActive) {
            val span = activeRun.elapsedActiveSpanCount.coerceAtLeast(1)
            val stage = when {
                activeRun.treeState == GroveTreeState.SAPLING_ACTIVE || span < 7 -> BotanicalPaintingStage.EARLY_SAPLING
                span < 14 -> BotanicalPaintingStage.ESTABLISHED_TREE
                else -> BotanicalPaintingStage.MATURE_WOODLAND_TREE
            }
            // Place larger growing trees slightly grounded in the rich mid-foreground meadow
            val depthLayer = if (stage == BotanicalPaintingStage.EARLY_SAPLING) 2 else 1
            val terrainY = evaluateMeadowGroundNormY(clampedX, depthLayer)
            val scale = when (stage) {
                BotanicalPaintingStage.EARLY_SAPLING -> 0.72f + (span.coerceIn(1, 6) * 0.045f)
                BotanicalPaintingStage.ESTABLISHED_TREE -> 0.88f + ((span - 7).coerceIn(0, 6) * 0.025f)
                BotanicalPaintingStage.MATURE_WOODLAND_TREE -> (1.05f + ((span - 14).coerceIn(0, 46) * 0.006f)).coerceAtMost(1.32f)
                else -> 0.8f
            }
            val canopyOffsetY = when (stage) {
                BotanicalPaintingStage.EARLY_SAPLING -> 0.11f * scale
                BotanicalPaintingStage.ESTABLISHED_TREE -> 0.24f * scale
                BotanicalPaintingStage.MATURE_WOODLAND_TREE -> 0.28f * scale
                else -> 0.08f
            }
            val hitRadius = when (stage) {
                BotanicalPaintingStage.EARLY_SAPLING -> 0.075f
                BotanicalPaintingStage.ESTABLISHED_TREE -> 0.11f
                BotanicalPaintingStage.MATURE_WOODLAND_TREE -> 0.13f
                else -> 0.07f
            }

            plants += PaintedBotanicalPlant(
                id = "active_${habit.habitId}",
                habitId = habit.habitId,
                habitName = habit.name,
                habitColor = parsedColor,
                runId = activeRun.runId,
                stage = stage,
                treeState = activeRun.treeState,
                completedStreakCount = activeRun.completedStreakCount,
                elapsedActiveSpanCount = activeRun.elapsedActiveSpanCount,
                streakUnit = activeRun.streakUnit,
                startLocalDate = activeRun.startLocalDate,
                endLocalDate = activeRun.endLocalDate,
                rootNormX = clampedX,
                rootNormY = terrainY,
                leafCenterNormX = clampedX,
                leafCenterNormY = (terrainY - canopyOffsetY).coerceIn(0.18f, 0.85f),
                leafHitRadiusNorm = hitRadius,
                scaleFactor = scale,
                depthLayer = depthLayer,
                seedHash = seedHash
            )
        } else if (habit.lifecycleState == HabitLifecycleState.ACTIVE) {
            // Newly planted habit with 0 active streak days -> Stage 1 Seed Mound with tiny emerging cotyledon
            val terrainY = evaluateMeadowGroundNormY(clampedX, depthLayer = 2)
            plants += PaintedBotanicalPlant(
                id = "seed_${habit.habitId}",
                habitId = habit.habitId,
                habitName = habit.name,
                habitColor = parsedColor,
                runId = null,
                stage = BotanicalPaintingStage.SEED_MOUND,
                treeState = GroveTreeState.SAPLING_ACTIVE,
                completedStreakCount = 0,
                elapsedActiveSpanCount = 0,
                streakUnit = summary?.streakUnit ?: StreakUnit.DAYS,
                startLocalDate = null,
                endLocalDate = null,
                rootNormX = clampedX,
                rootNormY = terrainY,
                leafCenterNormX = clampedX,
                leafCenterNormY = (terrainY - 0.055f).coerceIn(0.25f, 0.90f),
                leafHitRadiusNorm = 0.07f,
                scaleFactor = 0.78f,
                depthLayer = 2,
                seedHash = seedHash
            )
        }
    }

    // 2. Preserved Dormant Grove Trees (closed runs with qualifiedForGrove && treeState == DORMANT_TREE)
    val dormantRuns = allRuns
        .filter { !it.isActive && it.qualifiedForGrove && it.treeState == GroveTreeState.DORMANT_TREE }
        .takeLast(8)

    val hillsideSlotsX = listOf(0.20f, 0.80f, 0.33f, 0.67f, 0.13f, 0.87f, 0.43f, 0.58f)
    dormantRuns.forEachIndexed { idx, run ->
        val habit = habits.find { it.habitId == run.habitId }
        val habitName = habit?.name ?: "Preserved Habit"
        val parsedColor = parseHabitBotanicalTint(habit?.colorHex)
        val normX = hillsideSlotsX[idx % hillsideSlotsX.size]
        val terrainY = evaluateMeadowGroundNormY(normX, depthLayer = 0)
        val span = run.elapsedActiveSpanCount.coerceAtLeast(7)
        val scale = (0.76f + (span.coerceAtMost(35) - 7) * 0.008f).coerceIn(0.76f, 0.98f)
        val seedHash = run.runId.fold(23) { acc, c -> acc * 31 + c.code }

        plants += PaintedBotanicalPlant(
            id = "dormant_${run.runId}",
            habitId = run.habitId,
            habitName = habitName,
            habitColor = parsedColor,
            runId = run.runId,
            stage = BotanicalPaintingStage.DORMANT_AUTUMN_TREE,
            treeState = GroveTreeState.DORMANT_TREE,
            completedStreakCount = run.completedStreakCount,
            elapsedActiveSpanCount = run.elapsedActiveSpanCount,
            streakUnit = run.streakUnit,
            startLocalDate = run.startLocalDate,
            endLocalDate = run.endLocalDate,
            rootNormX = normX,
            rootNormY = terrainY,
            leafCenterNormX = normX,
            leafCenterNormY = (terrainY - 0.19f * scale).coerceIn(0.18f, 0.78f),
            leafHitRadiusNorm = 0.095f,
            scaleFactor = scale,
            depthLayer = 0,
            seedHash = seedHash
        )
    }

    // Sort back-to-front by depthLayer and rootNormY so foreground saplings/trees overlap mid-ground naturally
    return plants.sortedWith(compareBy<PaintedBotanicalPlant> { it.depthLayer }.thenBy { it.rootNormY })
}

private fun evaluateMeadowGroundNormY(normX: Float, depthLayer: Int): Float {
    return when (depthLayer) {
        0 -> {
            // Distant-mid hillside crest
            0.63f - 0.035f * sin(normX * PI.toFloat() * 1.3f) + 0.015f * cos(normX * PI.toFloat() * 2.4f)
        }
        1 -> {
            // Mid-ground sunlit glade
            0.74f + 0.025f * cos((normX - 0.25f) * PI.toFloat() * 1.6f)
        }
        else -> {
            // Foreground meadow bank
            0.84f + 0.02f * sin((normX + 0.15f) * PI.toFloat() * 2.0f)
        }
    }
}

private fun parseHabitBotanicalTint(colorHex: String?): Color {
    if (colorHex.isNullOrBlank()) return Color(0xFF4A7C4B)
    return try {
        val cleaned = colorHex.trim().removePrefix("#")
        val longVal = cleaned.toLong(16)
        val raw = if (cleaned.length == 6) {
            Color(0xFF000000 or longVal)
        } else {
            Color(longVal)
        }
        // Harmonize with natural botanical chlorophyll green so every tree feels painted in the same world
        lerp(raw, Color(0xFF3E6E41), 0.45f)
    } catch (_: Exception) {
        Color(0xFF4A7C4B)
    }
}

/**
 * The primary evolving botanical landscape painting for the Living Grove screen.
 * Keeps the painting dominant and uncrowded; interactive controls appear ONLY when a leaf/plant is touched.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LivingGroveLandscapeSection(
    habits: List<Habit>,
    summaries: Map<String, HabitSummary>,
    allRuns: List<HabitStreakRun>,
    selectedHabitFilter: String?,
    touchedPlantId: String?,
    onTouchPlant: (String?) -> Unit,
    onSelectHabitFilter: (String?) -> Unit,
    onInspectHabit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val plants = remember(habits, summaries, allRuns) {
        buildBotanicalLandscapeSpecimens(habits, summaries, allRuns)
    }

    val touchedPlant = remember(plants, touchedPlantId) {
        plants.find { it.id == touchedPlantId }
    }

    val stageSummaryLabel = remember(plants, habits) {
        val growingCount = plants.count {
            it.stage == BotanicalPaintingStage.ESTABLISHED_TREE || it.stage == BotanicalPaintingStage.MATURE_WOODLAND_TREE
        }
        val saplingCount = plants.count { it.stage == BotanicalPaintingStage.EARLY_SAPLING }
        val dormantCount = plants.count { it.stage == BotanicalPaintingStage.DORMANT_AUTUMN_TREE }
        val seedCount = plants.count { it.stage == BotanicalPaintingStage.SEED_MOUND }
        val saplingWord = if (saplingCount == 1) "sapling" else "saplings"
        val dormantWord = if (dormantCount == 1) "autumn tree" else "autumn trees"
        when {
            plants.isEmpty() -> "Quiet Meadow Glade — Waiting for your first habit seed"
            growingCount >= 2 -> "Thriving Woodland Sanctuary — $growingCount growing trees, $saplingCount $saplingWord, $dormantCount $dormantWord"
            growingCount == 1 -> "Established Glade — 1 growing tree rooted in your Grove"
            saplingCount > 0 -> "Sprouting Meadow — $saplingCount active $saplingWord reaching toward the sun"
            seedCount > 0 -> "Freshly Planted Meadow — $seedCount ${if (seedCount == 1) "seed" else "seeds"} resting in warm soil"
            else -> "Autumn Woodland Memory — $dormantCount preserved $dormantWord"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Framed Botanical Storybook Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grove_botanical_painting_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF16211A) else Color(0xFFF7F1E1)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isDark) Color(0xFF384D3D) else Color(0xFFC8BC9E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .testTag("grove_botanical_painting_canvas")
                        .semantics {
                            contentDescription = "Botanical Grove Landscape Painting: $stageSummaryLabel"
                        }
                ) {
                    val density = LocalDensity.current
                    val canvasWidthPx = with(density) { maxWidth.toPx() }
                    val canvasHeightPx = with(density) { maxHeight.toPx() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(plants, touchedPlantId) {
                                detectTapGestures { tapOffset ->
                                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                    val normX = tapOffset.x / size.width.toFloat()
                                    val normY = tapOffset.y / size.height.toFloat()

                                    // Find the closest plant whose leaf/canopy or trunk was touched
                                    val hit = plants
                                        .reversed() // Foreground plants take priority
                                        .minByOrNull { plant ->
                                            hypot(normX - plant.leafCenterNormX, normY - plant.leafCenterNormY)
                                        }
                                        ?.takeIf { plant ->
                                            val leafDist = hypot(
                                                normX - plant.leafCenterNormX,
                                                normY - plant.leafCenterNormY
                                            )
                                            val trunkDist = hypot(
                                                normX - plant.rootNormX,
                                                normY - ((plant.rootNormY + plant.leafCenterNormY) * 0.5f)
                                            )
                                            leafDist <= plant.leafHitRadiusNorm * 1.25f ||
                                                trunkDist <= plant.leafHitRadiusNorm * 0.9f
                                        }

                                    if (hit != null) {
                                        onTouchPlant(if (touchedPlantId == hit.id) null else hit.id)
                                    } else {
                                        onTouchPlant(null)
                                    }
                                }
                            }
                    ) {
                        drawBotanicalStorybookLandscape(
                            plants = plants,
                            touchedPlantId = touchedPlantId,
                            selectedHabitFilter = selectedHabitFilter,
                            isDark = isDark
                        )
                    }

                    // Accessible minimum 48dp leaf touch targets for each plant in the painting
                    plants.forEach { plant ->
                        val targetSizeDp = 48.dp
                        val targetSizePx = with(density) { targetSizeDp.toPx() }
                        val offsetX = (plant.leafCenterNormX * canvasWidthPx - targetSizePx * 0.5f)
                            .roundToInt()
                            .coerceIn(0, (canvasWidthPx - targetSizePx).roundToInt().coerceAtLeast(0))
                        val offsetY = (plant.leafCenterNormY * canvasHeightPx - targetSizePx * 0.5f)
                            .roundToInt()
                            .coerceIn(0, (canvasHeightPx - targetSizePx).roundToInt().coerceAtLeast(0))

                        val stageDesc = when (plant.stage) {
                            BotanicalPaintingStage.SEED_MOUND -> "newly planted seed"
                            BotanicalPaintingStage.EARLY_SAPLING -> "active sapling, ${plant.elapsedActiveSpanCount} active span days"
                            BotanicalPaintingStage.ESTABLISHED_TREE -> "growing tree, ${plant.elapsedActiveSpanCount} active span days"
                            BotanicalPaintingStage.MATURE_WOODLAND_TREE -> "mature woodland tree, ${plant.elapsedActiveSpanCount} active span days"
                            BotanicalPaintingStage.DORMANT_AUTUMN_TREE -> "preserved dormant autumn tree, ${plant.elapsedActiveSpanCount} active span days"
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY) }
                                .size(targetSizeDp)
                                .clip(CircleShape)
                                .clickable(
                                    onClickLabel = "Inspect ${plant.habitName} leaves"
                                ) {
                                    onTouchPlant(if (touchedPlantId == plant.id) null else plant.id)
                                }
                                .testTag("grove_leaf_target_${plant.habitId}" + if (plant.stage == BotanicalPaintingStage.DORMANT_AUTUMN_TREE) "_${plant.runId}" else "")
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Touch leaves of ${plant.habitName} ($stageDesc)"
                                }
                        )
                    }
                }

                // Subtle storybook plate caption bar at the bottom of the frame
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF1A2820) else Color(0xFFF1E9D4)
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stageSummaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = if (isDark) Color(0xFFD5E0D4) else Color(0xFF3E4D3B),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grove_painting_stage_caption")
                    )
                    if (plants.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (touchedPlant == null) "Touch a leaf to inspect" else "Leaf selected",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Contextual Painting Controls — ONLY visible when a corresponding leaf/plant in the painting is touched
        if (touchedPlant != null) {
            TouchedLeafControlsCard(
                plant = touchedPlant,
                habits = habits,
                selectedHabitFilter = selectedHabitFilter,
                onSelectHabitFilter = onSelectHabitFilter,
                onInspectHabit = { onInspectHabit(touchedPlant.habitId) },
                onDismiss = { onTouchPlant(null) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TouchedLeafControlsCard(
    plant: PaintedBotanicalPlant,
    habits: List<Habit>,
    selectedHabitFilter: String?,
    onSelectHabitFilter: (String?) -> Unit,
    onInspectHabit: () -> Unit,
    onDismiss: () -> Unit
) {
    val unitLabel = if (plant.streakUnit == StreakUnit.WEEKS) {
        if (plant.completedStreakCount == 1) "week" else "weeks"
    } else {
        if (plant.completedStreakCount == 1) "day" else "days"
    }

    val stageBadge = when (plant.stage) {
        BotanicalPaintingStage.SEED_MOUND -> "Freshly Planted Seed • Ready for first check-in"
        BotanicalPaintingStage.EARLY_SAPLING -> "Active Sapling • ${plant.completedStreakCount} $unitLabel (${plant.elapsedActiveSpanCount}d active span)"
        BotanicalPaintingStage.ESTABLISHED_TREE -> "Growing Tree • ${plant.completedStreakCount} $unitLabel (${plant.elapsedActiveSpanCount}d active span)"
        BotanicalPaintingStage.MATURE_WOODLAND_TREE -> "Mature Woodland Tree • ${plant.completedStreakCount} $unitLabel (${plant.elapsedActiveSpanCount}d active span)"
        BotanicalPaintingStage.DORMANT_AUTUMN_TREE -> "Preserved Dormant Tree • ${plant.completedStreakCount} $unitLabel (${plant.elapsedActiveSpanCount}d span)"
    }

    val stageNextHint = when (plant.stage) {
        BotanicalPaintingStage.SEED_MOUND ->
            "Tend this habit today to sprout its first sapling stem and leaves."
        BotanicalPaintingStage.EARLY_SAPLING -> {
            val remaining = (7 - plant.elapsedActiveSpanCount).coerceAtLeast(1)
            "$remaining more active-span ${if (remaining == 1) "day" else "days"} until this sapling roots as a permanent Grove tree."
        }
        BotanicalPaintingStage.ESTABLISHED_TREE ->
            "Rooted permanently in your Grove. Continues adding boughs and sunlit foliage as your streak grows."
        BotanicalPaintingStage.MATURE_WOODLAND_TREE ->
            "A deep-rooted canopy anchor of your Grove (${plant.startLocalDate ?: ""} – ${plant.endLocalDate ?: "present"})."
        BotanicalPaintingStage.DORMANT_AUTUMN_TREE ->
            "Preserved forever in autumn gold (${plant.startLocalDate ?: ""} – ${plant.endLocalDate ?: ""})."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grove_touched_plant_controls"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = when (plant.stage) {
                            BotanicalPaintingStage.ESTABLISHED_TREE,
                            BotanicalPaintingStage.MATURE_WOODLAND_TREE -> Icons.Default.Forest
                            BotanicalPaintingStage.DORMANT_AUTUMN_TREE -> Icons.Default.Eco
                            else -> Icons.Default.Spa
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = plant.habitName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stageBadge,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("grove_touched_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hide plant controls"
                    )
                }
            }

            Text(
                text = stageNextHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Habit filter chips & Inspect action revealed on leaf touch
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedHabitFilter == null,
                    onClick = { onSelectHabitFilter(null) },
                    label = { Text("All Habits") },
                    modifier = Modifier.testTag("grove_filter_all")
                )
                habits.forEach { habit ->
                    FilterChip(
                        selected = selectedHabitFilter == habit.habitId,
                        onClick = {
                            onSelectHabitFilter(
                                if (selectedHabitFilter == habit.habitId) null else habit.habitId
                            )
                        },
                        label = { Text(habit.name) },
                        leadingIcon = if (habit.habitId == plant.habitId) {
                            {
                                Icon(
                                    imageVector = Icons.Default.FilterCenterFocus,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("grove_filter_${habit.habitId}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onInspectHabit,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("grove_touched_inspect_btn")
                ) {
                    Text("Inspect Habit Details")
                }
            }
        }
    }
}

/**
 * Master Canvas drawing routine for the botanical storybook landscape painting.
 */
internal fun DrawScope.drawBotanicalStorybookLandscape(
    plants: List<PaintedBotanicalPlant>,
    touchedPlantId: String?,
    selectedHabitFilter: String?,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    // 1. Atmospheric Sky Wash
    val skyColors = if (isDark) {
        listOf(
            Color(0xFF0F1813),
            Color(0xFF17251D),
            Color(0xFF233529),
            Color(0xFF2D4132)
        )
    } else {
        listOf(
            Color(0xFFFBF6E9),
            Color(0xFFF3EBD4),
            Color(0xFFE7DEC2),
            Color(0xFFDAD4B8)
        )
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = skyColors,
            startY = 0f,
            endY = h * 0.68f
        ),
        size = Size(w, h)
    )

    // 2. Warm Sun / Moon Glow & Crepuscular Sunbeams
    val sunCenter = Offset(w * 0.22f, h * 0.17f)
    val sunGlowColor = if (isDark) Color(0xFFE8D18B) else Color(0xFFFFF4C8)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                sunGlowColor.copy(alpha = if (isDark) 0.26f else 0.65f),
                sunGlowColor.copy(alpha = if (isDark) 0.10f else 0.24f),
                Color.Transparent
            ),
            center = sunCenter,
            radius = w * 0.48f
        ),
        radius = w * 0.48f,
        center = sunCenter
    )
    drawCircle(
        color = if (isDark) Color(0xFFF2E2AA).copy(alpha = 0.78f) else Color(0xFFFFFBEB).copy(alpha = 0.92f),
        radius = min(w, h) * 0.055f,
        center = sunCenter
    )

    // Diagonal sunbeams slanting across the valley
    val rayAngles = listOf(0.32f, 0.52f, 0.74f)
    rayAngles.forEach { targetNormX ->
        val rayPath = Path().apply {
            moveTo(sunCenter.x - w * 0.03f, sunCenter.y)
            lineTo(sunCenter.x + w * 0.04f, sunCenter.y)
            lineTo(w * (targetNormX + 0.14f), h * 0.75f)
            lineTo(w * (targetNormX - 0.08f), h * 0.75f)
            close()
        }
        drawPath(
            path = rayPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    sunGlowColor.copy(alpha = if (isDark) 0.05f else 0.13f),
                    Color.Transparent
                ),
                startY = sunCenter.y,
                endY = h * 0.75f
            )
        )
    }

    // Soft painted cloud ribbons
    drawStorybookCloudWash(
        centerX = w * 0.68f,
        centerY = h * 0.14f,
        cloudWidth = w * 0.34f,
        cloudHeight = h * 0.065f,
        color = if (isDark) Color(0xFF33493B).copy(alpha = 0.35f) else Color(0xFFFFFCF4).copy(alpha = 0.65f)
    )
    drawStorybookCloudWash(
        centerX = w * 0.42f,
        centerY = h * 0.22f,
        cloudWidth = w * 0.26f,
        cloudHeight = h * 0.048f,
        color = if (isDark) Color(0xFF2D4235).copy(alpha = 0.28f) else Color(0xFFFFFDF7).copy(alpha = 0.52f)
    )

    // 3. Distant Mountain / Woodland Ridge (Horizon Layer)
    val distantRidgePath = Path().apply {
        moveTo(0f, h * 0.53f)
        cubicTo(w * 0.18f, h * 0.44f, w * 0.36f, h * 0.46f, w * 0.54f, h * 0.50f)
        cubicTo(w * 0.72f, h * 0.54f, w * 0.86f, h * 0.43f, w, h * 0.47f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = distantRidgePath,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF24372B), Color(0xFF1D2E23))
            } else {
                listOf(Color(0xFF9CB19C), Color(0xFF839B85))
            },
            startY = h * 0.43f,
            endY = h * 0.70f
        )
    )

    // Distant woodland silhouettes along the far ridge crest for organic storybook depth
    drawDistantTreelineSilhouette(
        startX = w * 0.02f,
        endX = w * 0.46f,
        baseY = h * 0.485f,
        isLeftSlope = true,
        colorBack = if (isDark) Color(0xFF1A2A20) else Color(0xFF7A947E),
        colorFront = if (isDark) Color(0xFF213428) else Color(0xFF6A866E)
    )
    drawDistantTreelineSilhouette(
        startX = w * 0.60f,
        endX = w * 0.98f,
        baseY = h * 0.475f,
        isLeftSlope = false,
        colorBack = if (isDark) Color(0xFF1A2A20) else Color(0xFF7E9881),
        colorFront = if (isDark) Color(0xFF213428) else Color(0xFF6D8971)
    )

    // Valley Mist Ribbon floating between distant ridge and mid-ground hillside
    val mistPath = Path().apply {
        moveTo(0f, h * 0.54f)
        cubicTo(w * 0.30f, h * 0.50f, w * 0.65f, h * 0.57f, w, h * 0.51f)
        lineTo(w, h * 0.66f)
        lineTo(0f, h * 0.66f)
        close()
    }
    drawPath(
        path = mistPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color(0xFF395243).copy(alpha = 0.28f) else Color(0xFFF9F4E6).copy(alpha = 0.62f),
                Color.Transparent
            ),
            startY = h * 0.50f,
            endY = h * 0.66f
        )
    )

    // 4. Mid-Ground Sunlit Rolling Hills (Left & Right Overlapping Slopes)
    val midHillBackRight = Path().apply {
        moveTo(w * 0.28f, h * 0.65f)
        cubicTo(w * 0.52f, h * 0.56f, w * 0.78f, h * 0.55f, w, h * 0.60f)
        lineTo(w, h)
        lineTo(w * 0.28f, h)
        close()
    }
    drawPath(
        path = midHillBackRight,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF28402E), Color(0xFF1E3123))
            } else {
                listOf(Color(0xFF94B07B), Color(0xFF6F8E5A))
            },
            startY = h * 0.55f,
            endY = h * 0.82f
        )
    )

    val midHillPath = Path().apply {
        moveTo(0f, h * 0.61f)
        cubicTo(w * 0.22f, h * 0.56f, w * 0.48f, h * 0.61f, w * 0.76f, h * 0.65f)
        cubicTo(w * 0.88f, h * 0.66f, w * 0.95f, h * 0.63f, w, h * 0.64f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = midHillPath,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF2E4733), Color(0xFF213526))
            } else {
                listOf(Color(0xFF8AA870), Color(0xFF658450))
            },
            startY = h * 0.56f,
            endY = h * 0.85f
        )
    )

    // Sunlit golden-sage rim highlight along the hill crests
    val hillContour1 = Path().apply {
        moveTo(0f, h * 0.612f)
        cubicTo(w * 0.22f, h * 0.562f, w * 0.46f, h * 0.608f, w * 0.68f, h * 0.642f)
    }
    drawPath(
        path = hillContour1,
        color = if (isDark) Color(0xFF496D4E).copy(alpha = 0.45f) else Color(0xFFC6DC98).copy(alpha = 0.65f),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // Render Depth Layer 0 Plants (Distant/Hillside Dormant Trees)
    plants.filter { it.depthLayer == 0 }.forEach { plant ->
        drawSingleBotanicalSpecimen(
            plant = plant,
            isTouched = plant.id == touchedPlantId,
            isDeemphasized = selectedHabitFilter != null && selectedHabitFilter != plant.habitId,
            isDark = isDark
        )
    }

    // 5. Mid-Foreground Glade Slope & Winding Earthen Storybook Path
    val midGladePath = Path().apply {
        moveTo(0f, h * 0.74f)
        cubicTo(w * 0.28f, h * 0.69f, w * 0.62f, h * 0.72f, w, h * 0.76f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = midGladePath,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF263E2B), Color(0xFF1B2D1F))
            } else {
                listOf(Color(0xFF76985D), Color(0xFF557540))
            },
            startY = h * 0.69f,
            endY = h
        )
    )

    // Sunlit glade patch in the center of the meadow
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                if (isDark) Color(0xFF38593E).copy(alpha = 0.35f) else Color(0xFFB7D182).copy(alpha = 0.42f),
                Color.Transparent
            ),
            center = Offset(w * 0.48f, h * 0.78f),
            radius = w * 0.42f
        ),
        topLeft = Offset(w * 0.08f, h * 0.68f),
        size = Size(w * 0.80f, h * 0.22f)
    )

    // Winding Earthen & Sandstone Footpath inviting the viewer into the Grove
    drawWindingEarthenFootpath(isDark = isDark)

    // Render Depth Layer 1 Plants (Mid-ground Established & Mature Growing Trees)
    plants.filter { it.depthLayer == 1 }.forEach { plant ->
        drawSingleBotanicalSpecimen(
            plant = plant,
            isTouched = plant.id == touchedPlantId,
            isDeemphasized = selectedHabitFilter != null && selectedHabitFilter != plant.habitId,
            isDark = isDark
        )
    }

    // 6. Rich Foreground Meadow Bank
    val foregroundMeadowPath = Path().apply {
        moveTo(0f, h * 0.84f)
        cubicTo(w * 0.24f, h * 0.80f, w * 0.55f, h * 0.86f, w, h * 0.81f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = foregroundMeadowPath,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF1F3424), Color(0xFF142217))
            } else {
                listOf(Color(0xFF5B7D45), Color(0xFF3E592E))
            },
            startY = h * 0.80f,
            endY = h
        )
    )

    // If completely empty (0 habits yet), paint a subtle fertile soil clearing with two tiny wild clover leaves
    if (plants.isEmpty()) {
        drawWaitingFertileSoilPatch(
            centerX = w * 0.50f,
            centerY = h * 0.83f,
            isDark = isDark
        )
    }

    // Foreground botanical fern fronds, pointed meadow leaves, mossy stones & wildflowers
    drawMeadowBotanicalGroundcover(
        hasGrowingTrees = plants.any {
            it.stage == BotanicalPaintingStage.ESTABLISHED_TREE ||
                it.stage == BotanicalPaintingStage.MATURE_WOODLAND_TREE
        },
        hasAnyPlants = plants.isNotEmpty(),
        isDark = isDark
    )

    // Render Depth Layer 2 Plants (Foreground Saplings & Seed Mounds)
    plants.filter { it.depthLayer == 2 }.forEach { plant ->
        drawSingleBotanicalSpecimen(
            plant = plant,
            isTouched = plant.id == touchedPlantId,
            isDeemphasized = selectedHabitFilter != null && selectedHabitFilter != plant.habitId,
            isDark = isDark
        )
    }
}

private fun DrawScope.drawStorybookCloudWash(
    centerX: Float,
    centerY: Float,
    cloudWidth: Float,
    cloudHeight: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(centerX - cloudWidth * 0.5f, centerY + cloudHeight * 0.3f)
        cubicTo(
            centerX - cloudWidth * 0.38f, centerY - cloudHeight * 0.45f,
            centerX - cloudWidth * 0.12f, centerY - cloudHeight * 0.55f,
            centerX + cloudWidth * 0.05f, centerY - cloudHeight * 0.25f
        )
        cubicTo(
            centerX + cloudWidth * 0.22f, centerY - cloudHeight * 0.60f,
            centerX + cloudWidth * 0.42f, centerY - cloudHeight * 0.25f,
            centerX + cloudWidth * 0.5f, centerY + cloudHeight * 0.3f
        )
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

private fun DrawScope.drawDistantTreelineSilhouette(
    startX: Float,
    endX: Float,
    baseY: Float,
    isLeftSlope: Boolean,
    colorBack: Color,
    colorFront: Color
) {
    val span = endX - startX
    val canopyCenters = listOf(0.08f, 0.19f, 0.31f, 0.44f, 0.58f, 0.71f, 0.84f, 0.94f)
    val heights = listOf(11f, 16f, 13f, 18f, 14f, 17f, 12f, 9f)

    // Back atmospheric woodland layer
    canopyCenters.forEachIndexed { idx, norm ->
        val cx = startX + span * norm
        val slopeOffset = if (isLeftSlope) (norm - 0.3f) * 8.dp.toPx() else (0.6f - norm) * 8.dp.toPx()
        val cy = baseY + slopeOffset
        val rx = (12f + (idx % 3) * 4f).dp.toPx()
        val ry = heights[idx % heights.size].dp.toPx()
        drawScallopedFoliageMass(
            centerX = cx,
            centerY = cy - ry * 0.35f,
            radiusX = rx,
            radiusY = ry * 0.75f,
            color = colorBack.copy(alpha = 0.65f),
            lobeCount = 7,
            seed = idx * 29 + 11
        )
    }

    // Front varied woodland silhouettes
    canopyCenters.forEachIndexed { idx, norm ->
        if (idx % 2 == 0) {
            val cx = startX + span * (norm + 0.04f)
            val slopeOffset = if (isLeftSlope) (norm - 0.3f) * 8.dp.toPx() else (0.6f - norm) * 8.dp.toPx()
            val cy = baseY + slopeOffset + 2.dp.toPx()
            val rx = (11f + (idx % 2) * 4f).dp.toPx()
            val ry = (heights[(idx + 1) % heights.size] * 0.85f).dp.toPx()
            drawScallopedFoliageMass(
                centerX = cx,
                centerY = cy - ry * 0.3f,
                radiusX = rx,
                radiusY = ry * 0.7f,
                color = colorFront.copy(alpha = 0.72f),
                lobeCount = 8,
                seed = idx * 41 + 7
            )
        }
    }
}

private fun DrawScope.drawWindingEarthenFootpath(isDark: Boolean) {
    val w = size.width
    val h = size.height

    val pathFill = Path().apply {
        moveTo(w * 0.45f, h * 0.63f)
        cubicTo(w * 0.37f, h * 0.71f, w * 0.56f, h * 0.79f, w * 0.36f, h)
        lineTo(w * 0.58f, h)
        cubicTo(w * 0.69f, h * 0.79f, w * 0.46f, h * 0.71f, w * 0.48f, h * 0.63f)
        close()
    }
    drawPath(
        path = pathFill,
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF3B3529).copy(alpha = 0.55f), Color(0xFF4E4334).copy(alpha = 0.85f))
            } else {
                listOf(Color(0xFFD6C6A1).copy(alpha = 0.72f), Color(0xFFC0A77B).copy(alpha = 0.90f))
            },
            startY = h * 0.63f,
            endY = h
        )
    )

    // Soft grassy verge & sunlit sandstone rim along the winding path
    val leftEdge = Path().apply {
        moveTo(w * 0.45f, h * 0.63f)
        cubicTo(w * 0.37f, h * 0.71f, w * 0.56f, h * 0.79f, w * 0.36f, h)
    }
    drawPath(
        path = leftEdge,
        color = if (isDark) Color(0xFF615442).copy(alpha = 0.45f) else Color(0xFFE9DEC0).copy(alpha = 0.70f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Flat weathered stepping stones nested into the path
    val stones = listOf(
        Triple(0.45f, 0.69f, 0.018f),
        Triple(0.49f, 0.76f, 0.024f),
        Triple(0.51f, 0.85f, 0.029f),
        Triple(0.46f, 0.93f, 0.034f)
    )
    stones.forEach { (sx, sy, sr) ->
        drawOval(
            color = if (isDark) Color(0xFF5A5145) else Color(0xFFB19E7C),
            topLeft = Offset(w * (sx - sr), h * (sy - sr * 0.32f)),
            size = Size(w * sr * 2f, h * sr * 0.64f)
        )
    }
}

private fun DrawScope.drawWaitingFertileSoilPatch(
    centerX: Float,
    centerY: Float,
    isDark: Boolean
) {
    val moundW = 46.dp.toPx()
    val moundH = 11.dp.toPx()
    val soilDark = if (isDark) Color(0xFF3B2D21) else Color(0xFF6A4E36)
    val soilWarm = if (isDark) Color(0xFF4E3B2B) else Color(0xFF876547)

    // Organic sculpted soil bed
    val bedPath = Path().apply {
        moveTo(centerX - moundW * 0.5f, centerY + moundH * 0.15f)
        cubicTo(
            centerX - moundW * 0.35f, centerY - moundH * 0.75f,
            centerX + moundW * 0.35f, centerY - moundH * 0.75f,
            centerX + moundW * 0.5f, centerY + moundH * 0.15f
        )
        cubicTo(
            centerX + moundW * 0.25f, centerY + moundH * 0.55f,
            centerX - moundW * 0.25f, centerY + moundH * 0.55f,
            centerX - moundW * 0.5f, centerY + moundH * 0.15f
        )
        close()
    }
    drawPath(path = bedPath, color = soilDark)
    drawOval(
        color = soilWarm,
        topLeft = Offset(centerX - moundW * 0.32f, centerY - moundH * 0.35f),
        size = Size(moundW * 0.64f, moundH * 0.55f)
    )
    // Two tiny wild meadow leaves beside the soil bed
    drawPointedBotanicalLeaf(
        baseX = centerX - moundW * 0.44f,
        baseY = centerY,
        length = 7.dp.toPx(),
        width = 3.2.dp.toPx(),
        angleDegrees = -125f,
        baseColor = Color(0xFF4E7D3E),
        highlightColor = Color(0xFF88B662),
        veinColor = null
    )
    drawPointedBotanicalLeaf(
        baseX = centerX + moundW * 0.44f,
        baseY = centerY,
        length = 6.5.dp.toPx(),
        width = 3.0.dp.toPx(),
        angleDegrees = -50f,
        baseColor = Color(0xFF4E7D3E),
        highlightColor = Color(0xFF88B662),
        veinColor = null
    )
}

private fun DrawScope.drawMeadowBotanicalGroundcover(
    hasGrowingTrees: Boolean,
    hasAnyPlants: Boolean,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    val leafDark = if (isDark) Color(0xFF28462D) else Color(0xFF395B2B)
    val leafMid = if (isDark) Color(0xFF39603E) else Color(0xFF557D3E)
    val leafSunlit = if (isDark) Color(0xFF538057) else Color(0xFF8BB35E)

    // Mossy foreground stones framing the left and right edges of the meadow
    val stoneSpots = listOf(0.11f to 0.89f, 0.88f to 0.87f)
    stoneSpots.forEach { (sx, sy) ->
        val cx = w * sx
        val cy = h * sy
        val sw = 24.dp.toPx()
        val sh = 10.dp.toPx()
        drawOval(
            color = if (isDark) Color(0xFF3D443B) else Color(0xFF8B8E7E),
            topLeft = Offset(cx - sw * 0.5f, cy - sh * 0.5f),
            size = Size(sw, sh)
        )
        // Moss cap on the stone
        drawScallopedFoliageMass(
            centerX = cx - sw * 0.08f,
            centerY = cy - sh * 0.28f,
            radiusX = sw * 0.38f,
            radiusY = sh * 0.34f,
            color = leafMid,
            lobeCount = 6,
            seed = (sx * 100).toInt()
        )
    }

    // Painterly botanical fern & leaf clusters across the meadow (using real pointed botanical leaves + curved stems)
    val floraClusters = listOf(
        0.08f to 0.83f, 0.19f to 0.91f, 0.31f to 0.86f,
        0.66f to 0.87f, 0.78f to 0.92f, 0.91f to 0.84f,
        0.15f to 0.73f, 0.84f to 0.74f
    )
    floraClusters.forEachIndexed { idx, (nx, ny) ->
        val cx = w * nx
        val cy = h * ny
        val angles = listOf(-145f, -110f, -72f, -38f)
        angles.forEachIndexed { aIdx, angle ->
            val len = (7.5f + ((idx + aIdx) % 3) * 2.2f).dp.toPx()
            drawPointedBotanicalLeaf(
                baseX = cx + (aIdx - 1.5f) * 2.2f.dp.toPx(),
                baseY = cy,
                length = len,
                width = len * 0.42f,
                angleDegrees = angle,
                baseColor = if (aIdx % 2 == 0) leafDark else leafMid,
                highlightColor = leafSunlit,
                veinColor = null
            )
        }
    }

    // Botanical wildflowers bloom across the meadow
    val flowerSpots = if (hasGrowingTrees) {
        listOf(
            0.13f to 0.86f, 0.24f to 0.90f, 0.34f to 0.85f,
            0.64f to 0.86f, 0.75f to 0.89f, 0.87f to 0.84f
        )
    } else if (hasAnyPlants) {
        listOf(0.22f to 0.89f, 0.74f to 0.87f, 0.86f to 0.84f)
    } else {
        listOf(0.20f to 0.89f, 0.79f to 0.86f)
    }
    val petalColors = listOf(
        Color(0xFFF6E394),
        Color(0xFFEFA878),
        Color(0xFFE8C5D8)
    )
    flowerSpots.forEachIndexed { idx, (fx, fy) ->
        val cx = w * fx
        val cy = h * fy
        val petalColor = petalColors[idx % petalColors.size]
        val petalR = 2.5.dp.toPx()
        for (p in 0 until 5) {
            val angle = (p * 72.0) * (PI / 180.0)
            drawCircle(
                color = petalColor,
                radius = petalR,
                center = Offset(
                    cx + cos(angle).toFloat() * petalR * 1.15f,
                    cy + sin(angle).toFloat() * petalR * 1.15f
                )
            )
        }
        drawCircle(
            color = Color(0xFFD68C29),
            radius = petalR * 0.65f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Renders an individual botanical plant specimen at its exact growth stage.
 */
private fun DrawScope.drawSingleBotanicalSpecimen(
    plant: PaintedBotanicalPlant,
    isTouched: Boolean,
    isDeemphasized: Boolean,
    isDark: Boolean
) {
    val alphaMultiplier = if (isDeemphasized) 0.38f else 1.0f
    val rootX = size.width * plant.rootNormX
    val rootY = size.height * plant.rootNormY

    // Soft warm sunlight halo when the user touches this plant's leaves
    if (isTouched) {
        val haloCenter = Offset(
            size.width * plant.leafCenterNormX,
            size.height * plant.leafCenterNormY
        )
        val haloRadius = size.width * plant.leafHitRadiusNorm * 1.35f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF3B8).copy(alpha = 0.52f),
                    Color(0xFFE3C468).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = haloCenter,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = haloCenter
        )
    }

    when (plant.stage) {
        BotanicalPaintingStage.SEED_MOUND -> {
            drawStage1SeedMound(
                rootX = rootX,
                rootY = rootY,
                scale = plant.scaleFactor,
                alpha = alphaMultiplier,
                isDark = isDark
            )
        }
        BotanicalPaintingStage.EARLY_SAPLING -> {
            drawStage2BotanicalSapling(
                rootX = rootX,
                rootY = rootY,
                spanDays = plant.elapsedActiveSpanCount,
                tintColor = plant.habitColor,
                scale = plant.scaleFactor,
                alpha = alphaMultiplier,
                seedHash = plant.seedHash,
                isDark = isDark
            )
        }
        BotanicalPaintingStage.ESTABLISHED_TREE -> {
            drawStage3Or4WoodlandTree(
                rootX = rootX,
                rootY = rootY,
                spanDays = plant.elapsedActiveSpanCount,
                isMature = false,
                tintColor = plant.habitColor,
                scale = plant.scaleFactor,
                alpha = alphaMultiplier,
                seedHash = plant.seedHash,
                isDark = isDark
            )
        }
        BotanicalPaintingStage.MATURE_WOODLAND_TREE -> {
            drawStage3Or4WoodlandTree(
                rootX = rootX,
                rootY = rootY,
                spanDays = plant.elapsedActiveSpanCount,
                isMature = true,
                tintColor = plant.habitColor,
                scale = plant.scaleFactor,
                alpha = alphaMultiplier,
                seedHash = plant.seedHash,
                isDark = isDark
            )
        }
        BotanicalPaintingStage.DORMANT_AUTUMN_TREE -> {
            drawStage5DormantAutumnTree(
                rootX = rootX,
                rootY = rootY,
                spanDays = plant.elapsedActiveSpanCount,
                scale = plant.scaleFactor,
                alpha = alphaMultiplier,
                seedHash = plant.seedHash,
                isDark = isDark
            )
        }
    }
}

private fun DrawScope.drawStage1SeedMound(
    rootX: Float,
    rootY: Float,
    scale: Float,
    alpha: Float,
    isDark: Boolean
) {
    val moundW = 36.dp.toPx() * scale
    val moundH = 11.dp.toPx() * scale

    // Ground shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        topLeft = Offset(rootX - moundW * 0.55f, rootY - moundH * 0.2f),
        size = Size(moundW * 1.1f, moundH * 0.85f)
    )
    // Earthen soil dome
    val moundPath = Path().apply {
        moveTo(rootX - moundW * 0.5f, rootY + moundH * 0.2f)
        cubicTo(
            rootX - moundW * 0.35f, rootY - moundH * 0.9f,
            rootX + moundW * 0.35f, rootY - moundH * 0.9f,
            rootX + moundW * 0.5f, rootY + moundH * 0.2f
        )
        close()
    }
    drawPath(
        path = moundPath,
        color = (if (isDark) Color(0xFF4B3828) else Color(0xFF725339)).copy(alpha = alpha)
    )
    // Tiny emerging pale-green cotyledon bud
    val stemTopY = rootY - 12.dp.toPx() * scale
    val stemPath = Path().apply {
        moveTo(rootX, rootY - 2.dp.toPx())
        quadraticTo(rootX - 2.dp.toPx(), rootY - 8.dp.toPx() * scale, rootX + 1.dp.toPx(), stemTopY)
    }
    drawPath(
        path = stemPath,
        color = Color(0xFF7CB35B).copy(alpha = alpha),
        style = Stroke(width = 2.2.dp.toPx() * scale, cap = StrokeCap.Round)
    )
    drawPointedBotanicalLeaf(
        baseX = rootX + 1.dp.toPx(),
        baseY = stemTopY,
        length = 9.dp.toPx() * scale,
        width = 4.8.dp.toPx() * scale,
        angleDegrees = -135f,
        baseColor = Color(0xFF6BA34D).copy(alpha = alpha),
        highlightColor = Color(0xFFA8D474).copy(alpha = alpha),
        veinColor = Color(0xFFD7EEAF).copy(alpha = 0.7f * alpha)
    )
    drawPointedBotanicalLeaf(
        baseX = rootX + 1.dp.toPx(),
        baseY = stemTopY,
        length = 8.dp.toPx() * scale,
        width = 4.4.dp.toPx() * scale,
        angleDegrees = -45f,
        baseColor = Color(0xFF5D9341).copy(alpha = alpha),
        highlightColor = Color(0xFF98C767).copy(alpha = alpha),
        veinColor = Color(0xFFD7EEAF).copy(alpha = 0.7f * alpha)
    )
}

/**
 * Stage 2: Active Sapling (1..6 elapsedActiveSpanCount).
 * Renders a supple S-curved stem rising from rich soil with 2..6 pointed, veined botanical leaves.
 */
private fun DrawScope.drawStage2BotanicalSapling(
    rootX: Float,
    rootY: Float,
    spanDays: Int,
    tintColor: Color,
    scale: Float,
    alpha: Float,
    seedHash: Int,
    isDark: Boolean
) {
    val clampedDays = spanDays.coerceIn(1, 6)
    val stemHeight = (34f + clampedDays * 6.5f).dp.toPx() * scale
    val curveDir = if (seedHash % 2 == 0) 1f else -1f

    // Root shadow & earthen mound
    val moundW = 30.dp.toPx() * scale
    val moundH = 8.dp.toPx() * scale
    drawOval(
        color = Color.Black.copy(alpha = 0.20f * alpha),
        topLeft = Offset(rootX - moundW * 0.55f, rootY - moundH * 0.3f),
        size = Size(moundW * 1.1f, moundH)
    )
    drawOval(
        color = (if (isDark) Color(0xFF473628) else Color(0xFF6B4F37)).copy(alpha = alpha),
        topLeft = Offset(rootX - moundW * 0.45f, rootY - moundH * 0.45f),
        size = Size(moundW * 0.9f, moundH * 0.8f)
    )

    val tipX = rootX + curveDir * 4.dp.toPx() * scale
    val tipY = rootY - stemHeight

    // Tapered organic stem
    val stemPath = Path().apply {
        moveTo(rootX - 2.2.dp.toPx() * scale, rootY)
        cubicTo(
            rootX - curveDir * 7.dp.toPx() * scale, rootY - stemHeight * 0.35f,
            rootX + curveDir * 8.dp.toPx() * scale, rootY - stemHeight * 0.70f,
            tipX, tipY
        )
        cubicTo(
            rootX + curveDir * 10.dp.toPx() * scale, rootY - stemHeight * 0.70f,
            rootX - curveDir * 4.dp.toPx() * scale, rootY - stemHeight * 0.35f,
            rootX + 2.2.dp.toPx() * scale, rootY
        )
        close()
    }
    drawPath(
        path = stemPath,
        color = Color(0xFF557941).copy(alpha = alpha)
    )
    // Sunlit stem ridge
    val stemHighlight = Path().apply {
        moveTo(rootX, rootY)
        cubicTo(
            rootX - curveDir * 5.dp.toPx() * scale, rootY - stemHeight * 0.35f,
            rootX + curveDir * 8.dp.toPx() * scale, rootY - stemHeight * 0.70f,
            tipX, tipY
        )
    }
    drawPath(
        path = stemHighlight,
        color = Color(0xFF8DB865).copy(alpha = 0.85f * alpha),
        style = Stroke(width = 1.4.dp.toPx() * scale, cap = StrokeCap.Round)
    )

    // Individual pointed ovate botanical leaves along the stem (2 to 6 leaves based on active span days)
    val leafCount = (clampedDays + 1).coerceIn(2, 6)
    val leafBaseColor = lerp(tintColor, Color(0xFF3E7840), 0.45f).copy(alpha = alpha)
    val leafHighlightColor = lerp(leafBaseColor, Color(0xFFA7D46F), 0.55f).copy(alpha = alpha)
    val veinColor = Color(0xFFDDF0B5).copy(alpha = 0.75f * alpha)

    for (i in 0 until leafCount) {
        val progress = 0.32f + (i.toFloat() / leafCount.toFloat()) * 0.64f
        val nodeY = rootY - stemHeight * progress
        val nodeX = rootX + sin(progress * PI.toFloat()) * curveDir * 3.dp.toPx() * scale
        val side = if (i % 2 == 0) -1f else 1f
        val leafLen = (13f + (i % 3) * 2.5f).dp.toPx() * scale
        val leafWid = leafLen * 0.46f
        val angle = if (side < 0f) -142f + i * 5f else -38f - i * 5f

        // Delicate petiole stalk connecting stem to leaf blade
        val petioleEnd = Offset(
            nodeX + side * 4.dp.toPx() * scale,
            nodeY - 2.dp.toPx() * scale
        )
        drawLine(
            color = Color(0xFF6A944E).copy(alpha = alpha),
            start = Offset(nodeX, nodeY),
            end = petioleEnd,
            strokeWidth = 1.3.dp.toPx() * scale,
            cap = StrokeCap.Round
        )

        drawPointedBotanicalLeaf(
            baseX = petioleEnd.x,
            baseY = petioleEnd.y,
            length = leafLen,
            width = leafWid,
            angleDegrees = angle,
            baseColor = leafBaseColor,
            highlightColor = leafHighlightColor,
            veinColor = veinColor
        )
    }

    // Apical bud leaf at the very crown tip
    drawPointedBotanicalLeaf(
        baseX = tipX,
        baseY = tipY,
        length = 12.dp.toPx() * scale,
        width = 5.4.dp.toPx() * scale,
        angleDegrees = -88f + curveDir * 8f,
        baseColor = leafBaseColor,
        highlightColor = Color(0xFFB7E07B).copy(alpha = alpha),
        veinColor = veinColor
    )
}

/**
 * Stage 3 (Established Young Tree, 7..13d) and Stage 4 (Mature Woodland Tree, 14..60+d).
 * Renders a sculpted tapered trunk with root buttresses, spreading primary/secondary boughs,
 * bark texture ridges, scalloped painterly foliage masses, and 60..130 pointed botanical leaves.
 */
private fun DrawScope.drawStage3Or4WoodlandTree(
    rootX: Float,
    rootY: Float,
    spanDays: Int,
    isMature: Boolean,
    tintColor: Color,
    scale: Float,
    alpha: Float,
    seedHash: Int,
    isDark: Boolean
) {
    val trunkHeight = (if (isMature) 108.dp.toPx() else 84.dp.toPx()) * scale
    val trunkBaseHalfW = (if (isMature) 13.dp.toPx() else 8.5.dp.toPx()) * scale
    val trunkTopHalfW = (if (isMature) 5.5.dp.toPx() else 3.8.dp.toPx()) * scale
    val lean = ((seedHash % 5) - 2) * 1.8.dp.toPx() * scale
    val forkX = rootX + lean
    val forkY = rootY - trunkHeight * 0.56f
    val crownCenterX = rootX + lean * 1.2f
    val crownCenterY = rootY - trunkHeight * 0.80f

    // 1. Dappled Ground Shadow beneath the tree canopy
    val shadowW = (if (isMature) 118.dp.toPx() else 82.dp.toPx()) * scale
    val shadowH = (if (isMature) 22.dp.toPx() else 16.dp.toPx()) * scale
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = (if (isDark) 0.36f else 0.24f) * alpha),
                Color.Transparent
            ),
            center = Offset(rootX, rootY + 2.dp.toPx()),
            radius = shadowW * 0.55f
        ),
        topLeft = Offset(rootX - shadowW * 0.5f, rootY - shadowH * 0.45f),
        size = Size(shadowW, shadowH)
    )

    // 2. Deep Interior Shadow Canopy Boughs (behind the upper branches)
    val deepShadowLeafColor = lerp(tintColor, Color(0xFF1A311D), 0.72f).copy(alpha = alpha)
    val midEmeraldColor = lerp(tintColor, Color(0xFF2D5631), 0.48f).copy(alpha = alpha)
    val sunlitCanopyColor = lerp(tintColor, Color(0xFF6FA14B), 0.52f).copy(alpha = alpha)
    val apicalGoldGreen = lerp(sunlitCanopyColor, Color(0xFFB5D66C), 0.55f).copy(alpha = alpha)

    val crownW = (if (isMature) 116.dp.toPx() else 84.dp.toPx()) * scale
    val crownH = (if (isMature) 86.dp.toPx() else 64.dp.toPx()) * scale

    // Back shadow foliage masses
    val backBoughOffsets = listOf(
        Triple(-0.32f, 0.08f, 0.42f),
        Triple(0.32f, 0.10f, 0.40f),
        Triple(0.0f, -0.18f, 0.46f),
        Triple(-0.18f, -0.08f, 0.38f),
        Triple(0.20f, -0.06f, 0.38f)
    )
    backBoughOffsets.forEachIndexed { idx, (ox, oy, rScale) ->
        drawScallopedFoliageMass(
            centerX = crownCenterX + crownW * ox,
            centerY = crownCenterY + crownH * oy,
            radiusX = crownW * rScale * 0.62f,
            radiusY = crownH * rScale * 0.54f,
            color = deepShadowLeafColor,
            lobeCount = 9 + (idx % 3),
            seed = seedHash + idx * 19
        )
    }

    // 3. Sculpted Tapered Trunk, Root Buttresses & Spreading Boughs
    val barkDark = (if (isDark) Color(0xFF38271B) else Color(0xFF4B3524)).copy(alpha = alpha)
    val barkMid = (if (isDark) Color(0xFF523928) else Color(0xFF6B4C35)).copy(alpha = alpha)
    val barkSunlit = (if (isDark) Color(0xFF7B5B40) else Color(0xFF9A7450)).copy(alpha = alpha)

    val trunkPath = Path().apply {
        // Left flared root buttress
        moveTo(rootX - trunkBaseHalfW * 1.65f, rootY + 3.dp.toPx() * scale)
        quadraticTo(
            rootX - trunkBaseHalfW * 0.85f, rootY - trunkHeight * 0.14f,
            forkX - trunkTopHalfW, forkY
        )
        lineTo(forkX + trunkTopHalfW, forkY)
        quadraticTo(
            rootX + trunkBaseHalfW * 0.85f, rootY - trunkHeight * 0.14f,
            rootX + trunkBaseHalfW * 1.70f, rootY + 3.dp.toPx() * scale
        )
        // Root flare notches at the ground
        quadraticTo(
            rootX + trunkBaseHalfW * 0.4f, rootY - 1.dp.toPx(),
            rootX, rootY + 4.dp.toPx() * scale
        )
        quadraticTo(
            rootX - trunkBaseHalfW * 0.4f, rootY - 1.dp.toPx(),
            rootX - trunkBaseHalfW * 1.65f, rootY + 3.dp.toPx() * scale
        )
        close()
    }
    drawPath(path = trunkPath, color = barkDark)

    // Sunlit left/center bark face
    val sunlitTrunkPath = Path().apply {
        moveTo(rootX - trunkBaseHalfW * 1.35f, rootY + 2.dp.toPx() * scale)
        quadraticTo(
            rootX - trunkBaseHalfW * 0.70f, rootY - trunkHeight * 0.15f,
            forkX - trunkTopHalfW * 0.85f, forkY
        )
        lineTo(forkX + trunkTopHalfW * 0.15f, forkY)
        quadraticTo(
            rootX - trunkBaseHalfW * 0.10f, rootY - trunkHeight * 0.15f,
            rootX - trunkBaseHalfW * 0.15f, rootY + 2.dp.toPx() * scale
        )
        close()
    }
    drawPath(path = sunlitTrunkPath, color = barkMid)

    // Bark ridge highlight lines
    val ridgePath = Path().apply {
        moveTo(rootX - trunkBaseHalfW * 0.9f, rootY)
        quadraticTo(
            rootX - trunkBaseHalfW * 0.55f,
            rootY - trunkHeight * 0.25f,
            forkX - trunkTopHalfW * 0.55f,
            forkY
        )
    }
    drawPath(
        path = ridgePath,
        color = barkSunlit,
        style = Stroke(width = 1.8.dp.toPx() * scale, cap = StrokeCap.Round)
    )

    // Spreading wooden boughs reaching up into the foliage crown
    val branchTargets = if (isMature) {
        listOf(
            Offset(crownCenterX - crownW * 0.36f, crownCenterY + crownH * 0.08f) to 5.2f,
            Offset(crownCenterX - crownW * 0.20f, crownCenterY - crownH * 0.22f) to 4.4f,
            Offset(crownCenterX + crownW * 0.04f, crownCenterY - crownH * 0.28f) to 4.6f,
            Offset(crownCenterX + crownW * 0.24f, crownCenterY - crownH * 0.18f) to 4.2f,
            Offset(crownCenterX + crownW * 0.37f, crownCenterY + crownH * 0.10f) to 4.8f
        )
    } else {
        listOf(
            Offset(crownCenterX - crownW * 0.28f, crownCenterY + crownH * 0.05f) to 4.0f,
            Offset(crownCenterX - crownW * 0.06f, crownCenterY - crownH * 0.22f) to 4.2f,
            Offset(crownCenterX + crownW * 0.26f, crownCenterY - crownH * 0.02f) to 3.8f
        )
    }

    branchTargets.forEachIndexed { idx, (target, thicknessDp) ->
        val startY = forkY + (idx % 2) * 5.dp.toPx() * scale
        val boughPath = Path().apply {
            moveTo(forkX, startY)
            quadraticTo(
                (forkX + target.x) * 0.5f,
                startY - (startY - target.y) * 0.65f,
                target.x,
                target.y
            )
        }
        drawPath(
            path = boughPath,
            color = barkDark,
            style = Stroke(width = thicknessDp.dp.toPx() * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = boughPath,
            color = barkSunlit.copy(alpha = 0.7f * alpha),
            style = Stroke(width = (thicknessDp * 0.38f).dp.toPx() * scale, cap = StrokeCap.Round)
        )
    }

    // 4. Mid-Canopy & Sunlit Scalloped Botanical Bough Masses
    val midBoughOffsets = if (isMature) {
        listOf(
            Triple(-0.35f, 0.04f, 0.36f),
            Triple(0.35f, 0.06f, 0.35f),
            Triple(-0.18f, -0.15f, 0.38f),
            Triple(0.18f, -0.14f, 0.37f),
            Triple(0.0f, 0.06f, 0.40f),
            Triple(0.0f, -0.26f, 0.36f)
        )
    } else {
        listOf(
            Triple(-0.26f, 0.04f, 0.38f),
            Triple(0.26f, 0.06f, 0.37f),
            Triple(-0.10f, -0.16f, 0.40f),
            Triple(0.12f, -0.12f, 0.38f)
        )
    }
    midBoughOffsets.forEachIndexed { idx, (ox, oy, rScale) ->
        val bx = crownCenterX + crownW * ox
        val by = crownCenterY + crownH * oy
        val rx = crownW * rScale * 0.58f
        val ry = crownH * rScale * 0.50f

        drawScallopedFoliageMass(
            centerX = bx,
            centerY = by,
            radiusX = rx,
            radiusY = ry,
            color = midEmeraldColor,
            lobeCount = 10,
            seed = seedHash + 101 + idx * 13
        )
        // Upper-left sunlit bough wash
        drawScallopedFoliageMass(
            centerX = bx - rx * 0.14f,
            centerY = by - ry * 0.18f,
            radiusX = rx * 0.74f,
            radiusY = ry * 0.68f,
            color = sunlitCanopyColor,
            lobeCount = 8,
            seed = seedHash + 211 + idx * 17
        )
    }

    // 5. Individually Painted Pointed Botanical Leaves Across the Crown (65 to 120 leaves!)
    val totalLeaves = if (isMature) {
        (88 + (spanDays - 14).coerceIn(0, 36)).coerceAtMost(124)
    } else {
        (58 + (spanDays - 7).coerceIn(0, 6) * 4)
    }
    val veinLight = Color(0xFFDFF2B6).copy(alpha = 0.65f * alpha)

    for (i in 0 until totalLeaves) {
        val hash = (seedHash * 31 + i * 97) and 0x7FFFFFFF
        // Golden-angle spiral distribution within the elliptical crown
        val normIdx = (i + 0.5f) / totalLeaves.toFloat()
        val radial = kotlin.math.sqrt(normIdx)
        val theta = i * 2.3999631f + (seedHash % 17) * 0.1f
        val lx = crownCenterX + cos(theta) * (crownW * 0.47f) * radial
        val ly = crownCenterY + sin(theta) * (crownH * 0.42f) * radial

        // Determine tonal tier from vertical & horizontal position relative to upper-left sun
        val sunFactor = ((crownCenterY - ly) / (crownH * 0.5f)) + ((crownCenterX - lx) / (crownW * 0.7f))
        val leafBase = when {
            sunFactor > 0.35f -> sunlitCanopyColor
            sunFactor > -0.15f -> midEmeraldColor
            else -> deepShadowLeafColor
        }
        val leafTipHighlight = when {
            sunFactor > 0.35f -> apicalGoldGreen
            sunFactor > -0.15f -> sunlitCanopyColor
            else -> midEmeraldColor
        }

        val angleDeg = (atan2Degrees(ly - crownCenterY, lx - crownCenterX) + ((hash % 36) - 18))
        val leafLen = (if (isMature) 11.5f else 10.0f).dp.toPx() * scale * (0.82f + (hash % 35) * 0.01f)
        val leafWid = leafLen * 0.46f

        drawPointedBotanicalLeaf(
            baseX = lx,
            baseY = ly,
            length = leafLen,
            width = leafWid,
            angleDegrees = angleDeg,
            baseColor = leafBase,
            highlightColor = leafTipHighlight,
            veinColor = if (i % 4 == 0 && sunFactor > 0f) veinLight else null
        )
    }

    // 6. Mossy root tufts at base of trunk
    for (r in -2..2) {
        val rx = rootX + r * trunkBaseHalfW * 0.55f
        val bladePath = Path().apply {
            moveTo(rx, rootY + 3.dp.toPx() * scale)
            quadraticTo(
                rx + r * 2.dp.toPx() * scale,
                rootY - 4.dp.toPx() * scale,
                rx + r * 3.5f.dp.toPx() * scale,
                rootY - 7.dp.toPx() * scale
            )
        }
        drawPath(
            path = bladePath,
            color = sunlitCanopyColor,
            style = Stroke(width = 1.5.dp.toPx() * scale, cap = StrokeCap.Round)
        )
    }
}

/**
 * Stage 5: Preserved Dormant Autumn Tree (closed streak run with >= 7 active-span days).
 * Renders a weathered sculptural trunk, fine exposed branches, warm russet/amber/gold autumn leaves,
 * and fallen autumn leaves resting on the hillside beneath.
 */
private fun DrawScope.drawStage5DormantAutumnTree(
    rootX: Float,
    rootY: Float,
    spanDays: Int,
    scale: Float,
    alpha: Float,
    seedHash: Int,
    isDark: Boolean
) {
    val trunkHeight = 76.dp.toPx() * scale
    val trunkBaseHalfW = 7.5.dp.toPx() * scale
    val trunkTopHalfW = 3.2.dp.toPx() * scale
    val forkX = rootX + ((seedHash % 3) - 1) * 2.dp.toPx() * scale
    val forkY = rootY - trunkHeight * 0.54f
    val crownCenterX = forkX
    val crownCenterY = rootY - trunkHeight * 0.78f
    val crownW = 74.dp.toPx() * scale
    val crownH = 54.dp.toPx() * scale

    val russetDeep = Color(0xFF8F3E27).copy(alpha = alpha)
    val amberMid = Color(0xFFC46B33).copy(alpha = alpha)
    val ochreGold = Color(0xFFE09F41).copy(alpha = alpha)
    val sunlitGold = Color(0xFFF0C766).copy(alpha = alpha)

    // Ground shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        topLeft = Offset(rootX - 34.dp.toPx() * scale, rootY - 6.dp.toPx() * scale),
        size = Size(68.dp.toPx() * scale, 12.dp.toPx() * scale)
    )

    // Fallen autumn leaves around the roots
    for (f in 0 until 6) {
        val fx = rootX + ((f - 2.5f) * 9.dp.toPx() * scale)
        val fy = rootY + ((f % 2) * 2.5f.dp.toPx() * scale)
        drawPointedBotanicalLeaf(
            baseX = fx,
            baseY = fy,
            length = 7.dp.toPx() * scale,
            width = 3.4.dp.toPx() * scale,
            angleDegrees = -15f + f * 28f,
            baseColor = if (f % 2 == 0) amberMid else ochreGold,
            highlightColor = sunlitGold,
            veinColor = null
        )
    }

    // Back autumn foliage clouds
    drawScallopedFoliageMass(
        centerX = crownCenterX - crownW * 0.22f,
        centerY = crownCenterY + crownH * 0.04f,
        radiusX = crownW * 0.26f,
        radiusY = crownH * 0.24f,
        color = russetDeep,
        lobeCount = 8,
        seed = seedHash + 7
    )
    drawScallopedFoliageMass(
        centerX = crownCenterX + crownW * 0.22f,
        centerY = crownCenterY + crownH * 0.02f,
        radiusX = crownW * 0.25f,
        radiusY = crownH * 0.23f,
        color = russetDeep,
        lobeCount = 8,
        seed = seedHash + 19
    )

    // Weathered silver-umber trunk & branches
    val barkWeathered = (if (isDark) Color(0xFF4F4136) else Color(0xFF695545)).copy(alpha = alpha)
    val barkHighlight = (if (isDark) Color(0xFF7B6858) else Color(0xFF9B836E)).copy(alpha = alpha)

    val trunkPath = Path().apply {
        moveTo(rootX - trunkBaseHalfW * 1.4f, rootY + 2.dp.toPx() * scale)
        quadraticTo(rootX - trunkBaseHalfW * 0.7f, rootY - trunkHeight * 0.2f, forkX - trunkTopHalfW, forkY)
        lineTo(forkX + trunkTopHalfW, forkY)
        quadraticTo(rootX + trunkBaseHalfW * 0.7f, rootY - trunkHeight * 0.2f, rootX + trunkBaseHalfW * 1.4f, rootY + 2.dp.toPx() * scale)
        close()
    }
    drawPath(path = trunkPath, color = barkWeathered)

    val branches = listOf(
        Offset(crownCenterX - crownW * 0.32f, crownCenterY + crownH * 0.02f),
        Offset(crownCenterX - crownW * 0.12f, crownCenterY - crownH * 0.26f),
        Offset(crownCenterX + crownW * 0.14f, crownCenterY - crownH * 0.24f),
        Offset(crownCenterX + crownW * 0.32f, crownCenterY + crownH * 0.04f)
    )
    branches.forEach { target ->
        val bPath = Path().apply {
            moveTo(forkX, forkY)
            quadraticTo((forkX + target.x) * 0.5f, forkY - (forkY - target.y) * 0.6f, target.x, target.y)
        }
        drawPath(
            path = bPath,
            color = barkWeathered,
            style = Stroke(width = 3.2.dp.toPx() * scale, cap = StrokeCap.Round)
        )
        drawPath(
            path = bPath,
            color = barkHighlight,
            style = Stroke(width = 1.1.dp.toPx() * scale, cap = StrokeCap.Round)
        )
    }

    // Autumn leaf clusters (45..65 pointed autumn leaves)
    val leafCount = (46 + (spanDays - 7).coerceIn(0, 20)).coerceAtMost(66)
    for (i in 0 until leafCount) {
        val normIdx = (i + 0.5f) / leafCount.toFloat()
        val radial = kotlin.math.sqrt(normIdx)
        val theta = i * 2.3999631f + (seedHash % 13) * 0.2f
        val lx = crownCenterX + cos(theta) * (crownW * 0.44f) * radial
        val ly = crownCenterY + sin(theta) * (crownH * 0.39f) * radial
        val angleDeg = atan2Degrees(ly - crownCenterY, lx - crownCenterX) + ((i * 19) % 30 - 15)
        val leafLen = 9.5.dp.toPx() * scale * (0.85f + (i % 4) * 0.06f)

        val baseC = when (i % 3) {
            0 -> russetDeep
            1 -> amberMid
            else -> ochreGold
        }
        val highC = if (i % 2 == 0) ochreGold else sunlitGold

        drawPointedBotanicalLeaf(
            baseX = lx,
            baseY = ly,
            length = leafLen,
            width = leafLen * 0.45f,
            angleDegrees = angleDeg,
            baseColor = baseC,
            highlightColor = highC,
            veinColor = null
        )
    }
}

/**
 * Draws an organic multi-lobed scalloped foliage bough mass using connected cubic Bezier curves.
 */
private fun DrawScope.drawScallopedFoliageMass(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    lobeCount: Int,
    seed: Int
) {
    val path = Path()
    val count = lobeCount.coerceAtLeast(6)
    val step = (2.0 * PI) / count

    for (i in 0 until count) {
        val a0 = i * step
        val aMid = (i + 0.5) * step
        val a1 = (i + 1) * step

        val jitter0 = 0.88f + ((seed + i * 17) and 15) * 0.012f
        val jitterMid = 1.12f + ((seed + i * 31) and 15) * 0.014f
        val jitter1 = 0.88f + ((seed + (i + 1) * 17) and 15) * 0.012f

        val x0 = centerX + cos(a0).toFloat() * radiusX * jitter0
        val y0 = centerY + sin(a0).toFloat() * radiusY * jitter0
        val xCtrl = centerX + cos(aMid).toFloat() * radiusX * jitterMid
        val yCtrl = centerY + sin(aMid).toFloat() * radiusY * jitterMid
        val x1 = centerX + cos(a1).toFloat() * radiusX * jitter1
        val y1 = centerY + sin(a1).toFloat() * radiusY * jitter1

        if (i == 0) {
            path.moveTo(x0, y0)
        }
        path.quadraticTo(xCtrl, yCtrl, x1, y1)
    }
    path.close()
    drawPath(path = path, color = color, style = Fill)
}

/**
 * Draws a single pointed ovate botanical leaf with dual-tone shading and optional midrib vein.
 * Never uses a circle to represent a leaf.
 */
internal fun DrawScope.drawPointedBotanicalLeaf(
    baseX: Float,
    baseY: Float,
    length: Float,
    width: Float,
    angleDegrees: Float,
    baseColor: Color,
    highlightColor: Color,
    veinColor: Color?
) {
    val rad = angleDegrees * (PI.toFloat() / 180f)
    val cosA = cos(rad)
    val sinA = sin(rad)
    // Perpendicular vector for leaf blade width
    val perpX = -sinA
    val perpY = cosA

    val tipX = baseX + cosA * length
    val tipY = baseY + sinA * length

    val mid1X = baseX + cosA * (length * 0.38f)
    val mid1Y = baseY + sinA * (length * 0.38f)
    val mid2X = baseX + cosA * (length * 0.72f)
    val mid2Y = baseY + sinA * (length * 0.72f)

    // Full pointed leaf blade
    val leafPath = Path().apply {
        moveTo(baseX, baseY)
        cubicTo(
            mid1X + perpX * width * 0.65f,
            mid1Y + perpY * width * 0.65f,
            mid2X + perpX * width * 0.35f,
            mid2Y + perpY * width * 0.35f,
            tipX,
            tipY
        )
        cubicTo(
            mid2X - perpX * width * 0.35f,
            mid2Y - perpY * width * 0.35f,
            mid1X - perpX * width * 0.65f,
            mid1Y - perpY * width * 0.65f,
            baseX,
            baseY
        )
        close()
    }
    drawPath(path = leafPath, color = baseColor, style = Fill)

    // Sunlit upper half of the leaf blade for organic painterly volume
    val sunlitHalfPath = Path().apply {
        moveTo(baseX, baseY)
        cubicTo(
            mid1X + perpX * width * 0.62f,
            mid1Y + perpY * width * 0.62f,
            mid2X + perpX * width * 0.32f,
            mid2Y + perpY * width * 0.32f,
            tipX,
            tipY
        )
        lineTo(baseX, baseY)
        close()
    }
    drawPath(path = sunlitHalfPath, color = highlightColor, style = Fill)

    // Optional central midrib vein
    if (veinColor != null) {
        drawLine(
            color = veinColor,
            start = Offset(baseX + cosA * (length * 0.12f), baseY + sinA * (length * 0.12f)),
            end = Offset(baseX + cosA * (length * 0.84f), baseY + sinA * (length * 0.84f)),
            strokeWidth = 0.9.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun atan2Degrees(dy: Float, dx: Float): Float {
    return (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * (180.0 / PI)).toFloat()
}

/**
 * Painterly single-habit glade vignette used inside `HabitBranchCanvas` on Habit Detail and Habit Grove cards.
 * Replaces the old horizontal line with circle nodes with a genuine mini botanical landscape vignette.
 */
internal fun DrawScope.drawHabitGladeVignette(
    habitName: String,
    activeRun: HabitStreakRun?,
    groveRuns: List<HabitStreakRun>,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    // Soft sky background
    drawRect(
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF142019), Color(0xFF213327))
            } else {
                listOf(Color(0xFFF9F3E3), Color(0xFFE7DFC6))
            }
        )
    )

    // Distant hillside contour
    val backHill = Path().apply {
        moveTo(0f, h * 0.66f)
        cubicTo(w * 0.32f, h * 0.54f, w * 0.68f, h * 0.58f, w, h * 0.64f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = backHill,
        color = if (isDark) Color(0xFF263B2C) else Color(0xFF8CA876)
    )

    // Preserved Grove trees along the left/middle hillside (up to 5)
    val displayedGrove = groveRuns.takeLast(5)
    displayedGrove.forEachIndexed { idx, run ->
        val normX = 0.16f + idx * 0.13f
        val rootX = w * normX
        val rootY = h * 0.78f
        val seedHash = run.runId.fold(13) { acc, c -> acc * 31 + c.code }
        if (run.isActive) {
            drawStage3Or4WoodlandTree(
                rootX = rootX,
                rootY = rootY,
                spanDays = run.elapsedActiveSpanCount,
                isMature = run.elapsedActiveSpanCount >= 14,
                tintColor = Color(0xFF3E7042),
                scale = 0.52f,
                alpha = 1f,
                seedHash = seedHash,
                isDark = isDark
            )
        } else {
            drawStage5DormantAutumnTree(
                rootX = rootX,
                rootY = rootY,
                spanDays = run.elapsedActiveSpanCount,
                scale = 0.50f,
                alpha = 1f,
                seedHash = seedHash,
                isDark = isDark
            )
        }
    }

    // Foreground meadow bank
    val foreBank = Path().apply {
        moveTo(0f, h * 0.80f)
        cubicTo(w * 0.35f, h * 0.74f, w * 0.70f, h * 0.78f, w, h * 0.75f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = foreBank,
        color = if (isDark) Color(0xFF1C2E21) else Color(0xFF5D7E48)
    )

    // Active sapling or growing tree featured in the right-center foreground glade
    val heroX = w * 0.80f
    val heroY = h * 0.86f
    val seedHash = habitName.fold(19) { acc, c -> acc * 31 + c.code }
    if (activeRun != null && activeRun.isActive) {
        if (activeRun.qualifiedForGrove) {
            drawStage3Or4WoodlandTree(
                rootX = heroX,
                rootY = heroY,
                spanDays = activeRun.elapsedActiveSpanCount,
                isMature = activeRun.elapsedActiveSpanCount >= 14,
                tintColor = Color(0xFF3B7340),
                scale = 0.62f,
                alpha = 1f,
                seedHash = seedHash,
                isDark = isDark
            )
        } else {
            drawStage2BotanicalSapling(
                rootX = heroX,
                rootY = heroY,
                spanDays = activeRun.elapsedActiveSpanCount,
                tintColor = Color(0xFF4C8B48),
                scale = 0.74f,
                alpha = 1f,
                seedHash = seedHash,
                isDark = isDark
            )
        }
    } else {
        drawStage1SeedMound(
            rootX = heroX,
            rootY = heroY,
            scale = 0.72f,
            alpha = 1f,
            isDark = isDark
        )
    }
}
