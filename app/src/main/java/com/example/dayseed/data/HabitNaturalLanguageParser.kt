package com.example.dayseed.data

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-level measurement classification for Smart Habit Creation.
 * Maps cleanly onto the persisted SQLite `tracking_mode`, `target_value`, and `unit_code` columns
 * without requiring destructive schema migrations.
 *
 * Fractional measurements (e.g. `1.5 litres`, `0.75 kilograms`) and minor-unit currencies
 * (e.g. `₦1,250.50`) encode their fixed-point decimal scale in `unit_code` using the canonical
 * `@scale<N>` suffix (e.g. `litres@scale2`, `kilograms@scale2`, `naira@scale2`) while storing exact
 * scaled integers in `habit_versions.target_value` and `habit_entries.value_units`.
 * Whole-item habits (`10 pages`, `10 seeds`, `₦5,000`) use `scale = 0` with plain `unit_code`.
 */
enum class HabitMeasurementKind(val displayLabel: String) {
    SIMPLE_CHECK_OFF("Simple check-off"),
    DURATION("Duration"),
    QUANTITY("Quantity"),
    AMOUNT_CURRENCY("Amount / Currency")
}

/**
 * Structured result of deterministic, offline natural-language habit parsing.
 */
data class SmartHabitInterpretation(
    val rawInput: String,
    val activityName: String,
    val measurementKind: HabitMeasurementKind,
    val trackingMode: TrackingMode,
    val targetValue: Int?,
    val unitCode: String?,
    val currencySymbol: String? = null,
    val targetPeriod: TargetPeriod = TargetPeriod.DAY,
    val scheduleKind: ScheduleKind = ScheduleKind.DAILY,
    val scheduledWeekdaysMask: Int = WeekdayMask.ALL_DAYS_MASK,
    val needsUserConfirmation: Boolean = false,
    val ambiguityReason: String? = null,
    val valueScale: Int = 0,
    val targetAmountText: String? = targetValue?.toString(),
    val storageUnitCode: String? = UnitFormatter.encodeUnitWithScale(unitCode, valueScale),
    val isDefaultSchedule: Boolean = true
) {
    fun formattedTargetSummary(): String {
        return when (measurementKind) {
            HabitMeasurementKind.SIMPLE_CHECK_OFF -> "Complete once"
            HabitMeasurementKind.DURATION -> {
                val v = targetValue ?: 15
                val encodedUnit = storageUnitCode ?: UnitFormatter.encodeUnitWithScale(unitCode ?: "min", valueScale)
                UnitFormatter.formatValueWithUnit(v, encodedUnit)
            }
            HabitMeasurementKind.AMOUNT_CURRENCY -> {
                val v = targetValue ?: 100
                val encodedUnit = storageUnitCode ?: UnitFormatter.encodeUnitWithScale(unitCode ?: "currency", valueScale)
                UnitFormatter.formatValueWithUnit(v, encodedUnit, currencySymbol)
            }
            HabitMeasurementKind.QUANTITY -> {
                val v = targetValue ?: 1
                val encodedUnit = storageUnitCode ?: UnitFormatter.encodeUnitWithScale(unitCode ?: "items", valueScale)
                UnitFormatter.formatValueWithUnit(v, encodedUnit)
            }
        }
    }

    fun formattedScheduleSummary(): String {
        return when (scheduleKind) {
            ScheduleKind.DAILY -> "Daily"
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val v = targetValue ?: 3
                "$v times per week"
            }
            ScheduleKind.SELECTED_WEEKDAYS -> {
                val days = WeekdayMask.fromMask(scheduledWeekdaysMask)
                when (days) {
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ) -> "Weekdays (Mon–Fri)"
                    setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends (Sat–Sun)"
                    else -> days.sortedBy { it.value }.joinToString(", ") {
                        it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                }
            }
        }
    }
}

/**
 * Deterministic, offline natural-language habit parser.
 * Parses everyday user phrases such as:
 * - "Read 10 pages of a book"
 * - "Plant 10 seeds"
 * - "Sell 5 handmade bags"
 * - "Feed 3 animals"
 * - "Write 500 words"
 * - "Save ₦5,000" / "Save ₦1,250.50"
 * - "Drink 1.5 litres" / "0.75 kilograms"
 * - "Walk for 30 minutes" / "Walk 2.5 hours"
 * - "Meditate daily"
 *
 * Runs 100% locally on-device with zero network calls or AI/LLM dependencies.
 */
object HabitNaturalLanguageParser {

    private data class CurrencySpec(
        val canonicalUnitCode: String,
        val symbol: String,
        val aliases: Set<String>
    )

    private val CURRENCY_SPECS = listOf(
        CurrencySpec("naira", "₦", setOf("naira", "ngn", "₦")),
        CurrencySpec("dollars", "$", setOf("dollar", "dollars", "usd", "$", "buck", "bucks")),
        CurrencySpec("euros", "€", setOf("euro", "euros", "eur", "€")),
        CurrencySpec("pounds", "£", setOf("pound", "pounds", "gbp", "£")),
        CurrencySpec("yen", "¥", setOf("yen", "jpy", "¥")),
        CurrencySpec("rupees", "₹", setOf("rupee", "rupees", "inr", "₹")),
        CurrencySpec("cedis", "₵", setOf("cedi", "cedis", "ghs", "₵")),
        CurrencySpec("shillings", "KSh", setOf("shilling", "shillings", "kes", "ksh", "ugx", "tzs")),
        CurrencySpec("rand", "R", setOf("rand", "zar"))
    )

