package com.example.dayseed

import android.content.Context
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.BackupAndExportManager
import com.example.dayseed.data.BackupCryptoConfig
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.Habit
import com.example.dayseed.ui.screens.SettingsSupport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Coverage for the reminder scheduling/delivery pipeline and the corrected backup file flow
 * (owner decisions recorded for this phase):
 * - schedule trigger computation (inexact AlarmManager; next-day rollover);
 * - global opt-in gate (disabled until physical-device delivery verification);
 * - delivery ledger helpers (statuses, POST_FAILED correction);
 * - pre-restore backup metadata (date, format/schema version, category counts)
 *   for both plain and password-protected portable backups.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedReminderAndBackupTest {

    private lateinit var context: Context
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = 1_773_568_800_000L
        )
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
        // D-230: reminder evaluation now gates on the production delivery environment; the
        // fixtures here assume a visible-delivery environment (see
        // DayseedReminderAlarmLifecycleTest for the receiver-flow equivalent).
        shadowOf(context.applicationContext as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    // ---- Schedule trigger computation (pure logic) ----

    @Test
    fun nextTriggerRollsOverToTomorrowWhenTodayPassed() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = LocalDateTime.of(2026, 3, 15, 10, 0)
        val trigger = ReminderScheduler.nextTriggerFor("09:00", zone, now)!!
        assertEquals(LocalDateTime.of(2026, 3, 16, 9, 0), trigger)
    }

    @Test
    fun nextTriggerStillTodayWhenTimeLaterToday() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = LocalDateTime.of(2026, 3, 15, 10, 0)
        val trigger = ReminderScheduler.nextTriggerFor("14:00", zone, now)!!
        assertEquals(LocalDateTime.of(2026, 3, 15, 14, 0), trigger)
    }

    @Test
    fun nextTriggerRejectsMalformedTimes() {
        assertNull(ReminderScheduler.nextTriggerFor("25:99", ZoneId.of("UTC"), LocalDateTime.now()))
        assertNull(ReminderScheduler.nextTriggerFor("abc", ZoneId.of("UTC"), LocalDateTime.now()))
    }

    // ---- Global opt-in gate: disabled until delivery is device-verified ----

    @Test
    fun globalRemindersDefaultToDisabled() {
        assertFalse(ReminderScheduler.isGlobalRemindersEnabled(repository))
    }

    @Test
    fun globalRemindersSettingIsHonouredWhenTrue() {
        repository.setSettingSync("reminders_enabled", "ENUM", "true")
        assertTrue(ReminderScheduler.isGlobalRemindersEnabled(repository))
        repository.setSettingSync("reminders_enabled", "ENUM", "false")
        assertFalse(ReminderScheduler.isGlobalRemindersEnabled(repository))
    }

    // ---- Delivery ledger helpers ----

    @Test
    fun deliveryLedgerRecordsAndCorrectsPostFailures() {
        val resolvedHabit = Habit(
            habitId = "h_rem",
            name = "Water plant",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cups",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            reminderMode = "AT_TIME",
            reminderTimeLocal = "09:00"
        )
        repository.saveHabitSync(resolvedHabit)

        val date = repository.clock.todayLocal()
        val deliveryKey = "HABIT_DAILY:h_rem:$date"

        // Nothing recorded before evaluation.
        assertNull(repository.getReminderDeliveryStatus(deliveryKey))

        val results = repository.evaluateTodayRemindersSync(date)
        val target = results.firstOrNull { it.habitId == "h_rem" }
        assertNotNull(target)
        // 09:00 is outside quiet hours (21:00–08:00), habit logged nothing, cap not reached.
        // A fresh decision reserves the slot as SCHEDULED; only a confirmed Android post
        // promotes the row to DELIVERED ("SCHEDULED" is not a delivery claim).
        assertEquals("SCHEDULED", target!!.deliveryStatus)
        assertEquals("SCHEDULED", repository.getReminderDeliveryStatus(deliveryKey))

        // Simulate a failed Android notification post: the ledger must reflect reality.
        repository.markReminderDeliveryNotDelivered(
            deliveryKey = deliveryKey,
            suppressionReason = "POST_FAILED",
            nowUtcMs = testClock.nowUtcMs()
        )
        val status = repository.getReminderDeliveryStatus(deliveryKey)
        assertEquals("SUPPRESSED", status)

        // Idempotent: a re-evaluation replay of the same key keeps the Suppressed state
        // rather than silently re-marking the reminder delivered.
        val replay = repository.evaluateTodayRemindersSync(date).firstOrNull { it.habitId == "h_rem" }
        assertEquals("SUPPRESSED", replay?.deliveryStatus)
    }

    // ---- Pre-restore backup metadata (date, versions, category counts) ----

    private fun seededBackup(password: String? = null): String {
        repository.setSettingSync("theme_mode", "ENUM", "DARK_DAYPART_ART")
        val habit = Habit(
            habitId = "h_meta",
            name = "Stretch",
            trackingMode = TrackingMode.EFFORT_AWARE
        )
        repository.saveHabitSync(habit, allowLegacyEffortModeCreation = true)
        return repository.exportPortableBackupSync(password)
    }

    @Test
    fun backupMetadataParsesPlainBackupBeforeMutation() {
        val raw = seededBackup()
        val metadata = SettingsSupport.inspectBackupMetadata(raw)
        assertNotNull(metadata)
        assertEquals("dayseed_portable_backup", metadata!!.backupKind)
        assertEquals(BackupAndExportManager.BACKUP_FORMAT_VERSION, metadata.formatVersion)
        assertFalse(metadata.isPasswordProtected)
        assertTrue(metadata.categoryCounts.keys.contains("habits"))
        assertTrue(metadata.categoryCounts.keys.contains("journal_entries"))
        assertEquals(1, metadata.categoryCounts["habits"])
        assertNotNull(metadata.exportedAtReadable)

        val line = SettingsSupport.backupMetadataLine(metadata)
        assertNotNull(line)
        assertTrue(line!!.contains("format v"))
        assertTrue(line.contains("schema v"))
        assertTrue(line.contains("exported 2026-03-15"))
        assertTrue(line.contains("1 habits"))
    }

    @Test
    fun backupMetadataNeedsCorrectPasswordForEncryptedBackup() {
        val raw = seededBackup(password = "pebble")
        assertTrue(raw.contains(BackupCryptoConfig.ENVELOPE_FORMAT))

        val withoutPassword = SettingsSupport.inspectBackupMetadata(raw)
        assertNull(withoutPassword)

        val wrongPassword = SettingsSupport.inspectBackupMetadata(raw, "wrong")
        assertNull(wrongPassword)

        val correct = SettingsSupport.inspectBackupMetadata(raw, "pebble")
        assertNotNull(correct)
        assertEquals("dayseed_portable_backup", correct!!.backupKind)
        assertTrue(correct.isPasswordProtected)
        assertEquals(BackupAndExportManager.BACKUP_FORMAT_VERSION, correct.formatVersion)
    }

    @Test
    fun backupMetadataRejectsNonBackupJson() {
        assertNull(SettingsSupport.inspectBackupMetadata("{\"not\":\"dayseed\"}"))
        assertNull(SettingsSupport.inspectBackupMetadata("plain text"))
        assertNull(SettingsSupport.inspectBackupMetadata(""))
    }
}
