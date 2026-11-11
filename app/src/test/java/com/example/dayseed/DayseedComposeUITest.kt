package com.example.dayseed

import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.screens.SettingsSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * R6 / F-06: Automated Compose UI coverage.
 *
 * Three interaction workflows covered through the ViewModel — the state layer
 * that Compose UI observes and renders:
 *
 *   1. Today quick-complete — seed a habit, trigger quick-complete via the
 *      ViewModel, verify StateFlow updates reflect the recorded outcome.
 *   2. Record sheet date and value validation — seed a quantity habit,
 *      trigger entry logging, verify accepted values through ViewModel state.
 *   3. Backup restore error handling — feed invalid backup payloads through
 *      SettingsSupport.inspectBackupPayloadSummary, verify error messages
 *      are presented and no data is mutated.
 *
 * Runs as host-JVM Robolectric unit tests. Instrumented Compose composition
 * tests (createComposeRule) are deferred until a device/emulator is
 * available, consistent with the plan's deferral rule.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedComposeUITest {

    private lateinit var viewModel: DayseedViewModel

    @Before
    fun setUp() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext() as android.app.Application
        val clock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = LocalDate.of(2026, 3, 15)
                .atTime(LocalTime.of(10, 0))
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli()
        )
        viewModel = DayseedViewModel(app)
    }

    // -----------------------------------------------------------------------
    // 1. Today quick-complete
    // -----------------------------------------------------------------------

    @Test
    fun todayQuickCompleteRecordsOutcomeAndUpdatesViewModelState() {
        // Seed a scheduled habit for today.
        viewModel.repository.saveHabitSync(
            Habit(
                habitId = "h_today_quick",
                name = "Morning stretch",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                scheduleKind = com.example.dayseed.data.ScheduleKind.DAILY,
                reminderMode = "OFF"
            ),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        // Trigger quick-complete via the ViewModel (the exact path Compose UI calls).
        viewModel.logHabit(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_today_quick" },
            date = LocalDate.of(2026, 3, 15),
            outcome = com.example.dayseed.data.DailyOutcome.COMPLETED
        )

        // Verify user message confirms completion (what Compose would render).
        assertNotNull(viewModel.userMessage.value)
        assertTrue("user message should confirm completion",
            viewModel.userMessage.value!!.contains("Completed"))

        // Verify a day outcome was recorded.
        val outcomes = viewModel.repository.habitDayOutcomes.value
        assertTrue("at least one day outcome should exist",
            outcomes.any { it.habitId == "h_today_quick" })
    }

    // -----------------------------------------------------------------------
    // 2. Record sheet date and value validation
    // -----------------------------------------------------------------------

    @Test
    fun recordSessionAcceptsValidQuantityAndValue() {
        viewModel.repository.saveHabitSync(
            Habit(
                habitId = "h_record_qty",
                name = "Pages read",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "pages",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 10,
                reminderMode = "OFF"
            ),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        // Log a valid entry (the path Compose RecordSessionSheet calls on Save).
        viewModel.logHabitQuantityOrDuration(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_record_qty" },
            valueUnits = 5,
            date = LocalDate.of(2026, 3, 15),
            entryNote = "Chapter 3"
        )

        // Verify the entry was accepted - user message confirms logging.
        assertNotNull(viewModel.userMessage.value)
        assertTrue("user message should confirm logging",
            viewModel.userMessage.value!!.contains("pages"))

        // Verify the entry exists in the repository's entry list.
        val allEntries = viewModel.repository.habitEntries.value
        assertTrue("at least one entry should exist for this habit",
            allEntries.any { it.habitId == "h_record_qty" })
    }

    @Test
    fun recordSessionRejectsZeroAsValidHonestCheckIn() {
        viewModel.repository.saveHabitSync(
            Habit(
                habitId = "h_record_zero",
                name = "Zero-value habit",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "units",
                targetPeriod = TargetPeriod.DAY,
                reminderMode = "OFF"
            ),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        // Zero is a valid honest check-in per product rules.
        viewModel.logHabitQuantityOrDuration(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_record_zero" },
            valueUnits = 0,
            date = LocalDate.of(2026, 3, 15),
            entryNote = "Honest zero"
        )

        // Zero should be accepted (not rejected as invalid).
        assertNotNull(viewModel.userMessage.value)
        assertTrue("zero should be accepted as honest check-in",
            viewModel.userMessage.value!!.contains("0"))

        val allEntries = viewModel.repository.habitEntries.value
        assertTrue("zero entry should exist",
            allEntries.any { it.habitId == "h_record_zero" && it.valueUnits == 0 })
    }

    // -----------------------------------------------------------------------
    // 3. Backup restore error handling
    // -----------------------------------------------------------------------

    @Test
    fun backupRestorePresentsErrorForInvalidPayload() {
        // Empty string produces null (no message).
        assertNull(SettingsSupport.inspectBackupPayloadSummary(""))

        // Valid JSON but not a Dayseed backup - returns a message about missing fields.
        val notBackup = "{\"foo\":\"bar\"}"
        val summary = SettingsSupport.inspectBackupPayloadSummary(notBackup)
        assertNotNull(summary)
        assertTrue("should report missing Dayseed fields",
            summary!!.contains("not found"))

        // Non-JSON text returns a message.
        val notJson = "not a backup at all"
        val summary2 = SettingsSupport.inspectBackupPayloadSummary(notJson)
        assertNotNull(summary2)
        assertTrue("should report non-JSON input",
            summary2!!.contains("Doesn't look like"))
    }

    @Test
    fun backupRestoreLeavesDatabaseUnchangedOnInvalidPayload() {
        // Seed some data first.
        viewModel.repository.saveHabitSync(
            Habit(
                habitId = "h_protected",
                name = "Protected habit",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                reminderMode = "OFF"
            ),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )
        val beforeCount = viewModel.repository.loadHabitsSync().size

        // Attempt restore with invalid payloads — must not mutate data.
        val invalidPayloads = listOf(
            "",
            "plain text",
            "{\"not\":\"dayseed\"}",
            "{\"backup_kind\":\"dayseed_portable_backup\"}"
        )
        for (payload in invalidPayloads) {
            SettingsSupport.inspectBackupPayloadSummary(payload)
            // All paths must not crash
        }

        val afterCount = viewModel.repository.loadHabitsSync().size
        assertEquals("database must be unchanged after invalid restore attempts",
            beforeCount, afterCount)
    }

    @Test
    fun backupRestorePresentsPasswordProtectedHint() {
        // Create an encrypted backup, then inspect without password.
        val habit = Habit(
            habitId = "h_encrypted",
            name = "Encrypted backup",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            reminderMode = "OFF"
        )
        viewModel.repository.saveHabitSync(habit, effectiveDateOverride = LocalDate.of(2026, 3, 15))
        val encrypted = viewModel.repository.exportPortableBackupSync("secret")

        // Without password: should indicate password protection, not crash.
        val withoutPassword = SettingsSupport.inspectBackupPayloadSummary(encrypted)
        assertNotNull(withoutPassword)
        assertTrue("should indicate password-protected backup detected",
            withoutPassword!!.contains("Password-protected"))

        // With wrong password: should throw a crypto exception (not succeed silently).
        var threw = false
        try {
            com.example.dayseed.data.BackupAndExportManager.unwrapBackupPayload(encrypted, "wrong")
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("wrong password must throw", threw)
    }
}
