package com.example.dayseed.ui.screens

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Pure settings-display support for the restructured Settings hub (owner instruction "A",
 * 2026-10-31; companion to D-210/D-211 theme work). No Compose types here: every function is
 * unit-testable JVM logic.
 *
 * Quiet hours are entered as free text but are always validated and normalised to strict
 * "HH:mm" before being saved, because reminder scheduling reads the stored setting back.
 * Row summaries follow the Material settings pattern: the secondary text shows the setting's
 * current state, and internal vocabulary (decision IDs, schema/table counts) never appears.
 */
object SettingsSupport {

    private val hhMm = Regex("^(\\d{1,2}):(\\d{2})$")

    /** Parses a quiet-hours time; returns null when malformed or outside 00:00–23:59. */
    fun parseQuietHours(raw: String): LocalTime? {
        val match = hhMm.matchEntire(raw.trim()) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    /** Returns a short correction message, or null when the input is saveable. */
    fun quietHoursError(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "Enter a time, for example 21:00"
        if (!hhMm.matchEntire(trimmed).let { it != null }) return "Use the 24-hour format HH:mm"
        return if (parseQuietHours(trimmed) == null) {
            "Hours run 00–23 and minutes 00–59"
        } else {
            null
        }
    }

    /** Formats a validated time as strict two-digit "HH:mm" for storage. */
    fun formatQuietHours(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)

    /**
     * Effective quiet-hours window shown on the hub row. Defaults mirror the delegated
     * planning default (21:00–08:00) so the row always communicates what is actually in force.
     */
    fun quietHoursSummary(start: String?, end: String?): String {
        val startText = start?.trim().takeUnless { it.isNullOrEmpty() } ?: "21:00"
        val endText = end?.trim().takeUnless { it.isNullOrEmpty() } ?: "08:00"
        return "$startText – $endText"
    }

    /** Row summary for paused/archived habits, phrased for people, not for the schema. */
    fun removedHabitsSummary(count: Int): String = when (count) {
        0 -> "None"
        1 -> "1 paused or archived"
        else -> "$count paused or archived"
    }

    fun weekStartSummary(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.SUNDAY -> "Sunday"
        else -> day.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
