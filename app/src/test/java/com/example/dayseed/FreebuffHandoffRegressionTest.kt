package com.example.dayseed

import com.example.dayseed.data.HabitMeasurementKind
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.UnitFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic JVM coverage for the Freebuff handoff corrections (2026-11-15).
 *
 * These are pure parsing/formatting tests. They make no claim about physical-device behaviour, and
 * Robolectric screenshots elsewhere remain automated evidence rather than device proof.
 */
class FreebuffHandoffRegressionTest {

    // ---------------------------------------------------------------------
    // Phase 2.1 — KES, UGX, TZS and ZAR stay distinct currencies
    // ---------------------------------------------------------------------

    @Test
    fun eastAfricanCurrenciesKeepDistinctCanonicalCodesAndSymbols() {
        val kes = HabitNaturalLanguageParser.parse("Save 5000 KES")
        val ugx = HabitNaturalLanguageParser.parse("Save 5000 UGX")
        val tzs = HabitNaturalLanguageParser.parse("Save 5000 TZS")
        val zar = HabitNaturalLanguageParser.parse("Save 5000 rand")

        assertEquals("kes", kes.unitCode)
        assertEquals("KSh", kes.currencySymbol)
        assertEquals("ugx", ugx.unitCode)
        assertEquals("USh", ugx.currencySymbol)
        assertEquals("tzs", tzs.unitCode)
        assertEquals("TSh", tzs.currencySymbol)
        assertEquals("rand", zar.unitCode)
        assertEquals("R", zar.currencySymbol)

        assertEquals(
            "KES, UGX and TZS must not collapse onto one currency code",
            3,
            setOf(kes.unitCode, ugx.unitCode, tzs.unitCode).size
        )
        assertEquals(5000, kes.targetValue)
        assertEquals(HabitMeasurementKind.AMOUNT_CURRENCY, kes.measurementKind)
    }

    @Test
    fun spelledOutCurrenciesResolveToTheirOwnCodes() {
        assertEquals("kes", HabitNaturalLanguageParser.parse("Save 1000 kenyan shillings").unitCode)
        assertEquals("ugx", HabitNaturalLanguageParser.parse("Save 1000 ugandan shillings").unitCode)
        assertEquals("tzs", HabitNaturalLanguageParser.parse("Save 1000 tanzanian shillings").unitCode)
        assertEquals("rand", HabitNaturalLanguageParser.parse("Save 1000 south african rand").unitCode)

        // A bare "shillings" is ambiguous across three currencies and must be flagged, not guessed.
        val ambiguous = HabitNaturalLanguageParser.parse("Save 1000 shillings")
        assertTrue(ambiguous.needsUserConfirmation)
        assertNotNull(ambiguous.ambiguityReason)
    }

    @Test
    fun nairaBehaviourIsPreserved() {
        val naira = HabitNaturalLanguageParser.parse("Save ₦5,000")
        assertEquals("naira", naira.unitCode)
        assertEquals("₦", naira.currencySymbol)
        assertEquals(5000, naira.targetValue)

        val minorUnit = HabitNaturalLanguageParser.parse("Save ₦1,250.50")
        assertEquals(125050, minorUnit.targetValue)
        assertEquals(2, minorUnit.valueScale)
        assertEquals("₦1,250.50 naira", minorUnit.formattedTargetSummary())
    }

    @Test
    fun formatterRendersEachCurrencyWithItsOwnSymbol() {
        assertTrue(UnitFormatter.formatValueWithUnit(5000, "kes").startsWith("KSh"))
        assertTrue(UnitFormatter.formatValueWithUnit(5000, "ugx").startsWith("USh"))
        assertTrue(UnitFormatter.formatValueWithUnit(5000, "tzs").startsWith("TSh"))
        assertTrue(UnitFormatter.formatValueWithUnit(5000, "rand").startsWith("R"))
        assertTrue(UnitFormatter.formatValueWithUnit(5000, "naira").startsWith("₦"))

        val ugxText = UnitFormatter.formatValueWithUnit(5000, "ugx")
        assertFalse("UGX must never render as Kenyan shillings", ugxText.contains("KSh"))
    }

    // ---------------------------------------------------------------------
    // Phase 2.3 — fixed-point precision is preserved end to end
    // ---------------------------------------------------------------------

    @Test
    fun fractionalTargetsRoundTripAtStoredPrecision() {
        val litres = HabitNaturalLanguageParser.parse("Drink 1.5 litres")
        assertEquals(150, litres.targetValue)
        assertEquals(2, litres.valueScale)
        assertEquals("litres@scale2", litres.storageUnitCode)
        assertEquals("1.5 litres", litres.formattedTargetSummary())

        val kilograms = HabitNaturalLanguageParser.parse("Lift 0.75 kilograms")
        assertEquals(75, kilograms.targetValue)
        assertEquals(2, kilograms.valueScale)
        assertEquals("0.75 kilograms", kilograms.formattedTargetSummary())
    }

