package com.example.dayseed.ui.screens

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Pure settings-display and validation support for the four-section Settings hub (D-212 / D-213).
 * No Compose types here: every function is deterministic, unit-testable JVM logic.
 */
object SettingsSupport {

    data class QuietHoursPreset(
        val id: String,
        val label: String,
        val start: String,
        val end: String
    )

    val quietHoursPresets: List<QuietHoursPreset> = listOf(
        QuietHoursPreset("default_21_08", "21:00 – 08:00", "21:00", "08:00"),
        QuietHoursPreset("late_22_07", "22:00 – 07:00", "22:00", "07:00"),
        QuietHoursPreset("night_23_0730", "23:00 – 07:30", "23:00", "07:30"),
        QuietHoursPreset("early_20_0630", "20:00 – 06:30", "20:00", "06:30")
    )

    private val hhMm = Regex("^(\\d{1,2}):(\\d{2})$")
    private val strictHhMm = Regex("^(\\d{2}):(\\d{2})$")

    /** Parses a quiet-hours time; returns null when malformed or outside 00:00–23:59. */
    fun parseQuietHours(raw: String): LocalTime? {
        val match = hhMm.matchEntire(raw.trim()) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    /**
     * Parses a strict two-digit 24-hour time in `HH:mm` format (`00:00`–`23:59`).
     * Rejects single-digit hours (`9:05`, `8:30`) or single-digit minutes (`09:5`) as well as out-of-range values.
     */
    fun parseStrictHhMm(raw: String): LocalTime? {
        val match = strictHhMm.matchEntire(raw.trim()) ?: return null
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
     * Steps a quiet-hours time string by [deltaMinutes] (e.g., -30 or +30), wrapping cleanly
     * across midnight. Falls back to [fallback] if [raw] is currently invalid.
     */
    fun stepQuietHours(raw: String, deltaMinutes: Long, fallback: LocalTime = LocalTime.of(21, 0)): String {
        val base = parseQuietHours(raw) ?: fallback
        return formatQuietHours(base.plusMinutes(deltaMinutes))
    }

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

    /**
     * Plain-language pre-backup / on-device data summary line shown at the top of the
     * unified Data & Backup sub-screen (satisfies docs/26-SCREEN-AND-DATA-CONTRACTS.md).
     */
    fun dataSummaryLine(
        activeHabits: Int,
        pausedHabits: Int,
        checkInCount: Int,
        journalEntryCount: Int
    ): String {
        val habitPart = when {
            activeHabits == 0 && pausedHabits == 0 -> "0 habits"
            pausedHabits == 0 -> "$activeHabits active ${if (activeHabits == 1) "habit" else "habits"}"
            else -> "$activeHabits active · $pausedHabits paused/archived"
        }
        val checkInPart = "$checkInCount ${if (checkInCount == 1) "check-in" else "check-ins"}"
        val journalPart = "$journalEntryCount saved ${if (journalEntryCount == 1) "reflection" else "reflections"}"
        return "$habitPart · $checkInPart · $journalPart"
    }

    /**
     * Validates optional backup password and confirmation.
     * - Empty password is valid (unencrypted portable backup).
     * - If confirmPassword is left blank (e.g. single-field quick entry or existing test), it is allowed.
     * - If confirmPassword is provided and differs from password, returns an error message.
     */
    fun backupPasswordMismatchError(password: String, confirmPassword: String): String? {
        if (password.isBlank() || confirmPassword.isBlank()) return null
        return if (password != confirmPassword) {
            "Passwords do not match yet"
        } else {
            null
        }
    }

    fun isBackupPasswordReady(password: String, confirmPassword: String): Boolean =
        backupPasswordMismatchError(password, confirmPassword) == null

    /**
     * Inspects a pasted backup payload before restoring so the user gets immediate feedback
     * on whether it is a password-protected backup, a plain Dayseed backup, or unrecognised text.
     */
    fun inspectBackupPayloadSummary(rawPayload: String): String? {
        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return "Doesn't look like a Dayseed backup file yet (expected JSON starting with '{')."
        }
        return when {
            trimmed.contains("\"dayseed_encrypted_backup_v1\"") ->
                "Password-protected Dayseed backup detected. Enter its password on the previous screen before restoring."
            trimmed.contains("\"habits\"") || trimmed.contains("\"journal_entries\"") || trimmed.contains("\"schema_version\"") ->
                "Dayseed portable backup detected and ready to restore."
            else ->
                "JSON detected, but Dayseed backup fields were not found."
        }
    }
}