    private val DURATION_MINUTE_UNITS = setOf(
        "min", "mins", "minute", "minutes", "m"
    )

    private val DURATION_HOUR_UNITS = setOf(
        "hr", "hrs", "hour", "hours", "h"
    )

    private val DURATION_SECOND_UNITS = setOf(
        "sec", "secs", "second", "seconds", "s"
    )

    private val STOPWORD_CONNECTORS = setOf(
        "of", "a", "an", "the", "for", "to", "in", "on", "at", "my", "our", "some", "every", "each", "per"
    )

    private val CURRENCY_SYMBOL_LEADING_REGEX =
        Regex("""^(.*?)\s*([₦$€£¥₹₵])\s*(\d[\d,]*(?:\.\d+)?)\b(.*)$""")
    private val CURRENCY_SYMBOL_TRAILING_REGEX =
        Regex("""^(.*?)\b(\d[\d,]*(?:\.\d+)?)\s*([₦$€£¥₹₵])(.*)$""")

    private val NUMBER_TOKEN_REGEX =
        Regex("""(?<![A-Za-z₦$€£¥₹₵])(\d[\d,]*(?:\.\d+)?)(?![A-Za-z])""")

    fun parse(rawInput: String): SmartHabitInterpretation {
        val trimmed = rawInput.trim().replace(Regex("""\s+"""), " ")
        if (trimmed.isEmpty()) {
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = "",
                measurementKind = HabitMeasurementKind.SIMPLE_CHECK_OFF,
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetValue = null,
                unitCode = null,
                valueScale = 0,
                targetAmountText = null,
                storageUnitCode = null,
                isDefaultSchedule = true
            )
        }

        // 1. Extract schedule phrases from the input while keeping a clean working string
        val scheduleExtraction = extractSchedule(trimmed)
        val working = scheduleExtraction.cleanedText.trim()

        // 2. Check for explicit currency symbols first (e.g., "Save ₦5,000", "Save ₦1,250.50", "Save $50")
        val leadingCurrencyMatch = CURRENCY_SYMBOL_LEADING_REGEX.find(working)
        if (leadingCurrencyMatch != null) {
            return buildCurrencySymbolInterpretation(
                rawInput = rawInput,
                beforePart = leadingCurrencyMatch.groupValues[1],
                symbol = leadingCurrencyMatch.groupValues[2],
                numberPart = leadingCurrencyMatch.groupValues[3],
                afterPart = leadingCurrencyMatch.groupValues[4],
                schedule = scheduleExtraction
            )
        }

        val trailingCurrencyMatch = CURRENCY_SYMBOL_TRAILING_REGEX.find(working)
        if (trailingCurrencyMatch != null) {
            return buildCurrencySymbolInterpretation(
                rawInput = rawInput,
                beforePart = trailingCurrencyMatch.groupValues[1],
                symbol = trailingCurrencyMatch.groupValues[3],
                numberPart = trailingCurrencyMatch.groupValues[2],
                afterPart = trailingCurrencyMatch.groupValues[4],
                schedule = scheduleExtraction
            )
        }

