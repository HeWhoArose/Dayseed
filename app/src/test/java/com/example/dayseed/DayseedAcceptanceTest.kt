package com.example.dayseed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.BackupAndExportManager
import com.example.dayseed.data.ComebackTier
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedMigrationBlockedException
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.JournalExportFormat
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ProgressBandClassifier
import com.example.dayseed.data.RestoreConflict
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.WeekdayMask
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedAcceptanceTest {

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
        // Use in-memory database per test for complete isolation
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    // =========================================================================
    // PHASE 1: 12-TABLE SCHEMA, CONSTRAINTS, TRIGGERS & MIGRATION (AC-016..AC-021)
    // =========================================================================

    @Test
    fun phase1_verifyAll12CanonicalTablesAndForeignKeysEnabled() {
        val db = repository.dbHelper.writableDatabase

        // Verify PRAGMA foreign_keys = 1
        db.rawQuery("PRAGMA foreign_keys", null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }

        val expectedTables = setOf(
            "habits",
            "habit_versions",
            "habit_entries",
            "habit_day_outcomes",
            "habit_date_notes",
            "journal_entries",
            "journal_drafts",
            "app_settings",
            "reminder_deliveries",
            "restore_sessions",
            "restore_conflicts",
            "notification_cancel_outbox"
        )

        val actualTables = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) {
                actualTables.add(c.getString(0))
            }
        }
        for (table in expectedTables) {
            assertTrue("Missing table: $table", actualTables.contains(table))
        }

        // Verify canonical columns on habits (no duplicate lifecycle columns)
        val habitCols = getTableColumns(db, "habits")
        assertEquals(setOf("habit_id", "created_at_utc_ms", "updated_at_utc_ms"), habitCols)

        // Verify canonical columns on journal_drafts (intended_journal_entry_id present)
        val draftCols = getTableColumns(db, "journal_drafts")
        assertTrue(draftCols.contains("intended_journal_entry_id"))
        assertTrue(draftCols.contains("source_context"))
    }

    @Test
    fun phase1_verifyCheckConstraintsAndCrossTableTriggers() {
        val db = repository.dbHelper.writableDatabase

        // Create a QUANTITY habit and an EFFORT_AWARE habit
        val qtyHabit = Habit(
            habitId = "h_qty",
            name = "Hydrate",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "glasses",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 8
        )
        val effortHabit = Habit(
            habitId = "h_eff",
            name = "Stretch",
            trackingMode = TrackingMode.EFFORT_AWARE
        )
        repository.saveHabitSync(qtyHabit)
        repository.saveHabitSync(effortHabit)

        // AC-017: habit_day_outcomes must reject SKIPPED or MISSED
        try {
            val cv = ContentValues().apply {
                put("outcome_id", "bad_out_1")
                put("habit_id", "h_eff")
                put("local_date", "2026-03-15")
                put("outcome_kind", "SKIPPED")
                putNull("effort_level")
                put("idempotency_key", "idem_bad_1")
                put("created_at_utc_ms", 1000L)
                put("updated_at_utc_ms", 1000L)
            }
            db.insertOrThrow("habit_day_outcomes", null, cv)
            fail("Expected SQLiteException for SKIPPED in habit_day_outcomes")
        } catch (_: SQLiteException) {
            // Expected
        }

        // AC-018: Mode exclusivity — habit_entries must reject EFFORT_AWARE habit
        try {
            repository.logHabitEntrySync("h_eff", LocalDate.of(2026, 3, 15), 1)
            fail("Expected trigger to reject habit_entries for EFFORT_AWARE habit")
        } catch (_: SQLiteException) {
            // Expected
        }

        // AC-018: Mode exclusivity — habit_day_outcomes must reject QUANTITY habit
        try {
            repository.logHabitDayOutcomeSync("h_qty", LocalDate.of(2026, 3, 15), "COMPLETED", null)
            fail("Expected trigger to reject habit_day_outcomes for QUANTITY habit")
        } catch (_: SQLiteException) {
            // Expected
        }

        // AC-019: Cross-table idempotency_key uniqueness
        val sharedKey = "shared_idem_key_123"
        repository.logHabitEntrySync("h_qty", LocalDate.of(2026, 3, 15), 2, idempotencyKey = sharedKey)
        try {
            repository.logHabitDayOutcomeSync(
                habitId = "h_eff",
                date = LocalDate.of(2026, 3, 15),
                outcomeKind = "EFFORT_LOGGED",
                effortLevel = "TINY",
                idempotencyKey = sharedKey
            )
            fail("Expected cross-table trigger to reject duplicate idempotency_key across tables")
        } catch (_: SQLiteException) {
            // Expected
        }
    }

    @Test
    fun phase1_verifyV1ToV2MigrationAndFailClosedOnSkippedRow() {
        val migDbName = "test_mig_${UUID.randomUUID()}.db"
        try {
            // 1. Build a valid v1 schema database and migrate to v2
            val v1Helper = object : SQLiteOpenHelper(context, migDbName, null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, tiny_desc TEXT, usual_desc TEXT, extra_desc TEXT, schedule_days TEXT NOT NULL, is_archived INTEGER NOT NULL DEFAULT 0, color_hex TEXT NOT NULL, created_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE daily_records (id TEXT PRIMARY KEY, habit_id TEXT NOT NULL, record_date TEXT NOT NULL, outcome TEXT NOT NULL, effort_level TEXT NOT NULL, note TEXT, updated_at INTEGER NOT NULL, UNIQUE(habit_id, record_date))")
                    db.execSQL("CREATE TABLE journal_entries (id TEXT PRIMARY KEY, entry_date TEXT NOT NULL, prompt TEXT, content TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE app_preferences (pref_key TEXT PRIMARY KEY, pref_value TEXT NOT NULL)")

                    db.execSQL("INSERT INTO habits VALUES ('h1', 'Read', 'Books', 'EFFORT_LEVELS', '1 page', '10 pages', '1 chapter', 'MONDAY,WEDNESDAY,FRIDAY', 0, '#286143', 1000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r1', 'h1', '2026-03-10', 'COMPLETED', 'TINY', 'Good start', 2000)")
                    db.execSQL("INSERT INTO journal_entries VALUES ('j1', '2026-03-10', 'Prompt', 'My reflection body', 3000, 3000)")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            v1Helper.writableDatabase.close()

            val v2Helper = DayseedDatabaseHelper(context, migDbName)
            val upgradedDb = v2Helper.writableDatabase
            upgradedDb.rawQuery("SELECT COUNT(*) FROM habit_versions WHERE habit_id = 'h1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            upgradedDb.rawQuery("SELECT outcome_kind, effort_level FROM habit_day_outcomes WHERE habit_id = 'h1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("EFFORT_LOGGED", c.getString(0))
                assertEquals("TINY", c.getString(1))
            }
            upgradedDb.rawQuery("SELECT body FROM habit_date_notes WHERE habit_id = 'h1'", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Good start", c.getString(0))
            }
            v2Helper.close()
        } finally {
            context.deleteDatabase(migDbName)
        }

        // 2. Verify fail-closed migration when v1 database has legacy SKIPPED row (D-197 / AC-021)
        val blockedDbName = "test_mig_blocked_${UUID.randomUUID()}.db"
        try {
            val badV1Helper = object : SQLiteOpenHelper(context, blockedDbName, null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE habits (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT, tracking_mode TEXT NOT NULL, tiny_desc TEXT, usual_desc TEXT, extra_desc TEXT, schedule_days TEXT NOT NULL, is_archived INTEGER NOT NULL DEFAULT 0, color_hex TEXT NOT NULL, created_at INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE daily_records (id TEXT PRIMARY KEY, habit_id TEXT NOT NULL, record_date TEXT NOT NULL, outcome TEXT NOT NULL, effort_level TEXT NOT NULL, note TEXT, updated_at INTEGER NOT NULL, UNIQUE(habit_id, record_date))")
                    db.execSQL("INSERT INTO habits VALUES ('h1', 'Read', '', 'EFFORT_LEVELS', '', '', '', 'MONDAY', 0, '#286143', 1000)")
                    db.execSQL("INSERT INTO daily_records VALUES ('r_skip', 'h1', '2026-03-11', 'SKIPPED', 'STANDARD', '', 2000)")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            badV1Helper.writableDatabase.close()

            val blockedV2Helper = DayseedDatabaseHelper(context, blockedDbName)
            try {
                blockedV2Helper.writableDatabase
                fail("Expected DayseedMigrationBlockedException when migrating v1 SKIPPED row")
            } catch (e: DayseedMigrationBlockedException) {
                assertTrue(e.message!!.contains("SKIPPED"))
            }
            blockedV2Helper.close()
        } finally {
            context.deleteDatabase(blockedDbName)
        }
    }

    // =========================================================================
    // PHASE 2: PROGRESS BANDS, MULTI-ENTRY, D-194, UNDO & GROWTH MARK (AC-001..006, 023, 025)
    // =========================================================================

    @Test
    fun phase2_verifyProgressBandBoundariesAndTinyTargetUnreachableSkipping() {
        // Target = 100 -> exact percentage boundaries (AC-002)
        assertEquals(ProgressBand.ZERO, ProgressBandClassifier.classify(0, 100))
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(1, 100))
        assertEquals(ProgressBand.TINY_STEP, ProgressBandClassifier.classify(24, 100))
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(25, 100))
        assertEquals(ProgressBand.GETTING_STARTED, ProgressBandClassifier.classify(49, 100))
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(50, 100))
        assertEquals(ProgressBand.HALFWAY, ProgressBandClassifier.classify(74, 100))
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(75, 100))
        assertEquals(ProgressBand.ALMOST_THERE, ProgressBandClassifier.classify(99, 100))
        assertEquals(ProgressBand.GOAL_MET, ProgressBandClassifier.classify(100, 100))
        assertEquals(ProgressBand.EXCEEDED, ProgressBandClassifier.classify(101, 100))

        // Tiny integer targets skip unreachable intermediate bands cleanly (D-040)
        // Target = 1 -> reachable: ZERO (0), GOAL_MET (1), EXCEEDED (2)
        assertEquals(
            listOf(ProgressBand.ZERO, ProgressBand.GOAL_MET, ProgressBand.EXCEEDED),
            ProgressBandClassifier.reachableBandsForTarget(1)
        )
        // Target = 2 -> reachable: ZERO (0), HALFWAY (1), GOAL_MET (2), EXCEEDED (3)
        assertEquals(
            listOf(ProgressBand.ZERO, ProgressBand.HALFWAY, ProgressBand.GOAL_MET, ProgressBand.EXCEEDED),
            ProgressBandClassifier.reachableBandsForTarget(2)
        )
        // Target = 3 -> reachable: ZERO (0), GETTING_STARTED (1 -> 33%),HALFWAY (2 -> 66%), GOAL_MET (3 -> 100%), EXCEEDED
        assertEquals(
            listOf(ProgressBand.ZERO, ProgressBand.GETTING_STARTED, ProgressBand.HALFWAY, ProgressBand.GOAL_MET, ProgressBand.EXCEEDED),
            ProgressBandClassifier.reachableBandsForTarget(3)
        )
    }

    @Test
    fun phase2_verifyD194ApplyTodayVsApplyTomorrowAndDerivedDayStatus() {
        val today = testClock.todayLocal() // 2026-03-15
        val habit = Habit(
            habitId = "h_read",
            name = "Reading",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20
        )
        repository.saveHabitSync(habit, effectiveDateOverride = today.minusDays(5))

        // Log 10 pages today -> PARTIAL (50% -> HALFWAY)
        repository.logHabitEntrySync("h_read", today, 10)
        assertEquals(DerivedDayStatus.PARTIAL, repository.evaluateDerivedDayStatus("h_read", today))

        // Edit target to 10 pages with applyTodayWhenLogged = false (Apply tomorrow: 2026-03-16)
        val updatedHabit = habit.copy(targetValue = 10)
        val verTomorrow = repository.saveHabitSync(updatedHabit, applyTodayWhenLogged = false)
        assertEquals(today.plusDays(1).toString(), verTomorrow.effectiveFromLocalDate)

        // Today still uses target = 20 (so 10 pages is still PARTIAL)
        assertEquals(20, repository.getEffectiveHabitVersion("h_read", today)?.targetValue)
        assertEquals(DerivedDayStatus.PARTIAL, repository.evaluateDerivedDayStatus("h_read", today))
        // Tomorrow uses target = 10
        assertEquals(10, repository.getEffectiveHabitVersion("h_read", today.plusDays(1))?.targetValue)

        // Past scheduled day without entries is DERIVED_MISSED, while today without entries is OPEN_TODAY
        assertEquals(DerivedDayStatus.DERIVED_MISSED, repository.evaluateDerivedDayStatus("h_read", today.minusDays(2)))
    }

    @Test
    fun phase2_verify8SecondGuardedUndoAndGrowthMarkG2G3() {
        val today = testClock.todayLocal()
        val durHabit = Habit(
            habitId = "h_meditate",
            name = "Meditate",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 20
        )
        val hrHabit = Habit(
            habitId = "h_deep_work",
            name = "Deep Work",
            trackingMode = TrackingMode.DURATION,
            unitCode = "hr",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 2
        )
        val qtyHabit = Habit(
            habitId = "h_water",
            name = "Water",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "glasses",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 8
        )
        repository.saveHabitSync(durHabit)
        repository.saveHabitSync(hrHabit)
        repository.saveHabitSync(qtyHabit)

        // Log 25 min + 1 hr (60 min) + 4 glasses (non-duration)
        repository.logHabitEntrySync("h_meditate", today, 25)
        val (_, undoTokenHr) = repository.logHabitEntrySync("h_deep_work", today, 1)
        val (waterEntry, undoTokenWater) = repository.logHabitEntrySync("h_water", today, 4)

        // Verify G2 (25 + 60 = 85 minutes; quantity habit excluded from G2) and G3 (3 sessions on today)
        val metrics = repository.growthMetrics.value
        assertEquals(85L, metrics.currentMonthDurationMinutes)
        assertEquals("1h 25m", DurationNormalizer.formatTotalMinutes(metrics.currentMonthDurationMinutes))
        assertEquals(3, metrics.currentMonthDateSessionCounts[today])

        // AC-023: Intervening edit on waterEntry invalidates undoTokenWater
        testClock.advanceMillis(1_000L)
        repository.updateHabitEntrySync(waterEntry.entryId, 6, "Updated")
        assertFalse("Stale undo token must fail when row updated_at_utc_ms changed", repository.executeUndoSync(undoTokenWater))

        // AC-023: Expired undo token (> 8000ms) fails
        testClock.advanceMillis(9_000L)
        assertFalse("Undo token after 8s must fail", repository.executeUndoSync(undoTokenHr))

        // AC-023: Fresh undo token within 8s succeeds
        val (_, freshToken) = repository.logHabitEntrySync("h_meditate", today, 15)
        testClock.advanceMillis(3_000L)
        assertTrue("Fresh undo token within 8s must succeed", repository.executeUndoSync(freshToken))
        assertEquals(85L, repository.growthMetrics.value.currentMonthDurationMinutes)
    }

    // =========================================================================
    // PHASE 3: MICRO-JOURNAL, ATOMIC DRAFT PROMOTION & D-042 PROMPTS (AC-007, 008, 024)
    // =========================================================================

    @Test
    fun phase3_verifyAtomicDraftPromotionDateIsolationAndRetrospectivePrompt() {
        val today = testClock.todayLocal()
        val habit = Habit(
            habitId = "h_stretch",
            name = "Evening Stretch",
            trackingMode = TrackingMode.EFFORT_AWARE
        )
        repository.saveHabitSync(habit, effectiveDateOverride = today.minusDays(10))

        // AC-008: Autosave draft -> intended_journal_entry_id is null while typing
        val draft = repository.saveJournalDraftSync(
            draftId = "draft_today",
            localDate = today,
            title = "Quiet Morning",
            body = "Noticed the rain outside.",
            primaryMoodCode = "CALM",
            secondaryMoodCode = "GRATEFUL",
            linkedHabitId = "h_stretch"
        )
        assertNull(draft.intendedJournalEntryId)
        assertEquals(1, repository.journalDrafts.value.size)

        // Promote draft to journal_entries atomically
        val promoted = repository.promoteDraftToJournalEntrySync("draft_today")
        assertNotNull(promoted)
        assertEquals("Quiet Morning", promoted!!.title)
        assertEquals("CALM", promoted.primaryMoodCode)
        assertEquals("GRATEFUL", promoted.secondaryMoodCode)
        assertEquals(0, repository.journalDrafts.value.size)
        assertEquals(1, repository.journalEntries.value.size)

        // AC-007: Create a second entry and verify editing its date does not mutate the first entry
        val secondEntry = repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_second",
                localDate = today.toString(),
                body = "Second thought",
                primaryMoodCode = "FOCUSED"
            )
        )
        repository.saveJournalEntrySync(secondEntry.copy(localDate = today.minusDays(1).toString()))
        val firstReloaded = repository.journalEntries.value.first { it.journalEntryId == promoted.journalEntryId }
        val secondReloaded = repository.journalEntries.value.first { it.journalEntryId == "j_second" }
        assertEquals(today.toString(), firstReloaded.localDate)
        assertEquals(today.minusDays(1).toString(), secondReloaded.localDate)

        // AC-024: D-042 Retrospective prompt eligibility (>= 2 derived missed days in past 7 days)
        assertTrue(repository.isHabitEligibleForRetrospectivePrompt("h_stretch", today))
        repository.dismissRetrospectivePromptSync("h_stretch", today)
        assertFalse("Cooldown must suppress prompt for 7 days after dismissal", repository.isHabitEligibleForRetrospectivePrompt("h_stretch", today.plusDays(3)))
        assertTrue("Prompt becomes eligible again after 7 days", repository.isHabitEligibleForRetrospectivePrompt("h_stretch", today.plusDays(7)))
    }

    // =========================================================================
    // PHASE 4: COMEBACK 14-DAY & 30-DAY ENGINE (AC-011)
    // =========================================================================

    @Test
    fun phase4_verifyComeback14DayAnd30DayBoundariesAndPrecedence() {
        val baseDate = LocalDate.of(2026, 3, 1)
        testClock.currentLocalDate = baseDate
        val habit = Habit(
            habitId = "h_walk",
            name = "Morning Walk",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetValue = 30
        )
        repository.saveHabitSync(habit, effectiveDateOverride = baseDate)
        repository.logHabitEntrySync("h_walk", baseDate, 30)

        // 13 days later (2026-03-14): not eligible
        testClock.currentLocalDate = baseDate.plusDays(13)
        assertNull(repository.evaluateComebackSync(testClock.currentLocalDate))

        // 14 days later (2026-03-15): eligible for DAYS_14
        testClock.currentLocalDate = baseDate.plusDays(14)
        val info14 = repository.evaluateComebackSync(testClock.currentLocalDate)
        assertNotNull(info14)
        assertEquals(ComebackTier.DAYS_14, info14!!.tier)
        assertEquals(14, info14.daysAway)

        // 29 days later (2026-03-30): still DAYS_14
        testClock.currentLocalDate = baseDate.plusDays(29)
        val info29 = repository.evaluateComebackSync(testClock.currentLocalDate)
        assertNotNull(info29)
        assertEquals(ComebackTier.DAYS_14, info29!!.tier)

        // 30 days later (2026-03-31): DAYS_30 supersedes DAYS_14
        testClock.currentLocalDate = baseDate.plusDays(30)
        val info30 = repository.evaluateComebackSync(testClock.currentLocalDate)
        assertNotNull(info30)
        assertEquals(ComebackTier.DAYS_30, info30!!.tier)
        assertEquals(30, info30.daysAway)

        // Apply adaptive adjustments (lower target to 15 min + save returning reflection)
        repository.applyComebackAdjustmentsSync(
            resumeHabitIds = setOf("h_walk"),
            pauseHabitIds = emptySet(),
            loweredTargetByHabitId = mapOf("h_walk" to 15),
            comebackReflectionBody = "Starting back with 15 minutes.",
            saveReflectionImmediately = true
        )
        assertEquals(15, repository.getEffectiveHabitVersion("h_walk", testClock.currentLocalDate)?.targetValue)
        assertTrue(repository.journalEntries.value.any { it.body.contains("Starting back with 15 minutes.") })

        // Once-per-episode suppression: re-evaluating on the same episode returns null
        assertNull(repository.evaluateComebackSync(testClock.currentLocalDate))
    }

    // =========================================================================
    // PHASE 5: MOOD INSIGHTS J1-J11 FIXTURE & REMINDERS / OUTBOX (AC-009, 012, 026)
    // =========================================================================

    @Test
    fun phase5_verifyMoodInsightsExactJ1ToJ11FixtureForMondayAndSundayWeekStart() {
        // Exact fixture J1-J11 from docs/07-TESTING-AND-QUALITY.md Section 5.1
        // Reference date: Sunday 2026-03-15
        testClock.currentLocalDate = LocalDate.of(2026, 3, 15)

        val fixtureEntries = listOf(
            JournalEntry("J1", "2026-03-01", "Body J1", primaryMoodCode = "TIRED", secondaryMoodCode = null, createdAtUtcMs = 1001L, updatedAtUtcMs = 1001L),
            JournalEntry("J2", "2026-03-03", "Body J2", primaryMoodCode = "CALM", secondaryMoodCode = "GRATEFUL", createdAtUtcMs = 1002L, updatedAtUtcMs = 1002L),
            JournalEntry("J3", "2026-03-07", "Body J3", primaryMoodCode = "STRESSED", secondaryMoodCode = "ANXIOUS", createdAtUtcMs = 1003L, updatedAtUtcMs = 1003L),
            JournalEntry("J4", "2026-03-08", "Body J4", primaryMoodCode = "HOPEFUL", secondaryMoodCode = "CALM", createdAtUtcMs = 1004L, updatedAtUtcMs = 1004L),
            JournalEntry("J5", "2026-03-09", "Body J5", primaryMoodCode = "CALM", secondaryMoodCode = "FOCUSED", createdAtUtcMs = 1005L, updatedAtUtcMs = 1005L),
            JournalEntry("J6", "2026-03-09", "Body J6", primaryMoodCode = "ENERGISED", secondaryMoodCode = null, createdAtUtcMs = 1006L, updatedAtUtcMs = 1006L),
            JournalEntry("J7", "2026-03-11", "Body J7", primaryMoodCode = "CALM", secondaryMoodCode = "CONTENT", createdAtUtcMs = 1007L, updatedAtUtcMs = 1007L),
            JournalEntry("J8", "2026-03-12", "Body J8", primaryMoodCode = null, secondaryMoodCode = null, createdAtUtcMs = 1008L, updatedAtUtcMs = 1008L),
            JournalEntry("J9", "2026-03-14", "Body J9", primaryMoodCode = "GRATEFUL", secondaryMoodCode = "CALM", createdAtUtcMs = 1009L, updatedAtUtcMs = 1009L),
            JournalEntry("J10", "2026-03-15", "Body J10", primaryMoodCode = "STRESSED", secondaryMoodCode = null, createdAtUtcMs = 1010L, updatedAtUtcMs = 1010L),
            JournalEntry("J11", "2026-02-28", "Body J11", primaryMoodCode = "LOW", secondaryMoodCode = null, createdAtUtcMs = 1011L, updatedAtUtcMs = 1011L)
        )
        fixtureEntries.forEach { repository.saveJournalEntrySync(it) }

        // 1. Monday week-start: current week = 2026-03-09..2026-03-15 (J5..J10 -> 5 primary-mood entries)
        // Previous week = 2026-03-02..2026-03-08 (J2, J3, J4 -> 3 primary-mood entries)
        repository.setSettingSync("week_start", "ENUM", "MONDAY")
        val mondayWeek = repository.computeMoodInsights("WEEK", LocalDate.of(2026, 3, 15))
        assertEquals(5, mondayWeek.totalEntriesWithPrimaryMood)
        assertEquals(3, mondayWeek.previousPeriodTotalEntries)

        val monByCode = mondayWeek.distribution.associateBy { it.moodCode }
        assertEquals(2, monByCode["CALM"]?.count)
        assertEquals(40, monByCode["CALM"]?.percentage)
        assertEquals(1, monByCode["ENERGISED"]?.count)
        assertEquals(20, monByCode["ENERGISED"]?.percentage)
        assertEquals(1, monByCode["GRATEFUL"]?.count)
        assertEquals(20, monByCode["GRATEFUL"]?.percentage)
        assertEquals(1, monByCode["STRESSED"]?.count)
        assertEquals(20, monByCode["STRESSED"]?.percentage)

        // 2. Sunday week-start: current week = 2026-03-15..2026-03-21 if reference is Sunday 2026-03-15,
        // or for Saturday 2026-03-14 (window 2026-03-08..2026-03-14 -> J4, J5, J6, J7, J9 = 5 entries;
        // let's check both 2026-03-14 and 2026-03-15)
        repository.setSettingSync("week_start", "ENUM", "SUNDAY")
        val sundayWindowMar8To14 = repository.computeMoodInsights("WEEK", LocalDate.of(2026, 3, 14))
        assertEquals(LocalDate.of(2026, 3, 8), sundayWindowMar8To14.startDate)
        assertEquals(LocalDate.of(2026, 3, 14), sundayWindowMar8To14.endDate)
        assertEquals(5, sundayWindowMar8To14.totalEntriesWithPrimaryMood) // J4, J5, J6, J7, J9

        // 3. This Month (March 2026): J1..J10 -> 9 entries with primary mood (J8 has null mood, J11 is Feb)
        val marchMonth = repository.computeMoodInsights("MONTH", LocalDate.of(2026, 3, 15))
        assertEquals(9, marchMonth.totalEntriesWithPrimaryMood)
        assertEquals(1, marchMonth.previousPeriodTotalEntries) // J11 in February
    }

    @Test
    fun phase5_verifyRemindersQuietHoursCapAndHabitDeleteOutbox() {
        val today = testClock.todayLocal()
        // Create 4 habits with reminders enabled: 1 inside quiet hours (22:30) and 3 in daytime (09:00, 12:00, 15:00, 18:00)
        val habits = listOf(
            Habit(habitId = "h_quiet", name = "Late Habit", trackingMode = TrackingMode.COMPLETION_ONLY, reminderMode = "AT_TIME", reminderTimeLocal = "22:30"),
            Habit(habitId = "h_1", name = "Habit 1", trackingMode = TrackingMode.COMPLETION_ONLY, reminderMode = "AT_TIME", reminderTimeLocal = "09:00"),
            Habit(habitId = "h_2", name = "Habit 2", trackingMode = TrackingMode.COMPLETION_ONLY, reminderMode = "AT_TIME", reminderTimeLocal = "12:00"),
            Habit(habitId = "h_3", name = "Habit 3", trackingMode = TrackingMode.COMPLETION_ONLY, reminderMode = "AT_TIME", reminderTimeLocal = "15:00"),
            Habit(habitId = "h_4", name = "Habit 4", trackingMode = TrackingMode.COMPLETION_ONLY, reminderMode = "AT_TIME", reminderTimeLocal = "18:00")
        )
        habits.forEach { repository.saveHabitSync(it) }

        val evals = repository.evaluateTodayRemindersSync(today).associateBy { it.habitId }
        assertEquals("SUPPRESSED", evals["h_quiet"]?.deliveryStatus)
        assertEquals("QUIET_HOURS", evals["h_quiet"]?.suppressionReason)
        assertEquals("DELIVERED", evals["h_1"]?.deliveryStatus)
        assertEquals("DELIVERED", evals["h_2"]?.deliveryStatus)
        assertEquals("DELIVERED", evals["h_3"]?.deliveryStatus)
        assertEquals("SUPPRESSED", evals["h_4"]?.deliveryStatus)
        assertEquals("DAILY_CAP_REACHED", evals["h_4"]?.suppressionReason)

        // AC-026: Link a journal entry to h_1, then permanently delete h_1
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_linked_h1",
                localDate = today.toString(),
                body = "Linked reflection",
                linkedHabitId = "h_1"
            )
        )
        repository.deleteHabitPermanentlySync("h_1")

        // Verify h_1 is gone, journal_entries row is preserved with linked_habit_id = NULL, and outbox row was processed to COMPLETED
        assertFalse(repository.habits.value.any { it.habitId == "h_1" })
        val preservedJournal = repository.journalEntries.value.find { it.journalEntryId == "j_linked_h1" }
        assertNotNull(preservedJournal)
        assertNull("linked_habit_id must be NULL after ON DELETE SET NULL", preservedJournal!!.linkedHabitId)

        val db = repository.dbHelper.readableDatabase
        db.rawQuery("SELECT status, attempt_count FROM notification_cancel_outbox", null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("COMPLETED", c.getString(0))
            assertEquals(1, c.getInt(1))
        }
    }

    // =========================================================================
    // PHASE 6 & 7: EXPORT, ENCRYPTED BACKUP, MERGE CONFLICTS & THEME (AC-013..015, 022)
    // =========================================================================

    @Test
    fun phase6_verifyReadableJournalExportExcludesDraftsAndEncryptedBackupMergeConflicts() {
        val today = testClock.todayLocal()
        val habit = Habit(
            habitId = "h_journal_test",
            name = "Morning Pages",
            trackingMode = TrackingMode.COMPLETION_ONLY
        )
        repository.saveHabitSync(habit)
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_saved_1",
                localDate = today.toString(),
                title = "Saved Title",
                body = "Saved journal body text",
                primaryMoodCode = "CALM"
            )
        )
        repository.saveJournalDraftSync(
            draftId = "draft_unsaved_1",
            localDate = today,
            body = "SECRET_UNSAVED_DRAFT_TEXT"
        )

        // AC-013: Readable exports contain saved journal entry and never contain unsaved draft text
        val mdExport = repository.exportReadableJournalsSync(JournalExportFormat.MARKDOWN)
        val csvExport = repository.exportReadableJournalsSync(JournalExportFormat.CSV)
        val jsonExport = repository.exportReadableJournalsSync(JournalExportFormat.JSON)
        assertTrue(mdExport.contains("Saved journal body text"))
        assertFalse(mdExport.contains("SECRET_UNSAVED_DRAFT_TEXT"))
        assertTrue(csvExport.contains("Saved journal body text"))
        assertFalse(csvExport.contains("SECRET_UNSAVED_DRAFT_TEXT"))
        assertTrue(jsonExport.contains("Saved journal body text"))
        assertFalse(jsonExport.contains("SECRET_UNSAVED_DRAFT_TEXT"))

        // AC-014: Export encrypted portable backup (.dayseed.json) with AES-256-GCM
        val encryptedBackup = repository.exportPortableBackupSync(password = "correct-horse-battery-staple")
        assertTrue(encryptedBackup.contains("dayseed_encrypted_backup_v1"))
        assertFalse(encryptedBackup.contains("Saved journal body text"))

        // Wrong password must fail cleanly without mutating live data
        try {
            repository.restorePortableBackupSync(encryptedBackup, restoreMode = "REPLACE", password = "wrong-password")
            fail("Expected decryption failure with wrong password")
        } catch (_: Exception) {
            // Expected
        }

        // Modify live journal entry and live habit version to trigger MERGE conflicts (AC-022)
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_saved_1",
                localDate = today.toString(),
                title = "Modified Live Title",
                body = "Modified live journal body",
                primaryMoodCode = "FOCUSED"
            )
        )
        repository.saveHabitSync(habit.copy(name = "Renamed Live Habit"))

        // Run MERGE restore with correct password
        val session = repository.restorePortableBackupSync(
            rawBackupPayload = encryptedBackup,
            restoreMode = "MERGE",
            password = "correct-horse-battery-staple"
        )
        assertEquals("AWAITING_CONFLICT_RESOLUTION", session.sessionState)
        assertEquals(1, session.safetyBackupVerified)

        val conflicts = repository.restoreConflicts.value
        assertEquals(2, conflicts.size)

        val journalConflict = conflicts.first { it.recordType == "journal_entry" }
        val habitVerConflict = conflicts.first { it.recordType == "habit_version" }

        // AC-022: KEEP_BOTH is allowed for journal_entry, and forbidden for habit_version
        assertTrue(ConflictResolution.KEEP_BOTH in journalConflict.allowedResolutions())
        assertFalse(ConflictResolution.KEEP_BOTH in habitVerConflict.allowedResolutions())

        try {
            repository.resolveRestoreConflictSync(habitVerConflict.conflictId, ConflictResolution.KEEP_BOTH)
            fail("Expected IllegalArgumentException for KEEP_BOTH on habit_version")
        } catch (_: IllegalArgumentException) {
            // Expected
        }

        // Resolve journal conflict with KEEP_BOTH -> results in 2 saved journal entries
        repository.resolveRestoreConflictSync(journalConflict.conflictId, ConflictResolution.KEEP_BOTH)
        // Resolve habit_version conflict with USE_BACKUP -> restores "Morning Pages"
        repository.resolveRestoreConflictSync(habitVerConflict.conflictId, ConflictResolution.USE_BACKUP)

        assertEquals(0, repository.restoreConflicts.value.size)
        assertEquals(2, repository.journalEntries.value.size)
        assertEquals("Morning Pages", repository.habits.value.first().name)

        // AC-015: Delete all personal data clears all tables and resets onboarding_completed to false
        repository.deleteAllDataSync()
        assertEquals(0, repository.habits.value.size)
        assertEquals(0, repository.journalEntries.value.size)
        assertEquals("false", repository.getSettingString("onboarding_completed", "true"))
    }

    @Test
    fun phase7_verifySoftBotanicalDaypartAndFixedThemeResolution() {
        assertEquals(BotanicalDaypart.MORNING, SoftBotanicalThemeConfig.resolveDaypart(LocalTime.of(7, 30)))
        assertEquals(BotanicalDaypart.DAY, SoftBotanicalThemeConfig.resolveDaypart(LocalTime.of(13, 0)))
        assertEquals(BotanicalDaypart.EVENING, SoftBotanicalThemeConfig.resolveDaypart(LocalTime.of(19, 0)))
        assertEquals(BotanicalDaypart.NIGHT, SoftBotanicalThemeConfig.resolveDaypart(LocalTime.of(22, 30)))

        val (_, isDarkNight) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, LocalTime.of(23, 0))
        assertTrue(isDarkNight)
        val (_, isDarkMorning) = resolveDayseedColorScheme(ThemeModeOption.SOFT_BOTANICAL_AUTO, true, LocalTime.of(8, 0))
        assertFalse(isDarkMorning)
    }

    private fun getTableColumns(db: SQLiteDatabase, tableName: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($tableName)", null).use { c ->
            val nameIdx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) {
                cols.add(c.getString(nameIdx))
            }
        }
        return cols
    }
}