    @Test
    fun wholeItemCountsStayWhole() {
        val pages = HabitNaturalLanguageParser.parse("Read 10 pages")
        assertEquals(10, pages.targetValue)
        assertEquals(0, pages.valueScale)
        assertEquals("10 pages", pages.formattedTargetSummary())

        val seeds = HabitNaturalLanguageParser.parse("Plant 10 seeds")
        assertEquals(10, seeds.targetValue)
        assertEquals(0, seeds.valueScale)
    }

    @Test
    fun progressAccumulatesWithoutPrecisionLoss() {
        val unit = "litres@scale2"
        val first = UnitFormatter.parseUserValueToStoredUnits("0.5", unit)
        val second = UnitFormatter.parseUserValueToStoredUnits("0.25", unit)
        assertEquals(50, first)
        assertEquals(25, second)
        assertEquals("0.75 / 1.5 litres", UnitFormatter.formatProgressRatio(75, 150, unit))
    }

    // ---------------------------------------------------------------------
    // Phase 2.2 — out-of-range targets are rejected, never silently clamped
    // ---------------------------------------------------------------------

    @Test
    fun editorRejectsTargetsBeyondStorageInsteadOfShrinkingThem() {
        val maxScaled = "21474836.47"
        val parsedMax = UnitFormatter.parseTargetInputToStoredUnitsAndUnit(
            targetInput = maxScaled,
            cleanUnit = "litres",
            isCurrency = false,
            isDuration = false
        )
        assertEquals(Int.MAX_VALUE, parsedMax?.first)

        assertNull(
            "A target above the supported range must be rejected, not clamped",
            UnitFormatter.parseTargetInputToStoredUnitsAndUnit(
                targetInput = "21474836.48",
                cleanUnit = "litres",
                isCurrency = false,
                isDuration = false
            )
        )
        assertNull(
            UnitFormatter.parseTargetInputToStoredUnitsAndUnit(
                targetInput = "2147483648",
                cleanUnit = "pages",
                isCurrency = false,
                isDuration = false
            )
        )
        assertNull(
            UnitFormatter.parseTargetInputToStoredUnitsAndUnit(
                targetInput = "2147483648",
                cleanUnit = "min",
                isCurrency = false,
                isDuration = true
            )
        )
    }

    @Test
    fun rejectionCarriesAReviewableMessage() {
        val problem = UnitFormatter.describeTargetInputProblem(
            targetInput = "99999999999",
            isCurrency = true,
            isDuration = false
        )
        assertNotNull(problem)
        assertTrue(problem!!.contains("larger than Dayseed can store"))

        assertNull(
            UnitFormatter.describeTargetInputProblem(
                targetInput = "1.5",
                isCurrency = false,
                isDuration = false
            )
        )
        assertEquals(
            "Target must be a positive number.",
            UnitFormatter.describeTargetInputProblem("0", isCurrency = false, isDuration = false)
        )
    }

    @Test
    fun naturalLanguageParserFlagsOversizedValuesInsteadOfStayingSilent() {
        val oversized = HabitNaturalLanguageParser.parse("Save 99999999999 naira")
        assertTrue(
            "An out-of-range target must surface a confirmation instead of silently changing the goal",
            oversized.needsUserConfirmation
        )
        assertNotNull(oversized.ambiguityReason)
        assertTrue(oversized.ambiguityReason!!.contains("larger than the largest target"))
    }

    // ---------------------------------------------------------------------
    // Phase 2.4 — quick increments are natural values at stored precision
    // ---------------------------------------------------------------------

    @Test
    fun quickIncrementsForFractionalTargetsAreFriendly() {
        val litres = UnitFormatter.quickIncrementStoredUnits(150, "litres@scale2")
        assertEquals(25, litres?.first)
        assertEquals(50, litres?.second)
        assertEquals("0.25 litres", UnitFormatter.formatValueWithUnit(25, "litres@scale2"))
        assertEquals("0.5 litres", UnitFormatter.formatValueWithUnit(50, "litres@scale2"))

        // The old engine artefact: 150 / 4 = 37 units, rendered as +0.37 litres.
        assertFalse(litres!!.first == 37)
    }

    @Test
    fun quickIncrementsForWholeCountsAndCurrenciesStayExact() {
        assertEquals(2 to 5, UnitFormatter.quickIncrementStoredUnits(10, "pages"))
        assertEquals(25 to 50, UnitFormatter.quickIncrementStoredUnits(100, "litres@scale2"))

        val nairaMinorUnits = UnitFormatter.quickIncrementStoredUnits(125050, "naira@scale2")
        assertNotNull(nairaMinorUnits)
        assertEquals(20_000, nairaMinorUnits!!.first)
        assertEquals("₦200.00 naira", UnitFormatter.formatValueWithUnit(nairaMinorUnits.first, "naira@scale2"))
    }

    @Test
    fun tinyTargetsHideQuickIncrementsRatherThanShowingZero() {
        assertNull(
            "A 3-page goal cannot support a quarter-step; the control must be hidden",
            UnitFormatter.quickIncrementStoredUnits(3, "pages")
        )
        assertNull(UnitFormatter.quickIncrementStoredUnits(0, "pages"))
    }
}
