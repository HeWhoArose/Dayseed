package com.example.dayseed.data

import java.text.NumberFormat
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.roundToInt

/**
 * High-level measurement classification for progressive-disclosure habit creation.
 * Maps cleanly onto the canonical v3 SQLite [TrackingMode] (`COMPLETION_ONLY`, `QUANTITY`, `DURATION`, `EFFORT_AWARE`)
 * while distinguishing monetary/amount targets (`AMOUNT_CURRENCY`) from general custom quantities (`QUANTITY`)
 * without requiring a destructive database schema change.
 */
enum class HabitMeasurementKind(val label: String) {
    SIMPLE_CHECK_OFF("Simple check-off"),
    QUANTITY("Quantity"),
    DURATION("Duration"),
    AMOUNT_CURRENCY("Amount / currency")
}

/**
 * Deterministic, 100% offline natural-language interpretation of a user's habit intention.
 */
data class SmartHabitInterpretation(
    val rawInput: String,
    val activityName: String,
    val measurementKind: HabitMeasurementKind,
    val trackingMode: TrackingMode,
    val targetValue: Int?,
    val unitCode: String?,
    val displayUnit: String?,
    val currencySymbol: String? = null,
    val scheduleKind: ScheduleKind = ScheduleKind.DAILY,
    val targetPeriod: TargetPeriod = TargetPeriod.DAY,
    val scheduledWeekdaysMask: Int? = null,
    val extractedMeasurementFromPrompt: Boolean = false,
    val extractedScheduleFromPrompt: Boolean = false,
    val needsUserConfirmation: Boolean = false,
    val ambiguityReason: String? = null
) {
    val scheduleSummary: String
        get() = when (scheduleKind) {
            ScheduleKind.DAILY -> "Daily"
            ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> "Weekly"
            ScheduleKind.SELECTED_WEEKDAYS -> formatWeekdaysMask(scheduledWeekdaysMask ?: 0b0011111)
        }

    companion object {
        fun formatWeekdaysMask(mask: Int): String {
            if (mask == WeekdayMask.ALL_DAYS_MASK) return "Every day"
            if (mask == 0b0011111) return "Mon–Fri"
            if (mask == 0b1100000) return "Weekends"
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val active = names.indices.filter { (mask and (1 shl it)) != 0 }.map { names[it] }
            return if (active.isEmpty()) "Selected weekdays" else active.joinToString(", ")
        }
    }
}

/**
 * Pure offline, deterministic parser that interprets natural-language habit prompts such as:
 * - "Read 10 pages of a book" -> Activity: Read, Target: 10, Unit: pages, Kind: QUANTITY
 * - "Plant 10 seeds" -> Activity: Plant, Target: 10, Unit: seeds, Kind: QUANTITY
 * - "Sell 5 handmade bags" -> Activity: Sell, Target: 5, Unit: handmade bags, Kind: QUANTITY
 * - "Feed 3 animals" -> Activity: Feed, Target: 3, Unit: animals, Kind: QUANTITY
 * - "Write 500 words" -> Activity: Write, Target: 500, Unit: words, Kind: QUANTITY
 * - "Save ₦5,000" -> Activity: Save, Target: 5000, Unit: naira, Currency: ₦, Kind: AMOUNT_CURRENCY
 * - "Walk for 30 minutes" -> Activity: Walk, Target: 30, Unit: min (minutes), Kind: DURATION
 * - "Meditate daily" -> Activity: Meditate, Kind: SIMPLE_CHECK_OFF, Schedule: DAILY
 */
object HabitNaturalLanguageParser {

    private data class CurrencySpec(
        val symbol: String,
        val canonicalUnit: String,
        val aliases: Set<String>
    )

    private val currencySpecs = listOf(
        CurrencySpec("₦", "naira", setOf("₦", "ngn", "naira")),
        CurrencySpec("$", "dollars", setOf("$", "usd", "dollar", "dollars")),
        CurrencySpec("£", "pounds", setOf("£", "gbp", "pound", "pounds")),
        CurrencySpec("€", "euros", setOf("€", "eur", "euro", "euros")),
        CurrencySpec("¥", "yen", setOf("¥", "jpy", "yen")),
        CurrencySpec("₹", "rupees", setOf("₹", "inr", "rupee", "rupees")),
        CurrencySpec("₵", "cedis", setOf("₵", "ghs", "cedi", "cedis")),
        CurrencySpec("KSh", "shillings", setOf("ksh", "kes", "shilling", "shillings")),
        CurrencySpec("R", "rand", setOf("zar", "rand"))
    )