        // 3. Find numeric tokens in the remaining working phrase
        val numberMatches = NUMBER_TOKEN_REGEX.findAll(working).toList()
        if (numberMatches.isEmpty()) {
            val cleanActivity = cleanActivityTitle(working).ifBlank { trimmed }
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = cleanActivity,
                measurementKind = HabitMeasurementKind.SIMPLE_CHECK_OFF,
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetValue = null,
                unitCode = null,
                targetPeriod = scheduleExtraction.targetPeriod,
                scheduleKind = scheduleExtraction.scheduleKind,
                scheduledWeekdaysMask = scheduleExtraction.weekdaysMask,
                needsUserConfirmation = false,
                valueScale = 0,
                targetAmountText = null,
                storageUnitCode = null,
                isDefaultSchedule = !scheduleExtraction.foundInText
            )
        }

        val primaryMatch = numberMatches.first()
        val rawNumStr = primaryMatch.groupValues[1].replace(",", "")
        val parsedDecimal = rawNumStr.toBigDecimalOrNull()
        val hasDecimalPoint = rawNumStr.contains(".") &&
            (parsedDecimal?.stripTrailingZeros()?.scale() ?: 0) > 0
        val hasMultipleNumbers = numberMatches.size > 1

        if (parsedDecimal == null || parsedDecimal <= BigDecimal.ZERO) {
            val cleanActivity = cleanActivityTitle(working).ifBlank { trimmed }
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = cleanActivity,
                measurementKind = HabitMeasurementKind.SIMPLE_CHECK_OFF,
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetValue = null,
                unitCode = null,
                targetPeriod = scheduleExtraction.targetPeriod,
                scheduleKind = scheduleExtraction.scheduleKind,
                scheduledWeekdaysMask = scheduleExtraction.weekdaysMask,
                needsUserConfirmation = true,
                ambiguityReason = "Target must be greater than zero — defaulted to simple check-off.",
                valueScale = 0,
                targetAmountText = null,
                storageUnitCode = null,
                isDefaultSchedule = !scheduleExtraction.foundInText
            )
        }

        val beforeNum = working.substring(0, primaryMatch.range.first).trim()
        val afterNum = working.substring(primaryMatch.range.last + 1).trim()

        // Extract unit phrase from `afterNum`
        val afterTokens = afterNum
            .replace(Regex("""^[-\s]+"""), "")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        val unitWordsCount = extractUnitTokenCount(afterTokens)
        val rawUnitPhrase = afterTokens.take(unitWordsCount).joinToString(" ").trim(',', '.', ';', ':')
        val remainderAfterUnit = afterTokens.drop(unitWordsCount).joinToString(" ").trim()

        val activityCandidate = buildActivityTitle(beforeNum, remainderAfterUnit, rawUnitPhrase)
        val finalActivity = activityCandidate.ifBlank {
            working.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        val unitLower = rawUnitPhrase.lowercase(Locale.getDefault())

        // 3a. Duration check
        if (unitLower in DURATION_MINUTE_UNITS || unitLower in DURATION_HOUR_UNITS || unitLower in DURATION_SECOND_UNITS) {
            val minutesExactDecimal = when (unitLower) {
                in DURATION_HOUR_UNITS -> parsedDecimal.multiply(BigDecimal.valueOf(60))
                in DURATION_SECOND_UNITS -> parsedDecimal.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                else -> parsedDecimal
            }
            val minutesStripped = minutesExactDecimal.stripTrailingZeros()
            val minutesHasFraction = minutesStripped.scale() > 0 && unitLower !in DURATION_SECOND_UNITS
            val targetMinutes = minutesExactDecimal.setScale(0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
                .toInt()
                .coerceAtLeast(1)

            val ambiguity = when {
                hasMultipleNumbers -> "Multiple numbers found — using $targetMinutes min as the target."
                minutesHasFraction -> "Converted $rawNumStr $rawUnitPhrase to $targetMinutes min."
                else -> null
            }
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = finalActivity,
                measurementKind = HabitMeasurementKind.DURATION,
                trackingMode = TrackingMode.DURATION,
                targetValue = targetMinutes,
                unitCode = "min",
                targetPeriod = scheduleExtraction.targetPeriod,
                scheduleKind = scheduleExtraction.scheduleKind,
                scheduledWeekdaysMask = scheduleExtraction.weekdaysMask,
                needsUserConfirmation = ambiguity != null,
                ambiguityReason = ambiguity,
                valueScale = 0,
                targetAmountText = targetMinutes.toString(),
                storageUnitCode = "min",
                isDefaultSchedule = !scheduleExtraction.foundInText
            )
        }

        // 3b. Currency word/code check (e.g., "Save 5000 naira", "Save 1250.50 naira", "Save 50 dollars")
        val matchedCurrency = CURRENCY_SPECS.firstOrNull { spec ->
            unitLower in spec.aliases || afterTokens.firstOrNull()?.lowercase(Locale.getDefault()) in spec.aliases
        }
        if (matchedCurrency != null) {
            return buildCurrencySpecInterpretation(
                rawInput = rawInput,
                finalActivity = finalActivity,
                spec = matchedCurrency,
                parsedDecimal = parsedDecimal,
                hasDecimalPoint = hasDecimalPoint,
                hasMultipleNumbers = hasMultipleNumbers,
                schedule = scheduleExtraction
            )
        }

        // 3c. General Quantity (with custom or standard unit, supporting both whole items and fractional measurements)
        val resolvedUnit = rawUnitPhrase.ifBlank { "times" }
        val isKnownFractional = UnitFormatter.isFractionalMeasurementUnit(resolvedUnit)
        val isKnownWholeCountable = UnitFormatter.isWholeCountableUnit(resolvedUnit)

        if (hasDecimalPoint) {
            // Determine scale (2 for up to 2 decimal places, 3 for 3 decimal places)
            val rawScale = parsedDecimal.stripTrailingZeros().scale().coerceIn(1, 3)
            val effectiveScale = if (rawScale <= 2) 2 else 3
            val maxUnscaled = BigDecimal.valueOf(Int.MAX_VALUE.toLong()).movePointLeft(effectiveScale)
            val clampedDecimal = parsedDecimal.min(maxUnscaled).setScale(effectiveScale, RoundingMode.HALF_UP)
            val scaledTargetInt = clampedDecimal.movePointRight(effectiveScale).toInt().coerceAtLeast(1)
            val cleanAmountText = clampedDecimal.stripTrailingZeros().toPlainString()
            val encodedStorageUnit = UnitFormatter.encodeUnitWithScale(resolvedUnit, effectiveScale)

            val ambiguity = when {
                hasMultipleNumbers ->
                    "Multiple numbers detected — using $cleanAmountText $resolvedUnit as the target."
                isKnownWholeCountable ->
                    "Whole-item unit '$resolvedUnit' was given a decimal value ($cleanAmountText) — tap Adjust interpretation to confirm."
                !isKnownFractional ->
                    "Decimal target ($cleanAmountText $resolvedUnit) — tap Adjust interpretation if you prefer a whole-number unit."
                else -> null
            }

            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = finalActivity,
                measurementKind = HabitMeasurementKind.QUANTITY,
                trackingMode = TrackingMode.QUANTITY,
                targetValue = scaledTargetInt,
                unitCode = resolvedUnit,
                targetPeriod = scheduleExtraction.targetPeriod,
                scheduleKind = scheduleExtraction.scheduleKind,
                scheduledWeekdaysMask = scheduleExtraction.weekdaysMask,
                needsUserConfirmation = ambiguity != null,
                ambiguityReason = ambiguity,
                valueScale = effectiveScale,
                targetAmountText = cleanAmountText,
                storageUnitCode = encodedStorageUnit,
                isDefaultSchedule = !scheduleExtraction.foundInText
            )
        }

        val targetInt = parsedDecimal.setScale(0, RoundingMode.HALF_UP)
            .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
            .toInt()
            .coerceAtLeast(1)
        val ambiguity = when {
            hasMultipleNumbers -> "Multiple numbers detected — using $targetInt $resolvedUnit as the target."
            rawUnitPhrase.isBlank() -> "No unit specified after $targetInt — using '$resolvedUnit'."
            else -> null
        }

        return SmartHabitInterpretation(
            rawInput = rawInput,
            activityName = finalActivity,
            measurementKind = HabitMeasurementKind.QUANTITY,
            trackingMode = TrackingMode.QUANTITY,
            targetValue = targetInt,
            unitCode = resolvedUnit,
            targetPeriod = scheduleExtraction.targetPeriod,
            scheduleKind = scheduleExtraction.scheduleKind,
            scheduledWeekdaysMask = scheduleExtraction.weekdaysMask,
            needsUserConfirmation = ambiguity != null,
            ambiguityReason = ambiguity,
            valueScale = 0,
            targetAmountText = targetInt.toString(),
            storageUnitCode = resolvedUnit,
            isDefaultSchedule = !scheduleExtraction.foundInText
        )
    }

    private fun buildCurrencySymbolInterpretation(
        rawInput: String,
        beforePart: String,
        symbol: String,
        numberPart: String,
        afterPart: String,
        schedule: ScheduleExtraction
    ): SmartHabitInterpretation {
        val spec = CURRENCY_SPECS.firstOrNull { it.symbol == symbol }
            ?: CurrencySpec("currency", symbol, setOf(symbol))
        val cleanNumStr = numberPart.replace(",", "")
        val parsedDecimal = cleanNumStr.toBigDecimalOrNull() ?: BigDecimal.ONE
        val hasDecimalPoint = cleanNumStr.contains(".") &&
            parsedDecimal.stripTrailingZeros().scale() > 0

        val afterTrimmed = afterPart.trim()
        val afterWithoutCurrencyWord = afterTrimmed
            .split(Regex("""\s+"""))
            .dropWhile { it.lowercase(Locale.getDefault()).trim(',', '.') in spec.aliases }
            .joinToString(" ")
        val activity = buildActivityTitle(beforePart.trim(), afterWithoutCurrencyWord, spec.canonicalUnitCode)
            .ifBlank { "Save" }

        return buildCurrencySpecInterpretation(
            rawInput = rawInput,
            finalActivity = activity,
            spec = spec,
            parsedDecimal = parsedDecimal,
            hasDecimalPoint = hasDecimalPoint,
            hasMultipleNumbers = false,
            schedule = schedule
        )
    }

    private fun buildCurrencySpecInterpretation(
        rawInput: String,
        finalActivity: String,
        spec: CurrencySpec,
        parsedDecimal: BigDecimal,
        hasDecimalPoint: Boolean,
        hasMultipleNumbers: Boolean,
        schedule: ScheduleExtraction
    ): SmartHabitInterpretation {
        if (hasDecimalPoint) {
            val effectiveScale = 2
            val maxUnscaled = BigDecimal.valueOf(Int.MAX_VALUE.toLong()).movePointLeft(effectiveScale)
            val clampedDecimal = parsedDecimal.min(maxUnscaled).setScale(effectiveScale, RoundingMode.HALF_UP)
            val scaledTargetInt = clampedDecimal.movePointRight(effectiveScale).toInt().coerceAtLeast(1)
            val formattedDecimalPlain = clampedDecimal.toPlainString()
            val encodedUnit = UnitFormatter.encodeUnitWithScale(spec.canonicalUnitCode, effectiveScale)
            val ambiguity = if (hasMultipleNumbers) {
                "Multiple numbers detected — using ${spec.symbol}$formattedDecimalPlain ${spec.canonicalUnitCode}."
            } else {
                null
            }
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = finalActivity,
                measurementKind = HabitMeasurementKind.AMOUNT_CURRENCY,
                trackingMode = TrackingMode.QUANTITY,
                targetValue = scaledTargetInt,
                unitCode = spec.canonicalUnitCode,
                currencySymbol = spec.symbol,
                targetPeriod = schedule.targetPeriod,
                scheduleKind = schedule.scheduleKind,
                scheduledWeekdaysMask = schedule.weekdaysMask,
                needsUserConfirmation = ambiguity != null,
                ambiguityReason = ambiguity,
                valueScale = effectiveScale,
                targetAmountText = formattedDecimalPlain,
                storageUnitCode = encodedUnit,
                isDefaultSchedule = !schedule.foundInText
            )
        }

        val targetInt = parsedDecimal.setScale(0, RoundingMode.HALF_UP)
            .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
            .toInt()
            .coerceAtLeast(1)
        val ambiguity = if (hasMultipleNumbers) {
            "Multiple numbers detected — using ${spec.symbol}$targetInt ${spec.canonicalUnitCode}."
        } else {
            null
        }
        return SmartHabitInterpretation(
            rawInput = rawInput,
            activityName = finalActivity,
            measurementKind = HabitMeasurementKind.AMOUNT_CURRENCY,
            trackingMode = TrackingMode.QUANTITY,
            targetValue = targetInt,
            unitCode = spec.canonicalUnitCode,
            currencySymbol = spec.symbol,
            targetPeriod = schedule.targetPeriod,
            scheduleKind = schedule.scheduleKind,
            scheduledWeekdaysMask = schedule.weekdaysMask,
            needsUserConfirmation = ambiguity != null,
            ambiguityReason = ambiguity,
            valueScale = 0,
            targetAmountText = targetInt.toString(),
            storageUnitCode = spec.canonicalUnitCode,
            isDefaultSchedule = !schedule.foundInText
        )
    }

    private fun extractUnitTokenCount(afterTokens: List<String>): Int {
        if (afterTokens.isEmpty()) return 0
        val firstLower = afterTokens[0].lowercase(Locale.getDefault()).trim(',', '.', ';', ':')
        if (firstLower in DURATION_MINUTE_UNITS ||
            firstLower in DURATION_HOUR_UNITS ||
            firstLower in DURATION_SECOND_UNITS
        ) {
            return 1
        }
        if (CURRENCY_SPECS.any { firstLower in it.aliases }) {
            return 1
        }
        val takeList = mutableListOf<String>()
        for (token in afterTokens) {
            val lower = token.lowercase(Locale.getDefault()).trim(',', '.', ';', ':')
            if (lower in STOPWORD_CONNECTORS) break
            takeList.add(token)
            if (takeList.size >= 3) break
        }
        return takeList.size.coerceAtLeast(1)
    }

    private fun buildActivityTitle(beforeNum: String, remainderAfterUnit: String, unitPhrase: String): String {
        val cleanedBefore = beforeNum
            .replace(Regex("""(?i)\b(for|at least|about|around|up to|by)\s*$"""), "")
            .trim()
        val cleanedRemainder = remainderAfterUnit
            .replace(Regex("""(?i)^(of|for|in|on|to)\s+"""), "")
            .trim()

        val rawCombined = when {
            cleanedBefore.isNotBlank() && cleanedRemainder.isNotBlank() -> {
                if (remainderAfterUnit.lowercase(Locale.getDefault()).startsWith("of ")) {
                    "$cleanedBefore ${remainderAfterUnit.trim()}"
                } else {
                    "$cleanedBefore $cleanedRemainder"
                }
            }
            cleanedBefore.isNotBlank() -> cleanedBefore
            cleanedRemainder.isNotBlank() -> cleanedRemainder
            unitPhrase.isNotBlank() -> unitPhrase
            else -> ""
        }
        return cleanActivityTitle(rawCombined)
    }

    private fun cleanActivityTitle(raw: String): String {
        val stripped = raw
            .replace(Regex("""(?i)\b(every\s+day|each\s+day|daily|every\s+week|weekly|per\s+day|a\s+day|per\s+week|a\s+week)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-', ';', ':')
        if (stripped.isEmpty()) return ""
        return stripped.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    private data class ScheduleExtraction(
        val cleanedText: String,
        val scheduleKind: ScheduleKind,
        val targetPeriod: TargetPeriod,
        val weekdaysMask: Int,
        val foundInText: Boolean
    )

    private fun extractSchedule(input: String): ScheduleExtraction {
        var text = input
        var scheduleKind = ScheduleKind.DAILY
        var targetPeriod = TargetPeriod.DAY
        var mask = WeekdayMask.ALL_DAYS_MASK
        var foundInText = false

        val weekdaysRegex = Regex("""(?i)\b(on\s+weekdays|every\s+weekday|weekdays)\b""")
        val weekendsRegex = Regex("""(?i)\b(on\s+weekends|every\s+weekend|weekends)\b""")
        val weeklyRegex = Regex("""(?i)\b(every\s+week|each\s+week|weekly|per\s+week|a\s+week)\b""")
        val dailyRegex = Regex("""(?i)\b(every\s+day|each\s+day|daily|per\s+day|a\s+day)\b""")

        when {
            weekdaysRegex.containsMatchIn(text) -> {
                text = text.replace(weekdaysRegex, " ")
                scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                targetPeriod = TargetPeriod.DAY
                mask = WeekdayMask.toMask(
                    setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                )
                foundInText = true
            }
            weekendsRegex.containsMatchIn(text) -> {
                text = text.replace(weekendsRegex, " ")
                scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                targetPeriod = TargetPeriod.DAY
                mask = WeekdayMask.toMask(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                foundInText = true
            }
            weeklyRegex.containsMatchIn(text) -> {
                text = text.replace(weeklyRegex, " ")
                scheduleKind = ScheduleKind.DAILY
                targetPeriod = TargetPeriod.WEEK
                mask = WeekdayMask.ALL_DAYS_MASK
                foundInText = true
            }
            dailyRegex.containsMatchIn(text) -> {
                text = text.replace(dailyRegex, " ")
                scheduleKind = ScheduleKind.DAILY
                targetPeriod = TargetPeriod.DAY
                mask = WeekdayMask.ALL_DAYS_MASK
                foundInText = true
            }
        }

        return ScheduleExtraction(
            cleanedText = text.replace(Regex("""\s+"""), " ").trim(),
            scheduleKind = scheduleKind,
            targetPeriod = targetPeriod,
            weekdaysMask = mask,
            foundInText = foundInText
        )
    }
}

/**
 * Natural singular/plural, fixed-point decimal scale, and currency unit formatter for display across
 * Habit Cards, Record Session Sheet, Smart Habit Creation, Day Detail, and Habit Detail.
 */
object UnitFormatter {

    private val SCALE_SUFFIX_REGEX = Regex("""(?i)^(.*?)@scale([1-3])$""")

    private val CURRENCY_SYMBOLS_BY_UNIT = mapOf(
        "naira" to "₦",
        "ngn" to "₦",
        "₦" to "₦",
        "dollar" to "$",
        "dollars" to "$",
        "usd" to "$",
        "$" to "$",
        "euro" to "€",
        "euros" to "€",
        "eur" to "€",
        "€" to "€",
        "pound" to "£",
        "pounds" to "£",
        "gbp" to "£",
        "£" to "£",
        "yen" to "¥",
        "jpy" to "¥",
        "¥" to "¥",
        "rupee" to "₹",
        "rupees" to "₹",
        "inr" to "₹",
        "₹" to "₹",
        "cedi" to "₵",
        "cedis" to "₵",
        "ghs" to "₵",
        "₵" to "₵"
    )

    private val INVARIANT_UNITS = setOf(
        "min", "mins", "hr", "hrs", "sec", "secs", "ml", "l", "g", "kg", "km", "m", "cm", "oz", "lb", "lbs",
        "naira", "yen", "rand", "currency", "water", "sleep", "focus", "money", "savings"
    )

    private val FRACTIONAL_MEASUREMENT_UNITS = setOf(
        "litre", "litres", "liter", "liters", "l", "ml", "millilitre", "millilitres", "milliliter", "milliliters",
        "kilogram", "kilograms", "kg", "kgs", "gram", "grams", "g", "mg", "milligram", "milligrams",
        "kilometre", "kilometres", "kilometer", "kilometers", "km", "kms",
        "metre", "metres", "meter", "meters", "m", "cm", "centimetre", "centimetres", "centimeter", "centimeters",
        "mile", "miles", "mi", "yard", "yards", "yd", "foot", "feet", "ft", "inch", "inches",
        "pound", "pounds", "lb", "lbs", "stone", "stones",
        "ounce", "ounces", "oz", "fl oz", "fluid ounce", "fluid ounces",
        "cup", "cups", "pint", "pints", "pt", "quart", "quarts", "qt", "gallon", "gallons", "gal",
        "tbsp", "tsp", "tablespoon", "tablespoons", "teaspoon", "teaspoons",
        "hour", "hours", "hr", "hrs"
    )

    private val WHOLE_COUNTABLE_UNITS = setOf(
        "page", "pages", "seed", "seeds", "bag", "bags", "handmade bag", "handmade bags",
        "animal", "animals", "word", "words", "book", "books", "chapter", "chapters", "verse", "verses",
        "rep", "reps", "set", "sets", "step", "steps", "session", "sessions",
        "application", "applications", "email", "emails", "call", "calls", "message", "messages",
        "task", "tasks", "item", "items", "time", "times", "glass", "glasses", "bottle", "bottles",
        "pushup", "pushups", "squat", "squats", "situp", "situps", "pullup", "pullups",
        "lap", "laps", "card", "cards", "flashcard", "flashcards", "problem", "problems",
        "lesson", "lessons", "article", "articles", "post", "posts", "sketch", "sketches",
        "drawing", "drawings", "photo", "photos", "plant", "plants", "tree", "trees",
        "person", "people", "client", "clients", "customer", "customers", "sale", "sales",
        "order", "orders", "piece", "pieces"
    )

    fun extractScale(rawUnitCode: String?): Int {
        val raw = rawUnitCode?.trim().orEmpty()
        val match = SCALE_SUFFIX_REGEX.find(raw) ?: return 0
        return match.groupValues[2].toIntOrNull()?.coerceIn(0, 3) ?: 0
    }

    fun cleanUnitCode(rawUnitCode: String?): String {
        val raw = rawUnitCode?.trim().orEmpty()
        val match = SCALE_SUFFIX_REGEX.find(raw)
        return (match?.groupValues?.get(1) ?: raw).trim()
    }

    fun encodeUnitWithScale(rawUnitCode: String?, scale: Int): String? {
        if (rawUnitCode == null) return null
        val clean = cleanUnitCode(rawUnitCode)
        if (clean.isEmpty()) return clean
        val clampedScale = scale.coerceIn(0, 3)
        return if (clampedScale > 0) "$clean@scale$clampedScale" else clean
    }

    fun isCurrencyUnit(unitCode: String?): Boolean {
        val lower = cleanUnitCode(unitCode).lowercase(Locale.getDefault())
        return lower in CURRENCY_SYMBOLS_BY_UNIT
    }

    fun currencySymbolFor(unitCode: String?): String? {
        val lower = cleanUnitCode(unitCode).lowercase(Locale.getDefault())
        return CURRENCY_SYMBOLS_BY_UNIT[lower]
    }

    fun isFractionalMeasurementUnit(unitCode: String?): Boolean {
        val lower = cleanUnitCode(unitCode).lowercase(Locale.getDefault())
        return lower in FRACTIONAL_MEASUREMENT_UNITS
    }

    fun isWholeCountableUnit(unitCode: String?): Boolean {
        val lower = cleanUnitCode(unitCode).lowercase(Locale.getDefault())
        if (lower in WHOLE_COUNTABLE_UNITS) return true
        val lastWord = lower.substringAfterLast(' ')
        return lastWord in WHOLE_COUNTABLE_UNITS
    }

    /**
     * Converts stored integer units (at the scale encoded in `rawUnitCode`) to a user-facing
     * number string.
     */
    fun formatStoredUnitsBig(
        storedUnits: BigInteger,
        rawUnitCode: String?,
        useGrouping: Boolean = false
    ): String {
        val scale = extractScale(rawUnitCode)
        val isCurr = isCurrencyUnit(rawUnitCode)
        val bd = BigDecimal(storedUnits, scale)
        if (scale == 0) {
            return if (useGrouping) {
                NumberFormat.getNumberInstance(Locale.US).format(storedUnits)
            } else {
                storedUnits.toString()
            }
        }
        if (isCurr) {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                isGroupingUsed = useGrouping
                minimumFractionDigits = scale
                maximumFractionDigits = scale
            }
            return nf.format(bd)
        }
        val stripped = bd.stripTrailingZeros()
        val plain = if (stripped.scale() < 0) stripped.setScale(0).toPlainString() else stripped.toPlainString()
        if (!useGrouping) return plain
        val parts = plain.split('.')
        val wholeBig = parts[0].toBigIntegerOrNull() ?: BigInteger.ZERO
        val formattedWhole = NumberFormat.getNumberInstance(Locale.US).format(wholeBig)
        return if (parts.size > 1) "$formattedWhole.${parts[1]}" else formattedWhole
    }

    fun formatStoredUnits(
        storedUnits: Int,
        rawUnitCode: String?,
        useGrouping: Boolean = false
    ): String = formatStoredUnitsBig(BigInteger.valueOf(storedUnits.toLong()), rawUnitCode, useGrouping)

    /**
     * Formats a stored integer value for pre-populating a text input field when editing an entry
     * or habit target (no grouping commas, preserves decimal representation when scaled).
     */
    fun formatStoredUnitsForInput(storedUnits: Int, rawUnitCode: String?): String {
        return formatStoredUnits(storedUnits, rawUnitCode, useGrouping = false)
    }

    /**
     * Parses a user-entered numeric string (integer or decimal) into stored integer units
     * according to `rawUnitCode`'s scale.
     * Returns `null` if the text is blank, non-numeric, or has more decimal places than allowed
     * for a whole-item unit.
     */
    fun parseUserValueToStoredUnits(inputText: String, rawUnitCode: String?): Int? {
        val cleaned = inputText.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val bd = cleaned.toBigDecimalOrNull() ?: return null
        val scale = extractScale(rawUnitCode)
        val stripped = bd.stripTrailingZeros()
        val inputScale = if (stripped.scale() < 0) 0 else stripped.scale()
        if (inputScale > scale) {
            // User entered a decimal for a scale=0 unit (or more precision than scale allows)
            return null
        }
        val scaled = bd.movePointRight(scale).setScale(0, RoundingMode.HALF_UP)
        if (scaled > BigDecimal.valueOf(Int.MAX_VALUE.toLong()) ||
            scaled < BigDecimal.valueOf(Int.MIN_VALUE.toLong())
        ) {
            return null
        }
        return scaled.toInt()
    }

    /**
     * Parses a user-entered target value on `CreateEditHabitScreen` and returns
     * `(storedTargetInt, encodedStorageUnitCode, scale)`.
     */
    fun parseTargetInputToStoredUnitsAndUnit(
        targetInput: String,
        cleanUnit: String,
        isCurrency: Boolean,
        isDuration: Boolean
    ): Triple<Int, String, Int>? {
        val cleaned = targetInput.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val bd = cleaned.toBigDecimalOrNull() ?: return null
        if (bd <= BigDecimal.ZERO) return null
        val stripped = bd.stripTrailingZeros()
        val hasFraction = stripped.scale() > 0
        if (isDuration) {
            val mins = bd.setScale(0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
                .toInt()
                .coerceAtLeast(1)
            return Triple(mins, "min", 0)
        }
        if (!hasFraction) {
            val whole = bd.setScale(0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
                .toInt()
                .coerceAtLeast(1)
            return Triple(whole, cleanUnitCode(cleanUnit), 0)
        }
        val scale = if (isCurrency) 2 else if (stripped.scale() <= 2) 2 else 3
        val maxUnscaled = BigDecimal.valueOf(Int.MAX_VALUE.toLong()).movePointLeft(scale)
        val clamped = bd.min(maxUnscaled).setScale(scale, RoundingMode.HALF_UP)
        val storedInt = clamped.movePointRight(scale).toInt().coerceAtLeast(1)
        val encodedUnit = encodeUnitWithScale(cleanUnit, scale) ?: cleanUnit
        return Triple(storedInt, encodedUnit, scale)
    }

    fun formatValueWithUnitBig(
        storedUnits: BigInteger,
        rawUnitCode: String?,
        explicitCurrencySymbol: String? = null
    ): String {
        val cleanUnit = cleanUnitCode(rawUnitCode).ifBlank { "units" }
        val symbol = explicitCurrencySymbol ?: currencySymbolFor(cleanUnit)
        if (symbol != null) {
            val formattedNum = formatStoredUnitsBig(storedUnits, rawUnitCode, useGrouping = true)
            val lower = cleanUnit.lowercase(Locale.getDefault())
            return if (lower == symbol.lowercase(Locale.getDefault()) || lower == "currency") {
                "$symbol$formattedNum"
            } else {
                "$symbol$formattedNum $cleanUnit"
            }
        }
        val scale = extractScale(rawUnitCode)
        val isExactOne = BigDecimal(storedUnits, scale).compareTo(BigDecimal.ONE) == 0
        val formattedNum = formatStoredUnitsBig(storedUnits, rawUnitCode, useGrouping = false)
        val displayUnit = if (isExactOne) formatUnitForCount(1, cleanUnit) else formatUnitForCount(2, cleanUnit)
        return "$formattedNum $displayUnit"
    }

    fun formatValueWithUnit(
        count: Int,
        unitCode: String?,
        explicitCurrencySymbol: String? = null
    ): String = formatValueWithUnitBig(BigInteger.valueOf(count.toLong()), unitCode, explicitCurrencySymbol)

    fun formatProgressRatioBig(
        currentUnits: BigInteger,
        targetUnits: BigInteger,
        rawUnitCode: String?
    ): String {
        val cleanUnit = cleanUnitCode(rawUnitCode).ifBlank { "units" }
        val symbol = currencySymbolFor(cleanUnit)
        if (symbol != null) {
            val curFmt = formatStoredUnitsBig(currentUnits, rawUnitCode, useGrouping = true)
            val tgtFmt = formatStoredUnitsBig(targetUnits, rawUnitCode, useGrouping = true)
            val lower = cleanUnit.lowercase(Locale.getDefault())
            return if (lower == symbol.lowercase(Locale.getDefault()) || lower == "currency") {
                "$symbol$curFmt / $symbol$tgtFmt"
            } else {
                "$symbol$curFmt / $symbol$tgtFmt $cleanUnit"
            }
        }
        val scale = extractScale(rawUnitCode)
        val isTargetExactOne = BigDecimal(targetUnits, scale).compareTo(BigDecimal.ONE) == 0
        val unitDisplay = if (isTargetExactOne) formatUnitForCount(1, cleanUnit) else formatUnitForCount(2, cleanUnit)
        val curFmt = formatStoredUnitsBig(currentUnits, rawUnitCode, useGrouping = false)
        val tgtFmt = formatStoredUnitsBig(targetUnits, rawUnitCode, useGrouping = false)
        return "$curFmt / $tgtFmt $unitDisplay"
    }

    fun formatProgressRatio(currentUnits: Int, targetUnits: Int, unitCode: String?): String {
        return formatProgressRatioBig(
            BigInteger.valueOf(currentUnits.toLong()),
            BigInteger.valueOf(targetUnits.toLong()),
            unitCode
        )
    }

    fun formatUnitForCount(count: Int, rawUnit: String): String {
        val trimmed = cleanUnitCode(rawUnit)
        if (trimmed.isEmpty()) return "units"
        val words = trimmed.split(Regex("""\s+""")).toMutableList()
        val lastWord = words.last()
        val lowerLast = lastWord.lowercase(Locale.getDefault())
        if (lowerLast in INVARIANT_UNITS) return trimmed

        val adjustedLast = if (count == 1) {
            singularizeWord(lastWord)
        } else {
            pluralizeWord(lastWord)
        }
        words[words.lastIndex] = adjustedLast
        return words.joinToString(" ")
    }

    private fun singularizeWord(word: String): String {
        val lower = word.lowercase(Locale.getDefault())
        if (lower.length <= 2) return word
        return when {
            lower == "children" -> "child"
            lower == "people" -> "person"
            lower == "Glasses".lowercase() || lower.endsWith("glasses") -> word.dropLast(2)
            lower.endsWith("ies") && lower.length > 4 -> word.dropLast(3) + "y"
            lower.endsWith("ches") || lower.endsWith("shes") || lower.endsWith("xes") || lower.endsWith("zes") ->
                word.dropLast(2)
            lower.endsWith("s") && !lower.endsWith("ss") && !lower.endsWith("us") ->
                word.dropLast(1)
            else -> word
        }
    }

    private fun pluralizeWord(word: String): String {
        val lower = word.lowercase(Locale.getDefault())
        if (lower.length <= 1) return word
        if (lower in setOf("children", "people", "fish", "sheep")) return word
        if (lower.endsWith("s")) return word
        return when {
            lower.endsWith("y") && lower.length > 2 && lower[lower.length - 2] !in "aeiou" ->
                word.dropLast(1) + "ies"
            lower.endsWith("ch") || lower.endsWith("sh") || lower.endsWith("x") || lower.endsWith("z") ->
                word + "es"
            else -> word + "s"
        }
    }
}
