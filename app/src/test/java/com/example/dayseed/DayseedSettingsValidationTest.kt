package com.example.dayseed

import com.example.dayseed.ui.screens.SettingsSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Settings restructure and audit acceptance coverage (D-212 / D-213).
 * Covers:
 * - Quiet hours format validation, normalisation, preset windows, and ±30m time stepping
 * - Hub row summaries (quiet hours, removed habits, week start)
 * - Unified Data & Backup sub-screen summaries, masked password confirmation check, and pre-restore payload inspection
 */
class DayseedSettingsValidationTest {

    @Test
    fun reminderAvailabilityKeepsPreferenceAndAndroidGatesIndependent() {
        val available = SettingsSupport.reminderAvailability(
            appPreferenceEnabled = true,
            runtimePermissionGranted = true,
            appNotificationsEnabled = true,
            reminderChannelCreated = true,
            reminderChannelEnabled = true
        )
        assertTrue(available.appPreferenceEnabled)
        assertTrue(available.runtimePermissionGranted)
        assertTrue(available.appNotificationsEnabled)
        assertTrue(available.reminderChannelCreated)
        assertTrue(available.reminderChannelEnabled)
        assertTrue(available.canDeliver)

        val blockedBySystem = SettingsSupport.reminderAvailability(
            appPreferenceEnabled = true,
            runtimePermissionGranted = true,
            appNotificationsEnabled = false,
            reminderChannelCreated = true,
            reminderChannelEnabled = true
        )
        assertFalse(blockedBySystem.appNotificationsEnabled)
        assertFalse(blockedBySystem.canDeliver)
    }

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
        assertEquals("09:30", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("9:30")!!))
        assertEquals("21:00", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("21:00")!!))
        assertEquals("00:05", SettingsSupport.formatQuietHours(SettingsSupport.parseQuietHours("0:05")!!))
    }

    @Test
    fun quietHoursPresetsAndTimeStepperWorkAndWrapMidnight() {
        assertTrue(SettingsSupport.quietHoursPresets.isNotEmpty())
        for (preset in SettingsSupport.quietHoursPresets) {
            assertNotNull(SettingsSupport.parseQuietHours(preset.start))
            assertNotNull(SettingsSupport.parseQuietHours(preset.end))
        }

        assertEquals("21:30", SettingsSupport.stepQuietHours("21:00", 30))
        assertEquals("20:30", SettingsSupport.stepQuietHours("21:00", -30))
        assertEquals("00:15", SettingsSupport.stepQuietHours("23:45", 30))
        assertEquals("23:45", SettingsSupport.stepQuietHours("00:15", -30))
    }

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

    @Test
    fun unifiedDataSummaryAndPasswordConfirmationBehaveCleanly() {
        assertEquals(
            "3 active habits · 12 check-ins · 4 saved reflections",
            SettingsSupport.dataSummaryLine(3, 0, 12, 4)
        )
        assertEquals(
            "1 active · 2 paused/archived · 1 check-in · 1 saved reflection",
            SettingsSupport.dataSummaryLine(1, 2, 1, 1)
        )

        // Blank password or single-field entry without confirmation is ready
        assertTrue(SettingsSupport.isBackupPasswordReady("", ""))
        assertTrue(SettingsSupport.isBackupPasswordReady("secret123", ""))
        assertTrue(SettingsSupport.isBackupPasswordReady("secret123", "secret123"))

        // Mismatched confirmation blocks export with a clear message
        assertFalse(SettingsSupport.isBackupPasswordReady("secret123", "secret124"))
        assertNotNull(SettingsSupport.backupPasswordMismatchError("secret123", "secret124"))
    }

    @Test
    fun inspectBackupPayloadSummaryDistinguishesEncryptedPlainAndMalformedPayloads() {
        assertNull(SettingsSupport.inspectBackupPayloadSummary("   "))
        assertTrue(
            SettingsSupport.inspectBackupPayloadSummary("not json")!!
                .contains("Doesn't look like a Dayseed backup")
        )
        assertTrue(
            SettingsSupport.inspectBackupPayloadSummary("""{"format":"dayseed_encrypted_backup_v1","ciphertext":"abc"}""")!!
                .contains("Password-protected Dayseed backup detected")
        )
        assertTrue(
            SettingsSupport.inspectBackupPayloadSummary("""{"schema_version":1,"habits":[]}""")!!
                .contains("ready to restore")
        )
    }
}