    private val durationMinutesAliases = setOf(
        "min", "mins", "minute", "minutes", "m"
    )

    private val durationHoursAliases = setOf(
        "hr", "hrs", "hour", "hours", "h"
    )

    private val trailingContextPatterns = listOf(
        Regex("""\s+of\s+(?:a|an|the|my|some)\s+.+$""", RegexOption.IGNORE_CASE),
        Regex("""\s+(?:in|from|on|with|to)\s+(?:a|an|the|my)\s+.+$""", RegexOption.IGNORE_CASE)
    )

    private val scheduleTrailingPatterns = listOf(
        Regex("""\b(?:every\s+day|daily|each\s+day|per\s+day|a\s+day)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:every\s+week|weekly|each\s+week|per\s+week|a\s+week)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:on\s+)?weekdays\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:on\s+)?weekends\b""", RegexOption.IGNORE_CASE)
    )

    private val weekdayTokenMap = linkedMapOf(
        Regex("""\b(?:mon|monday|mondays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.MONDAY,
        Regex("""\b(?:tue|tues|tuesday|tuesdays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.TUESDAY,
        Regex("""\b(?:wed|weds|wednesday|wednesdays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.WEDNESDAY,
        Regex("""\b(?:thu|thur|thurs|thursday|thursdays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.THURSDAY,
        Regex("""\b(?:fri|friday|fridays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.FRIDAY,
        Regex("""\b(?:sat|saturday|saturdays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.SATURDAY,
        Regex("""\b(?:sun|sunday|sundays)\b""", RegexOption.IGNORE_CASE) to DayOfWeek.SUNDAY
    )

    // Matches optional currency prefix + integer or decimal number (including comma-grouped thousands)
    private val numberTokenRegex = Regex(
        """(?<![A-Za-z])(₦|\$|£|€|¥|₹|₵|KSh|NGN|USD|GBP|EUR|KES|GHS|INR|JPY)?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d+))?(?![A-Za-z0-9])""",
        RegexOption.IGNORE_CASE
    )

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
                displayUnit = null,
                extractedMeasurementFromPrompt = false,
                extractedScheduleFromPrompt = false,
                needsUserConfirmation = false,
                ambiguityReason = null
            )
        }

        // 1. Detect schedule intent first and strip schedule phrases from working copy
        var working = trimmed
        var scheduleKind = ScheduleKind.DAILY
        var targetPeriod = TargetPeriod.DAY
        var weekdaysMask: Int? = null
        var extractedSchedule = false

        val matchedDays = mutableSetOf<DayOfWeek>()
        for ((regex, dayOfWeek) in weekdayTokenMap) {
            if (regex.containsMatchIn(working)) {
                matchedDays.add(dayOfWeek)
            }
        }
        if (matchedDays.isNotEmpty()) {
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
            targetPeriod = TargetPeriod.DAY
            weekdaysMask = WeekdayMask.toMask(matchedDays)
            extractedSchedule = true
            working = working.replace(
                Regex("""(?:\s+(?:on|every))?(?:\s*(?:,|and|&)?\s*\b(?:mon(?:day)?s?|tue(?:s|sday)?s?|wed(?:nesday)?s?|thu(?:r|rs|rsday)?s?|fri(?:day)?s?|sat(?:urday)?s?|sun(?:day)?s?)\b)+$""", RegexOption.IGNORE_CASE),
                ""
            ).trim()
        } else if (Regex("""\b(?:on\s+)?weekdays\b""", RegexOption.IGNORE_CASE).containsMatchIn(working)) {
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
            targetPeriod = TargetPeriod.DAY
            weekdaysMask = 0b0011111
            extractedSchedule = true
        } else if (Regex("""\b(?:on\s+)?weekends\b""", RegexOption.IGNORE_CASE).containsMatchIn(working)) {
            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
            targetPeriod = TargetPeriod.DAY
            weekdaysMask = 0b1100000
            extractedSchedule = true
        } else if (Regex("""\b(?:every\s+week|weekly|each\s+week|per\s+week|a\s+week)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working)) {
            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            targetPeriod = TargetPeriod.WEEK
            extractedSchedule = true
        } else if (Regex("""\b(?:every\s+day|daily|each\s+day|per\s+day|a\s+day)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working)) {
            scheduleKind = ScheduleKind.DAILY
            targetPeriod = TargetPeriod.DAY
            extractedSchedule = true
        }

        for (pattern in scheduleTrailingPatterns) {
            working = working.replace(pattern, "").trim()
        }
        working = working.replace(Regex("""\s+"""), " ").trim()

        // 2. Find numeric tokens in the remaining text
        val matches = numberTokenRegex.findAll(working).toList()
        if (matches.isEmpty()) {
            val cleanActivity = cleanActivityTitle(working.ifBlank { trimmed })
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = cleanActivity,
                measurementKind = HabitMeasurementKind.SIMPLE_CHECK_OFF,
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetValue = null,
                unitCode = null,
                displayUnit = null,
                scheduleKind = scheduleKind,
                targetPeriod = targetPeriod,
                scheduledWeekdaysMask = weekdaysMask,
                extractedMeasurementFromPrompt = false,
                extractedScheduleFromPrompt = extractedSchedule,
                needsUserConfirmation = false,
                ambiguityReason = null
            )
        }

        val hasMultipleNumbers = matches.size > 1
        val primaryMatch = matches.first()
        val prefixCurrencyRaw = primaryMatch.groupValues[1].trim()
        val integerPartRaw = primaryMatch.groupValues[2].replace(",", "")
        val decimalPartRaw = primaryMatch.groupValues[3]

        val parsedDouble = buildString {
            append(integerPartRaw)
            if (decimalPartRaw.isNotEmpty()) {
                append('.')
                append(decimalPartRaw)
            }
        }.toDoubleOrNull() ?: 1.0

        val hasDecimal = decimalPartRaw.isNotEmpty()

        val beforeNumber = working.substring(0, primaryMatch.range.first).trim()
        val afterNumber = working.substring(primaryMatch.range.last + 1).trim()

        // 3. Extract activity name and unit phrase
        val rawActivityCandidate = when {
            beforeNumber.isNotEmpty() -> stripTrailingPrepositions(beforeNumber)
            afterNumber.isNotEmpty() -> extractTrailingActivityVerb(afterNumber)
            else -> working
        }

        var unitCandidate = if (beforeNumber.isNotEmpty()) {
            cleanUnitPhrase(afterNumber)
        } else {
            cleanUnitWhenNumberLeads(afterNumber, rawActivityCandidate)
        }

        // 4. Determine measurement kind (Currency, Duration, or Quantity)
        val matchedPrefixCurrency = findCurrencyByToken(prefixCurrencyRaw)
        val matchedUnitCurrency = findCurrencyInPhrase(unitCandidate)
        val currencySpec = matchedPrefixCurrency ?: matchedUnitCurrency

        val ambiguityNotes = mutableListOf<String>()
        if (hasMultipleNumbers) {
            ambiguityNotes.add("Multiple numbers detected; using ${primaryMatch.value.trim()} as the target. You can adjust the target or unit below.")
        }

        if (currencySpec != null) {
            val targetInt = parsedDouble.roundToInt().coerceAtLeast(1)
            if (hasDecimal) {
                ambiguityNotes.add("Rounded ${primaryMatch.value.trim()} to whole ${currencySpec.canonicalUnit} ($targetInt).")
            }
            val activity = cleanActivityTitle(
                rawActivityCandidate.ifBlank { "Save" }
            )
            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = activity,
                measurementKind = HabitMeasurementKind.AMOUNT_CURRENCY,
                trackingMode = TrackingMode.QUANTITY,
                targetValue = targetInt,
                unitCode = currencySpec.canonicalUnit,
                displayUnit = "${currencySpec.canonicalUnit} (${currencySpec.symbol})",
                currencySymbol = currencySpec.symbol,
                scheduleKind = scheduleKind,
                targetPeriod = targetPeriod,
                scheduledWeekdaysMask = weekdaysMask,
                extractedMeasurementFromPrompt = true,
                extractedScheduleFromPrompt = extractedSchedule,
                needsUserConfirmation = ambiguityNotes.isNotEmpty(),
                ambiguityReason = ambiguityNotes.joinToString(" ").ifBlank { null }
            )
        }

        // Check Duration (minutes / hours)
        val firstUnitWord = unitCandidate.lowercase(Locale.US).split(" ").firstOrNull().orEmpty()
        if (firstUnitWord in durationMinutesAliases || firstUnitWord in durationHoursAliases) {
            val isHours = firstUnitWord in durationHoursAliases
            val afterDurationWord = unitCandidate.substringAfter(firstUnitWord, "").trim()
            val resolvedActivity = if (rawActivityCandidate.isNotBlank()) {
                cleanActivityTitle(rawActivityCandidate)
            } else {
                val fromAfter = afterDurationWord.replace(Regex("""^(?:of|for)\s+""", RegexOption.IGNORE_CASE), "").trim()
                cleanActivityTitle(fromAfter.ifBlank { "Practice" })
            }

            val (finalTarget, canonicalUnitCode, displayUnitLabel) = if (isHours && hasDecimal) {
                val convertedMinutes = (parsedDouble * 60.0).roundToInt().coerceAtLeast(1)
                ambiguityNotes.add("Converted ${primaryMatch.value.trim()} hours to $convertedMinutes minutes for accurate tracking.")
                Triple(convertedMinutes, "min", "minutes")
            } else if (isHours) {
                val hrs = parsedDouble.roundToInt().coerceAtLeast(1)
                Triple(hrs, "hr", if (hrs == 1) "hour" else "hours")
            } else {
                val mins = parsedDouble.roundToInt().coerceAtLeast(1)
                if (hasDecimal) {
                    ambiguityNotes.add("Rounded ${primaryMatch.value.trim()} minutes to $mins minutes.")
                }
                Triple(mins, "min", if (mins == 1) "minute" else "minutes")
            }

            return SmartHabitInterpretation(
                rawInput = rawInput,
                activityName = resolvedActivity,
                measurementKind = HabitMeasurementKind.DURATION,
                trackingMode = TrackingMode.DURATION,
                targetValue = finalTarget,
                unitCode = canonicalUnitCode,
                displayUnit = displayUnitLabel,
                scheduleKind = scheduleKind,
                targetPeriod = targetPeriod,
                scheduledWeekdaysMask = weekdaysMask,
                extractedMeasurementFromPrompt = true,
                extractedScheduleFromPrompt = extractedSchedule,
                needsUserConfirmation = ambiguityNotes.isNotEmpty(),
                ambiguityReason = ambiguityNotes.joinToString(" ").ifBlank { null }
            )
        }

        // General Quantity (supports arbitrary user units: pages, seeds, handmade bags, animals, words, etc.)
        val targetInt = parsedDouble.roundToInt().coerceAtLeast(1)
        if (hasDecimal) {
            ambiguityNotes.add("Targets are tracked in whole units; rounded ${primaryMatch.value.trim()} to $targetInt.")
        }

        if (unitCandidate.isBlank()) {
            unitCandidate = "times"
            ambiguityNotes.add("No unit was specified after ${primaryMatch.value.trim()}; defaulting unit to 'times'.")
        }

        val resolvedActivity = cleanActivityTitle(
            rawActivityCandidate.ifBlank {
                unitCandidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        )

        return SmartHabitInterpretation(
            rawInput = rawInput,
            activityName = resolvedActivity,
            measurementKind = HabitMeasurementKind.QUANTITY,
            trackingMode = TrackingMode.QUANTITY,
            targetValue = targetInt,
            unitCode = unitCandidate,
            displayUnit = unitCandidate,
            scheduleKind = scheduleKind,
            targetPeriod = targetPeriod,
            scheduledWeekdaysMask = weekdaysMask,
            extractedMeasurementFromPrompt = true,
            extractedScheduleFromPrompt = extractedSchedule,
            needsUserConfirmation = ambiguityNotes.isNotEmpty(),
            ambiguityReason = ambiguityNotes.joinToString(" ").ifBlank { null }
        )
    }

    private fun stripTrailingPrepositions(text: String): String {
        return text
            .replace(Regex("""\s+(?:for|at\s+least|about|around|up\s+to|of|by)$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanUnitPhrase(afterNumber: String): String {
        var cleaned = afterNumber.trim()
        for (pattern in trailingContextPatterns) {
            cleaned = cleaned.replace(pattern, "").trim()
        }
        cleaned = cleaned.replace(Regex("""^of\s+""", RegexOption.IGNORE_CASE), "").trim()
        cleaned = cleaned.trimEnd('.', ',', ';', '!', '?').trim()
        return cleaned
    }

    private fun extractTrailingActivityVerb(afterNumber: String): String {
        val cleaned = cleanUnitPhrase(afterNumber)
        val ofSplit = cleaned.split(Regex("""\s+of\s+""", RegexOption.IGNORE_CASE), limit = 2)
        if (ofSplit.size == 2) {
            return ofSplit[1].trim()
        }
        return cleaned
    }

    private fun cleanUnitWhenNumberLeads(afterNumber: String, extractedActivity: String): String {
        val cleaned = cleanUnitPhrase(afterNumber)
        val ofSplit = cleaned.split(Regex("""\s+of\s+""", RegexOption.IGNORE_CASE), limit = 2)
        if (ofSplit.size == 2) {
            return ofSplit[0].trim()
        }
        return if (cleaned.equals(extractedActivity, ignoreCase = true)) cleaned else cleaned
    }

    private fun cleanActivityTitle(raw: String): String {
        val cleaned = raw
            .trim()
            .trim('.', ',', ';', '-', '—', ':')
            .replace(Regex("""^(?:to|i\s+want\s+to|i'd\s+like\s+to|let's)\s+""", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isEmpty()) return ""
        return cleaned.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
        }
    }

    private fun findCurrencyByToken(token: String): CurrencySpec? {
        if (token.isBlank()) return null
        val lower = token.lowercase(Locale.US)
        return currencySpecs.firstOrNull { spec ->
            spec.symbol.equals(token, ignoreCase = true) || lower in spec.aliases
        }
    }

    private fun findCurrencyInPhrase(phrase: String): CurrencySpec? {
        if (phrase.isBlank()) return null
        val words = phrase.lowercase(Locale.US).split(Regex("""\s+"""))
        val firstWord = words.firstOrNull().orEmpty()
        return currencySpecs.firstOrNull { spec ->
            firstWord in spec.aliases || spec.symbol.equals(firstWord, ignoreCase = true)
        }
    }
}

/**
 * Formats custom units, singular/plural forms, and currency amounts cleanly without mutating
 * the underlying `unitCode` stored in SQLite.
 */
object UnitFormatter {

    private val currencySymbolByUnit = mapOf(
        "naira" to "₦",
        "ngn" to "₦",
        "₦" to "₦",
        "dollars" to "$",
        "dollar" to "$",
        "usd" to "$",
        "$" to "$",
        "pounds" to "£",
        "pound" to "£",
        "gbp" to "£",
        "£" to "£",
        "euros" to "€",
        "euro" to "€",
        "eur" to "€",
        "€" to "€",
        "yen" to "¥",
        "jpy" to "¥",
        "¥" to "¥",
        "rupees" to "₹",
        "rupee" to "₹",
        "inr" to "₹",
        "₹" to "₹",
        "cedis" to "₵",
        "cedi" to "₵",
        "ghs" to "₵",
        "₵" to "₵",
        "shillings" to "KSh",
        "shilling" to "KSh",
        "ksh" to "KSh",
        "kes" to "KSh"
    )

    private val irregularSingulars = mapOf(
        "children" to "child",
        "people" to "person",
        "feet" to "foot",
        "teeth" to "tooth",
        "leaves" to "leaf",
        "loaves" to "loaf",
        "halves" to "half",
        "entries" to "entry",
        "berries" to "berry",
        "stories" to "story",
        "glasses" to "glass",
        "boxes" to "box",
        "dishes" to "dish",
        "batches" to "batch",
        "stretches" to "stretch",
        "quizzes" to "quiz",
        "minutos" to "minuto"
    )

    private val irregularPlurals = irregularSingulars.entries.associate { (plural, singular) -> singular to plural }

    private val uncountableUnits = setOf(
        "naira", "yen", "rand", "ksh", "ngn", "usd", "gbp", "eur", "inr", "ghs", "kes",
        "water", "tea", "coffee", "sleep", "focus", "work", "progress", "count",
        "ml", "l", "km", "m", "kg", "g", "oz", "lb", "min", "hr"
    )

    fun isCurrencyUnit(unitCode: String?): Boolean {
        if (unitCode.isNullOrBlank()) return false
        val normalized = unitCode.trim().lowercase(Locale.US)
        return normalized in currencySymbolByUnit ||
            currencySymbolByUnit.keys.any { key -> normalized.startsWith("$key ") || normalized.endsWith("($key)") }
    }

    fun currencySymbolFor(unitCode: String?): String? {
        if (unitCode.isNullOrBlank()) return null
        val normalized = unitCode.trim().lowercase(Locale.US)
        currencySymbolByUnit[normalized]?.let { return it }
        for ((key, symbol) in currencySymbolByUnit) {
            if (normalized.startsWith("$key ") || normalized.contains("($symbol)")) {
                return symbol
            }
        }
        return null
    }

    fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    /**
     * Returns the natural singular or plural unit label for [count] while preserving multi-word custom units
     * (e.g., `1 handmade bag` vs `5 handmade bags`, `1 page` vs `2 pages`, `1 seed` vs `10 seeds`).
     */
    fun formatUnitForCount(count: Int, rawUnitCode: String?): String {
        val unit = rawUnitCode?.trim().orEmpty()
        if (unit.isEmpty()) return if (count == 1) "unit" else "units"
        if (unit == "min") return "min"
        if (unit == "hr") return if (count == 1) "hr" else "hrs"

        val words = unit.split(Regex("""\s+""")).toMutableList()
        val lastWord = words.last()
        val lowerLast = lastWord.lowercase(Locale.US)

        if (lowerLast in uncountableUnits || isCurrencyUnit(unit)) {
            return unit
        }

        val inflectedLast = if (count == 1) {
            toSingular(lastWord)
        } else {
            toPlural(lastWord)
        }
        words[words.lastIndex] = inflectedLast
        return words.joinToString(" ")
    }

    /**
     * Formats a single recorded value with its unit (e.g., `2 pages`, `1 page`, `₦2,000 naira`).
     */
    fun formatValueWithUnit(value: Int, rawUnitCode: String?): String {
        val unit = rawUnitCode?.trim().orEmpty()
        val symbol = currencySymbolFor(unit)
        val formattedVal = formatNumber(value)
        return if (symbol != null) {
            val cleanUnitName = unit.replace("($symbol)", "").trim()
            if (cleanUnitName.isEmpty() || cleanUnitName.equals(symbol, ignoreCase = true)) {
                "$symbol$formattedVal"
            } else {
                "$symbol$formattedVal $cleanUnitName"
            }
        } else {
            val unitLabel = formatUnitForCount(value, unit)
            "$formattedVal $unitLabel"
        }
    }

    /**
     * Formats progress towards a target (e.g. `6 / 10 pages`, `1 / 1 page`, `₦2,000 / ₦5,000 naira`).
     */
    fun formatProgressRatio(currentValue: Int, targetValue: Int, rawUnitCode: String?): String {
        val unit = rawUnitCode?.trim().orEmpty()
        val symbol = currencySymbolFor(unit)
        val currentStr = formatNumber(currentValue)
        val targetStr = formatNumber(targetValue)
        return if (symbol != null) {
            val cleanUnitName = unit.replace("($symbol)", "").trim()
            if (cleanUnitName.isEmpty() || cleanUnitName.equals(symbol, ignoreCase = true)) {
                "$symbol$currentStr / $symbol$targetStr"
            } else {
                "$symbol$currentStr / $symbol$targetStr $cleanUnitName"
            }
        } else {
            val unitLabel = formatUnitForCount(targetValue, unit)
            "$currentStr / $targetStr $unitLabel"
        }
    }

    private fun toSingular(word: String): String {
        val lower = word.lowercase(Locale.US)
        irregularSingulars[lower]?.let { return matchCase(word, it) }
        if (lower.endsWith("ies") && lower.length > 4) {
            return word.dropLast(3) + "y"
        }
        if (lower.endsWith("ses") || lower.endsWith("xes") || lower.endsWith("ches") || lower.endsWith("shes")) {
            return word.dropLast(2)
        }
        if (lower.endsWith("s") && !lower.endsWith("ss") && !lower.endsWith("us") && !lower.endsWith("is") && lower.length > 2) {
            return word.dropLast(1)
        }
        return word
    }

    private fun toPlural(word: String): String {
        val lower = word.lowercase(Locale.US)
        irregularPlurals[lower]?.let { return matchCase(word, it) }
        if (lower in irregularSingulars) return word
        if (lower.endsWith("s") && !lower.endsWith("ss")) {
            return word
        }
        if (lower.endsWith("y") && lower.length > 2 && lower[lower.length - 2] !in setOf('a', 'e', 'i', 'o', 'u')) {
            return word.dropLast(1) + "ies"
        }
        if (lower.endsWith("ss") || lower.endsWith("x") || lower.endsWith("ch") || lower.endsWith("sh")) {
            return word + "es"
        }
        return word + "s"
    }

    private fun matchCase(template: String, replacement: String): String {
        if (template.firstOrNull()?.isUpperCase() == true) {
            return replacement.replaceFirstChar { it.titlecase(Locale.US) }
        }
        return replacement
    }
}
