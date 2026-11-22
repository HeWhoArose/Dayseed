package com.example.dayseed.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reader-friendly dates for user-facing text. Internal values stay ISO (YYYY-MM-DD) for storage and tests;
 * UI copy should never show "2026-11-14" to people.
 *
 * "Sat, Nov 14" in the current year, "Sat, Nov 14, 2025" otherwise.
 */
fun friendlyDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val pattern = if (date.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/** Same as [friendlyDate] for an ISO string; falls back to the raw text if it cannot be parsed. */
fun friendlyDate(iso: String, today: LocalDate = LocalDate.now()): String =
    runCatching { friendlyDate(LocalDate.parse(iso), today) }.getOrDefault(iso)

/** "Nov 9 – Nov 15" for a week or other short range (adds years only when the range crosses years). */
fun friendlyDateRange(startIso: String, endIso: String): String = runCatching {
    val start = LocalDate.parse(startIso)
    val end = LocalDate.parse(endIso)
    val f = if (start.year == end.year) "MMM d" else "MMM d, yyyy"
    val fmt = DateTimeFormatter.ofPattern(f, Locale.getDefault())
    "${start.format(fmt)} – ${end.format(fmt)}"
}.getOrDefault("$startIso – $endIso")
