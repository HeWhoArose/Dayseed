package com.example.dayseed

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.BackupLimits
import com.example.dayseed.data.BackupRejectedException
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.readBackupFileBounded
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Backup-capacity validation of the 8 MiB import limit (R2 work item recorded in the
 * [BackupLimits.MAX_BACKUP_SIZE_BYTES] comment, D-196 low-risk default review).
 *
 * Method (deterministic, no timing dependence):
 *  - Four synthetic long-term user datasets are inserted DIRECTLY into the production
 *    SQLite schema via ContentValues bulk inserts inside one transaction (bypassing the
 *    per-row repository APIs, whose per-insert refreshAllSync is O(n) each and would make
 *    a 73k-row dataset O(n^2)). One [DayseedRepository.refreshAllSync] then loads every
 *    table into the StateFlows, so [DayseedRepository.exportPortableBackupSync] — the
 *    production serializer used for real exports — serialises exactly what a real user
 *    device would contain. This bulk-insert bypass is a measurement-instrumentation
 *    choice only; the EXPORTED payload and the RESTORE path are the production code.
 *  - The exported UTF-8 byte size is measured and printed as a CAPRESULT line for the
 *    capacity report, then classified against [BackupLimits.MAX_BACKUP_SIZE_BYTES].
 *  - Round-trip integrity: datasets at or under the limit are restored with the
 *    production REPLACE path and every table count (plus first/last entry spot checks)
 *    must match the pre-restore snapshot. Datasets over the limit exercise the real
 *    user-visible failure: the pre-restore safety backup file exceeds the bounded read
 *    cap, so restore must fail with [BackupRejectedException] WITHOUT mutating data.
 *  - [readBackupFileBounded] boundary behaviour is pinned below/at/above the limit.
 *
 * The stress dataset (20 habits x ~10 years) is export-measurement only: restore round
 * trips at that scale are memory-bound in the JVM test sandbox, and the restore code
 * path is identical for every dataset (already proven at the heavy scale).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCapacityValidationTest {

    private lateinit var app: Application
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    private val limit: Long = BackupLimits.MAX_BACKUP_SIZE_BYTES

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 18), // Wednesday
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = LocalDate.of(2026, 3, 18)
                .atTime(10, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        repository = DayseedRepository(
            context = app,
            clock = testClock,
            databaseName = null
        )
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    // =========================================================================
    // Deterministic synthetic dataset builder (bulk insert bypass)
    // =========================================================================

    private class DatasetSpec(
        val name: String,
        val habitCount: Int,
        val days: Int,
        val journalCount: Int,
        /** One habit_date_note per habit every Nth covered day. */
        val notesEveryNthCoveredDay: Int,
        val startDate: LocalDate
    )

    private class DatasetResult(
        val habits: Int,
        val versions: Int,
        val entries: Int,
        val outcomes: Int,
        val notes: Int,
        val journal: Int
    )

    private fun insertDataset(spec: DatasetSpec): DatasetResult {
        val db = repository.dbHelper.writableDatabase
        val baseCreatedMs = 1_600_000_000_000L // fixed deterministic base timestamp
        var msCounter = 0L
        var entriesTotal = 0
        var outcomesTotal = 0
        var notesTotal = 0

        db.beginTransaction()
        try {
            for (h in 0 until spec.habitCount) {
                val habitId = "h_cap_%03d".format(h)
                val createdAtMs = baseCreatedMs + h
                val isQuantity = h % 2 == 0

                // habits
                val habitCv = ContentValues().apply {
                    put("habit_id", habitId)
                    put("created_at_utc_ms", createdAtMs)
                    put("updated_at_utc_ms", createdAtMs)
                    putNull("category")
                    putNull("lifetime_goal_days")
                }
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABITS, null, habitCv, SQLiteDatabase.CONFLICT_NONE
                )

                // habit_versions (one version per habit, DAILY schedule)
                val versionCv = ContentValues().apply {
                    put("version_id", "v_cap_%03d_0".format(h))
                    put("habit_id", habitId)
                    put("effective_from_local_date", spec.startDate.toString())
                    put("name", "Capacity Habit %03d".format(h))
                    putNull("description")
                    if (isQuantity) {
                        put("tracking_mode", "QUANTITY")
                        put("unit_code", "reps")
                        put("target_value", 30)
                    } else {
                        put("tracking_mode", "COMPLETION_ONLY")
                        putNull("unit_code")
                        putNull("target_value")
                    }
                    put("target_period", "DAY")
                    put("schedule_kind", "DAILY")
                    putNull("scheduled_weekdays_mask")
                    putNull("preferred_weekdays_mask")
                    put("lifecycle_state", "ACTIVE")
                    put("reminder_mode", "OFF")
                    putNull("reminder_time_local")
                    put("tiny_description", "Deliberately small action")
                    put("usual_description", "The ordinary intended action")
                    put("extra_description", "Optional stretch action")
                    put("color_hex", "#286143")
                    put("sort_order", h)
                    put("created_at_utc_ms", createdAtMs)
                }
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, versionCv, SQLiteDatabase.CONFLICT_NONE
                )

                // ~6/7 days covered (every 7th day missed) -> deterministic gaps
                var coveredIndex = 0
                for (d in 0 until spec.days) {
                    if (d % 7 == 6) continue // deterministic missed day
                    val date = spec.startDate.plusDays(d.toLong())
                    msCounter += 1
                    val rowCreated = baseCreatedMs + 1_000_000 + msCounter
                    if (isQuantity) {
                        val entryCv = ContentValues().apply {
                            put("entry_id", "e_%03d_%05d".format(h, d))
                            put("habit_id", habitId)
                            put("local_date", date.toString())
                            put("value_units", (d % 20) + 1)
                            if (d % 10 == 0) put("entry_note", "Session note for day %d of habit %03d".format(d, h))
                            else putNull("entry_note")
                            put("created_at_utc_ms", rowCreated)
                            put("updated_at_utc_ms", rowCreated)
                            put("idempotency_key", "idem-$habitId-$date")
                        }
                        db.insertWithOnConflict(
                            DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, entryCv, SQLiteDatabase.CONFLICT_NONE
                        )
                        entriesTotal++
                    } else {
                        val outcomeCv = ContentValues().apply {
                            put("outcome_id", "o_%03d_%05d".format(h, d))
                            put("habit_id", habitId)
                            put("local_date", date.toString())
                            put("outcome_kind", "COMPLETED")
                            putNull("effort_level")
                            put("idempotency_key", "idem-$habitId-$date")
                            put("created_at_utc_ms", rowCreated)
                            put("updated_at_utc_ms", rowCreated)
                        }
                        db.insertWithOnConflict(
                            DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, outcomeCv, SQLiteDatabase.CONFLICT_NONE
                        )
                        outcomesTotal++
                    }
                    if (coveredIndex % spec.notesEveryNthCoveredDay == 0) {
                        val noteCv = ContentValues().apply {
                            put("note_id", "n_%03d_%05d".format(h, d))
                            put("habit_id", habitId)
                            put("local_date", date.toString())
                            put(
                                "body",
                                "Reflection on progress for habit %03d on day %d: steady effort, small deliberate action completed as planned.".format(
                                    h,
                                    d
                                )
                            )
                            put("created_at_utc_ms", rowCreated)
                            put("updated_at_utc_ms", rowCreated)
                        }
                        db.insertWithOnConflict(
                            DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, noteCv, SQLiteDatabase.CONFLICT_NONE
                        )
                        notesTotal++
                    }
                    coveredIndex++
                }
            }

            // journal_entries (deterministic ~300-char bodies)
            for (j in 0 until spec.journalCount) {
                val date = spec.startDate.plusDays((j % spec.days).toLong())
                msCounter += 1
                val rowCreated = baseCreatedMs + 2_000_000 + msCounter
                val body = StringBuilder("Journal reflection number ").append(j).append(". ")
                val sentence = "Today the practice felt steady; small deliberate action, honest check-in, no drama. "
                while (body.length < 300) body.append(sentence)
                val journalCv = ContentValues().apply {
                    put("journal_entry_id", "j_cap_%05d".format(j))
                    put("local_date", date.toString())
                    put("body", body.toString().take(300))
                    put("title", "Reflection day %d".format(j))
                    put("primary_mood_code", "CALM")
                    putNull("secondary_mood_code")
                    putNull("prompt_id")
                    putNull("linked_habit_id")
                    put("created_at_utc_ms", rowCreated)
                    put("updated_at_utc_ms", rowCreated)
                }
                db.insertWithOnConflict(
                    DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, journalCv, SQLiteDatabase.CONFLICT_NONE
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        repository.refreshAllSync()
        return DatasetResult(
            habits = spec.habitCount,
            versions = spec.habitCount,
            entries = entriesTotal,
            outcomes = outcomesTotal,
            notes = notesTotal,
            journal = spec.journalCount
        )
    }

    // =========================================================================
    // Snapshot / round-trip helpers
    // =========================================================================

    private fun snapshotCounts(): Map<String, Int> = mapOf(
        "habits" to repository.loadHabitsSync().size,
        "habit_versions" to repository.habitVersions.value.size,
        "habit_entries" to repository.loadHabitEntriesSync().size,
        "habit_day_outcomes" to repository.habitDayOutcomes.value.size,
        "habit_date_notes" to repository.habitDateNotes.value.size,
        "journal_entries" to repository.loadJournalEntriesSync().size,
        "journal_drafts" to repository.journalDrafts.value.size,
        "habit_streak_runs" to repository.loadStreakRunsSync().size,
        "app_settings" to repository.appSettings.value.size
    )

    private fun exportAndMeasure(spec: DatasetSpec): Pair<DatasetResult, Long> {
        val insertStart = System.currentTimeMillis()
        val result = insertDataset(spec)
        val refreshMs = System.currentTimeMillis() - insertStart

        val exportStart = System.currentTimeMillis()
        val json = repository.exportPortableBackupSync()
        val exportMs = System.currentTimeMillis() - exportStart
        val bytes = json.toByteArray(Charsets.UTF_8).size.toLong()
        val classification = if (bytes <= limit) "UNDER_LIMIT" else "OVER_LIMIT"
        println(
            "CAPRESULT dataset=${spec.name} export_utf8_bytes=$bytes limit=$limit " +
                "classification=$classification habits=${result.habits} versions=${result.versions} " +
                "entries=${result.entries} outcomes=${result.outcomes} notes=${result.notes} " +
                "journal=${result.journal} insert_refresh_ms=$refreshMs export_ms=$exportMs"
        )
        return result to bytes
    }

    private fun restoreRoundTripAndAssert(json: String, password: String?, expected: DatasetResult) {
        val before = snapshotCounts()
        assertEquals(expected.habits, before["habits"])
        assertEquals(expected.entries, before["habit_entries"])
        // First and last entry of habit h_cap_000 for the post-restore spot check.
        val spotBefore = repository.loadHabitEntriesSync()
            .filter { it.habitId == "h_cap_000" }
            .sortedBy { it.localDate }
        assertTrue("dataset must contain entries for h_cap_000", spotBefore.isNotEmpty())

        val restoreStart = System.currentTimeMillis()
        val session = repository.restorePortableBackupSync(json, "REPLACE", password)
        val restoreMs = System.currentTimeMillis() - restoreStart
        println("CAPRESULT restore_ms=$restoreMs state=${session.sessionState}")
        assertEquals("COMPLETED", session.sessionState)

        val after = snapshotCounts()
        assertEquals("all table counts must survive the REPLACE round trip", before, after)

        val spotAfter = repository.loadHabitEntriesSync()
            .filter { it.habitId == "h_cap_000" }
            .sortedBy { it.localDate }
        assertEquals(spotBefore.size, spotAfter.size)
        assertEquals(spotBefore.first().valueUnits, spotAfter.first().valueUnits)
        assertEquals(spotBefore.first().entryNote, spotAfter.first().entryNote)
        assertEquals(spotBefore.last().valueUnits, spotAfter.last().valueUnits)
    }

    private fun assertOverLimitRestoreFailsWithoutMutatingData(json: String, before: Map<String, Int>) {
        val exception = assertThrows(BackupRejectedException::class.java) {
            repository.restorePortableBackupSync(json, "REPLACE", null)
        }
        println("CAPRESULT over_limit_restore_rejected message=\"${exception.message}\"")
        assertEquals(
            "over-limit restore must leave every table untouched",
            before,
            snapshotCounts()
        )
    }

    // =========================================================================
    // 1. Bounded-read limit boundary (the enforcement point of the 8 MiB cap)
    // =========================================================================

    @Test
    fun limitBoundaryBelowAtAbove8MiB() {
        assertEquals(8L * 1024 * 1024, BackupLimits.MAX_BACKUP_SIZE_BYTES)
        val dir = File(app.cacheDir, "cap_limit_boundary").apply { mkdirs() }
        fun fileOfSize(name: String, size: Long): File {
            val file = File(dir, name)
            FileOutputStream(file).use { out ->
                val chunk = ByteArray(8192) { 'x'.code.toByte() }
                var remaining = size
                while (remaining > 0) {
                    val n = minOf(chunk.size.toLong(), remaining).toInt()
                    out.write(chunk, 0, n)
                    remaining -= n
                }
            }
            return file
        }

        val below = fileOfSize("below.dayseed.json", limit - 1)
        val at = fileOfSize("at.dayseed.json", limit)
        val above = fileOfSize("above.dayseed.json", limit + 1)
        try {
            assertEquals(limit - 1, readBackupFileBounded(below).length.toLong())
            assertEquals(limit, readBackupFileBounded(at).length.toLong())
            val exception = assertThrows(BackupRejectedException::class.java) {
                readBackupFileBounded(above)
            }
            println("CAPRESULT boundary_above_rejected message=\"${exception.message}\"")
        } finally {
            below.delete(); at.delete(); above.delete()
        }
    }

    // =========================================================================
    // 2. Small dataset: 5 habits x 30 days (+5 journal) — fresh-install scale
    // =========================================================================

    @Test
    fun smallDataset5x30ExportRestores() {
        val spec = DatasetSpec(
            name = "small_5habits_30days",
            habitCount = 5,
            days = 30,
            journalCount = 5,
            notesEveryNthCoveredDay = 30,
            startDate = LocalDate.of(2026, 2, 16)
        )
        val (result, bytes) = exportAndMeasure(spec)
        if (bytes <= limit) {
            val json = repository.exportPortableBackupSync()
            restoreRoundTripAndAssert(json, password = null, expected = result)
        } else {
            fail("small dataset unexpectedly exceeded the limit: $bytes bytes")
        }
    }

    // =========================================================================
    // 3. Typical dataset: 10 habits x 365 days (+100 journal) — one year of use
    // =========================================================================

    @Test
    fun typicalDataset10x365ExportRestores() {
        val spec = DatasetSpec(
            name = "typical_10habits_365days",
            habitCount = 10,
            days = 365,
            journalCount = 100,
            notesEveryNthCoveredDay = 30,
            startDate = LocalDate.of(2025, 3, 18)
        )
        val (result, bytes) = exportAndMeasure(spec)
        if (bytes <= limit) {
            val json = repository.exportPortableBackupSync()
            restoreRoundTripAndAssert(json, password = null, expected = result)
        } else {
            fail("typical dataset unexpectedly exceeded the limit: $bytes bytes")
        }
    }

    // =========================================================================
    // 4. Typical dataset exported WITH a password (encrypted envelope overhead)
    // =========================================================================

    @Test
    fun typicalEncryptedExportRestores() {
        val spec = DatasetSpec(
            name = "typical_10habits_365days_encrypted",
            habitCount = 10,
            days = 365,
            journalCount = 100,
            notesEveryNthCoveredDay = 30,
            startDate = LocalDate.of(2025, 3, 18)
        )
        val (result, _) = exportAndMeasure(spec)
        val exportStart = System.currentTimeMillis()
        val encrypted = repository.exportPortableBackupSync(password = "correct horse battery staple 1")
        val exportMs = System.currentTimeMillis() - exportStart
        val bytes = encrypted.toByteArray(Charsets.UTF_8).size.toLong()
        val classification = if (bytes <= limit) "UNDER_LIMIT" else "OVER_LIMIT"
        println(
            "CAPRESULT dataset=${spec.name} export_utf8_bytes=$bytes limit=$limit " +
                "classification=$classification export_ms=$exportMs"
        )
        if (bytes <= limit) {
            restoreRoundTripAndAssert(encrypted, password = "correct horse battery staple 1", expected = result)
        } else {
            fail("encrypted typical dataset unexpectedly exceeded the limit: $bytes bytes")
        }
    }

    // =========================================================================
    // 5. Heavy dataset: 10 habits x ~3 years (+500 journal, notes)
    //    Measured, and restored exactly per the limit outcome: under the limit
    //    the REPLACE round trip must succeed; over the limit the restore must
    //    be rejected (safety-backup bounded read) WITHOUT mutating data.
    // =========================================================================

    @Test
    fun heavyDataset10x3yExportRestoresPerLimit() {
        val spec = DatasetSpec(
            name = "heavy_10habits_3years",
            habitCount = 10,
            days = 1095,
            journalCount = 500,
            notesEveryNthCoveredDay = 60,
            startDate = LocalDate.of(2023, 3, 19)
        )
        val (result, bytes) = exportAndMeasure(spec)
        val json = repository.exportPortableBackupSync()
        if (bytes <= limit) {
            restoreRoundTripAndAssert(json, password = null, expected = result)
        } else {
            assertOverLimitRestoreFailsWithoutMutatingData(json, snapshotCounts())
        }
    }

    // =========================================================================
    // 6. Stress dataset: 20 habits x ~10 years (+1000 journal). Export and
    //    size measurement only — the restore code path is identical for every
    //    dataset (already exercised at heavy scale) and a full REPLACE round
    //    trip at this scale is memory-bound in the JVM test sandbox.
    // =========================================================================

    @Test
    fun stressDataset20x10yExportMeasure() {
        val spec = DatasetSpec(
            name = "stress_20habits_10years",
            habitCount = 20,
            days = 3650,
            journalCount = 1000,
            notesEveryNthCoveredDay = 120,
            startDate = LocalDate.of(2016, 3, 18)
        )
        exportAndMeasure(spec)
    }
}
