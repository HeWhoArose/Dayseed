package com.example.dayseed

import com.example.dayseed.ui.screens.SettingsSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Settings restructure acceptance coverage (hub + bottom-sheet panels; owner instruction "A",
 * 2026-10-31). Covers the pure settings-display logic the hub rows and the quiet-hours editor
 * rely on: format validation, normalisation before saving, and state summaries shown on rows.
 */
class DayseedSettingsValidationTest {

    // --- Quiet hours parsing (validated text entry replaces the previous bare free-text fields) ---

    @Test
    fun quietHoursAcceptsWellFormedTimes() {
        assertEquals(LocalTime.of(21, 0), SettingsSupport.parseQuietHours("21:00"))
        assertEquals(LocalTime.of(0, 0), SettingsSupport.parseQuietHours("00:00"))
        assertEquals(LocalTime.of(23, 59), SettingsSupport.parseQuietHours("23:59"))
        assertEquals(LocalTime.of(9, 30), SettingsSupport.parseQuietHours("9:30"))
        assertEquals(LocalTime.of(9, 30), SettingsSupport.parseQuietHours(" 9:30 "))
    }

    @Test
    fun quietHoursRejectsMalformedOrOutOfRangeInput() {
        assertNull(SettingsSupport.parseQuietHours(""))
        assertNull(SettingsSupport.parseQuietHours("abc"))
        assertNull(SettingsSupport.parseQuietHours("21"))
        assertNull(SettingsSupport.parseQuietHours("21:5"))
        assertNull(SettingsSupport.parseQuietHours("0:5"))
        assertNull(SettingsSupport.parseQuietHours("21:5x"))
        assertNull(SettingsSupport.parseQuietHours("24:00"))
        assertNull(SettingsSupport.parseQuietHours("21:60"))
        assertNull(SettingsSupport.parseQuietHours("-1:30"))
    }

    @Test
    fun quietHoursErrorIsNullOnlyForValidInput() {
        assertNull(SettingsSupport.quietHoursError("21:00"))
        assertNull(SettingsSupport.quietHoursError("08:00"))
        assertEquals(true, SettingsSupport.quietHoursError("") != null)
        assertEquals(true, SettingsSupport.quietHoursError("25:00") != null)
        assertEquals(true, SettingsSupport.quietHoursError("21:5") != null)
    }

    @Test
    fun quietHoursNormalisesToTwoDigitHhMmBeforeSaving() {
        // The stored setting must stay in strict HH:mm so reminder scheduling parsers agree.
        assertEquals("09:30", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("9:30")!!))
        assertEquals("21:00", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("21:00")!!))
        assertEquals("00:05", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("0:05")!!))
    }

    // --- Hub row summaries (current state visible without opening any panel) ---

    @Test
    fun quietHoursSummaryAlwaysShowsEffectiveWindow() {
        assertEquals("21:00 – 08:00", SettingsSupport.quietHoursSummary(null, null))
        assertEquals("21:00 – 08:00", SettingsSupport.quietHoursSummary("21:00", "08:00"))
        assertEquals("23:30 – 06:00", SettingsSupport.quietHoursSummary("23:30", "06:00"))
    }

    @Test
    fun removedHabitsSummaryNeverShowsInternalJargon() {
        assertEquals("None", SettingsSupport.removedHabitsSummary(0))
        assertEquals("1 paused or archived", SettingsSupport.removedHabitsSummary(1))
        assertEquals("3 paused or archived", SettingsSupport.removedHabitsSummary(3))
    }

    @Test
    fun weekStartSummaryIsPlainLanguage() {
        assertEquals("Monday", SettingsSupport.weekStartSummary(DayOfWeek.MONDAY))
        assertEquals("Sunday", SettingsSupport.weekStartSummary(DayOfWeek.SUNDAY))
    }
}
