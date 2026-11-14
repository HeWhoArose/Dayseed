package com.example.dayseed

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayRecordState
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.components.DAYSEED_BRAND_NAME
import com.example.dayseed.ui.components.DAYSEED_OFFICIAL_MOTTO
import com.example.dayseed.ui.screens.SettingsSheet
import com.example.dayseed.ui.theme.DayJournalSurface
import com.example.dayseed.ui.theme.DayseedTheme
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedArtworkBlend
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import com.example.dayseed.ui.theme.themeTransitionDurationMillis
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Comprehensive Robolectric regression test suite for the Canonical UI and
 * Product-Behaviour Reconciliation (D-233):
 * - §10.1 Five-tab canonical navigation and secondary route state transitions across all five theme modes
 * - §10.2 Fresh database, automatic cleanup of untouched `4c1187a` v1 demonstration data, and strict
 *         preservation of any demo habit with genuine user activity or dependent records
 * - §10.3 Evidence-derived habit recording without manual Tiny/Usual/Extra selection
 * - §10.4 Unified DaypartBackgroundLayer artwork resolution, LIGHT vs SYSTEM-light palette coherence, and reduced motion
 * - §10.5 Settings reachability, persistence, and dead-code removal
 * - §10.6 Cold-launch brand presentation and About build provenance
 * - §10.7 Live Android Activity + Jetpack Compose semantics verification across Today, Habits, Journal, Insights, and Settings
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CanonicalUiAndBehaviourReconciliationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var testClock: TestDayseedClock
    private val tempDbNames = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 13),
            currentLocalTime = LocalTime.of(10, 30),
            currentUtcMs = 1_794_565_800_000L
        )
    }

    @After
    fun tearDown() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        for (name in tempDbNames) {
            context.deleteDatabase(name)
        }
        tempDbNames.clear()
    }

    private fun newRepository(dbName: String? = null): DayseedRepository {
        if (dbName != null) tempDbNames.add(dbName)
        return DayseedRepository(context, clock = testClock, databaseName = dbName)
    }

    private fun newViewModel(repo: DayseedRepository): DayseedViewModel {
        return DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
    }

    /**
     * Seeds a true historical v1 (`DATABASE_VERSION = 1`) SQLite database matching commit `4c1187a`'s
     * `seedInitialData(db)` implementation before upgrading to v3.
     */
    private fun createHistorical4c1187aV1Database(
        dbName: String,
        logActivityOnGentleMovement: Boolean = false
    ) {
        tempDbNames.add(dbName)
        val v1Helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE habits (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT,
                        tracking_mode TEXT NOT NULL,
                        tiny_desc TEXT,
                        usual_desc TEXT,
                        extra_desc TEXT,
                        schedule_days TEXT NOT NULL,
                        is_archived INTEGER NOT NULL DEFAULT 0,
                        color_hex TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE daily_records (
                        id TEXT PRIMARY KEY,
                        habit_id TEXT NOT NULL,
                        date TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        effort_level TEXT,
                        note TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE journal_entries (
                        id TEXT PRIMARY KEY,
                        date TEXT NOT NULL,
                        prompt TEXT,
                        content TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE user_preferences (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val db = v1Helper.writableDatabase
        val now = 1_772_000_000_000L
        val todayStr = "2026-02-25"

        fun insertV1Habit(
            id: String,
            name: String,
            desc: String,
            mode: String,
            tiny: String,
            usual: String,
            extra: String,
            color: String
        ) {
            val cv = ContentValues().apply {
                put("id", id)
                put("name", name)
                put("description", desc)
                put("tracking_mode", mode)
                put("tiny_desc", tiny)
                put("usual_desc", usual)
                put("extra_desc", extra)
                put("schedule_days", "1,2,3,4,5,6,7")
                put("is_archived", 0)
                put("color_hex", color)
                put("created_at", now)
            }
            db.insertOrThrow("habits", null, cv)
        }

        insertV1Habit(
            id = "v1_demo_1",
            name = "Gentle Movement",
            desc = "Move your body with kindness, no workout pressure",
            mode = "EFFORT_LEVELS",
            tiny = "Roll shoulders or 1 deep stretch",
            usual = "10-minute walk or gentle yoga",
            extra = "25-minute walk in fresh air",
            color = "#286143"
        )
        insertV1Habit(
            id = "v1_demo_2",
            name = "Quiet Reading",
            desc = "Feed curiosity without finishing a whole book",
            mode = "EFFORT_LEVELS",
            tiny = "Read 1 paragraph or sentence",
            usual = "Read 1 chapter (10 mins)",
            extra = "Read for 30 minutes in peace",
            color = "#3B6955"
        )
        insertV1Habit(
            id = "v1_demo_3",
            name = "Drink Water",
            desc = "A cup of water to refresh your body",
            mode = "COMPLETION_ONLY",
            tiny = "",
            usual = "",
            extra = "",
            color = "#4A7C59"
        )

        if (logActivityOnGentleMovement) {
            val recCv = ContentValues().apply {
                put("id", "v1_rec_1")
                put("habit_id", "v1_demo_1")
                put("date", todayStr)
                put("outcome", "EFFORT_LOGGED")
                put("effort_level", "USUAL")
                put("note", "Morning walk in the park")
                put("updated_at", now + 1000L)
            }
            db.insertOrThrow("daily_records", null, recCv)
        }

        val jCv = ContentValues().apply {
            put("id", "v1_journal_1")
            put("date", todayStr)
            put("prompt", "What is one small thing that went well today?")
            put("content", "Started Dayseed today. Excited to track my habits with kindness and without streak anxiety.")
            put("created_at", now)
            put("updated_at", now)
        }
        db.insertOrThrow("journal_entries", null, jCv)

        val prefCv = ContentValues().apply {
            put("key", "onboarding_completed")
            put("value", "true")
        }
        db.insertOrThrow("user_preferences", null, prefCv)

        db.close()
        v1Helper.close()
    }

    private fun seedHistorical4c1187aDemoDataInV3(
        db: SQLiteDatabase,
        includeUserRecords: Boolean,
        includeActivityOnGentleMovement: Boolean = false
    ) {
        val now = 1_772_000_000_000L
        val todayStr = "2026-03-25"

        data class DemoSeed(
            val id: String,
            val name: String,
            val desc: String,
            val mode: String,
            val tiny: String,
            val usual: String,
            val extra: String,
            val color: String,
            val sortOrder: Int
        )

        val demoHabits = listOf(
            DemoSeed(
                id = "h_demo_1",
                name = "Gentle Movement",
                desc = "Move your body with kindness, no workout pressure",
                mode = "EFFORT_AWARE",
                tiny = "Roll shoulders or 1 deep stretch",
                usual = "10-minute walk or gentle yoga",
                extra = "25-minute walk in fresh air",
                color = "#286143",
                sortOrder = 0
            ),
            DemoSeed(
                id = "h_demo_2",
                name = "Quiet Reading",
                desc = "Feed curiosity without finishing a whole book",
                mode = "EFFORT_AWARE",
                tiny = "Read 1 paragraph or sentence",
                usual = "Read 1 chapter (10 mins)",
                extra = "Read for 30 minutes in peace",
                color = "#3B6955",
                sortOrder = 1
            ),
            DemoSeed(
                id = "h_demo_3",
                name = "Drink Water",
                desc = "A cup of water to refresh your body",
                mode = "COMPLETION_ONLY",
                tiny = "Deliberately small action",
                usual = "The ordinary intended action",
                extra = "Optional stretch action",
                color = "#4A7C59",
                sortOrder = 2
            )
        )

        for (seed in demoHabits) {
            val hCv = ContentValues().apply {
                put("habit_id", seed.id)
                put("created_at_utc_ms", now)
                put("updated_at_utc_ms", now)
                putNull("category")
                putNull("lifetime_goal_days")
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, hCv)

            val vCv = ContentValues().apply {
                put("version_id", "v1_mig_${seed.id}")
                put("habit_id", seed.id)
                put("effective_from_local_date", todayStr)
                put("name", seed.name)
                put("description", seed.desc)
                put("tracking_mode", seed.mode)
                putNull("unit_code")
                put("target_period", "DAY")
                putNull("target_value")
                put("schedule_kind", "DAILY")
                putNull("scheduled_weekdays_mask")
                putNull("preferred_weekdays_mask")
                put("lifecycle_state", "ACTIVE")
                put("reminder_mode", "OFF")
                putNull("reminder_time_local")
                put("tiny_description", seed.tiny)
                put("usual_description", seed.usual)
                put("extra_description", seed.extra)
                put("color_hex", seed.color)
                put("sort_order", seed.sortOrder)
                put("created_at_utc_ms", now)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, vCv)

            if (includeActivityOnGentleMovement && seed.id == "h_demo_1") {
                val outCv = ContentValues().apply {
                    put("outcome_id", "out_demo_1")
                    put("habit_id", seed.id)
                    put("local_date", todayStr)
                    put("outcome_kind", "EFFORT_LOGGED")
                    put("effort_level", "USUAL")
                    put("idempotency_key", "idem_demo_out_1")
                    put("created_at_utc_ms", now)
                    put("updated_at_utc_ms", now)
                }
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DAY_OUTCOMES, null, outCv)

                val noteCv = ContentValues().apply {
                    put("note_id", "note_demo_1")
                    put("habit_id", seed.id)
                    put("local_date", todayStr)
                    put("body", "User's genuine note on Gentle Movement")
                    put("created_at_utc_ms", now)
                    put("updated_at_utc_ms", now)
                }
                db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_DATE_NOTES, null, noteCv)
            }
        }

        val jCv = ContentValues().apply {
            put("journal_entry_id", "j_demo_1")
            put("local_date", todayStr)
            put("body", "Started Dayseed today. Excited to track my habits with kindness and without streak anxiety.")
            putNull("title")
            putNull("primary_mood_code")
            putNull("secondary_mood_code")
            put("prompt_id", "What is one small thing that went well today?")
            putNull("linked_habit_id")
            put("created_at_utc_ms", now)
            put("updated_at_utc_ms", now)
        }
        db.insertOrThrow(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, jCv)

        val sCv = ContentValues().apply {
            put("setting_key", "onboarding_completed")
            put("value_type", "BOOL")
            put("value_text", "true")
            put("schema_version", 1)
            put("updated_at_utc_ms", now)
        }
        db.insertWithOnConflict(DayseedDatabaseHelper.TABLE_APP_SETTINGS, null, sCv, SQLiteDatabase.CONFLICT_REPLACE)

        if (includeUserRecords) {
            val uHabitCv = ContentValues().apply {
                put("habit_id", "user_habit_1")
                put("created_at_utc_ms", now + 5000L)
                put("updated_at_utc_ms", now + 5000L)
                put("category", "Body")
                putNull("lifetime_goal_days")
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABITS, null, uHabitCv)

            val uVerCv = ContentValues().apply {
                put("version_id", "ver_user_habit_1")
                put("habit_id", "user_habit_1")
                put("effective_from_local_date", todayStr)
                put("name", "Evening Stretching")
                put("description", "15 minutes of mobility work")
                put("tracking_mode", "DURATION")
                put("unit_code", "min")
                put("target_period", "DAY")
                put("target_value", 15)
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
                put("sort_order", 10)
                put("created_at_utc_ms", now + 5000L)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_VERSIONS, null, uVerCv)

            val uEntryCv = ContentValues().apply {
                put("entry_id", "entry_user_1")
                put("habit_id", "user_habit_1")
                put("local_date", todayStr)
                put("value_units", 15)
                put("entry_note", "Felt great")
                put("created_at_utc_ms", now + 6000L)
                put("updated_at_utc_ms", now + 6000L)
                put("idempotency_key", "idem_user_entry_1")
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_HABIT_ENTRIES, null, uEntryCv)

            val uJournalCv = ContentValues().apply {
                put("journal_entry_id", "user_journal_1")
                put("local_date", todayStr)
                put("body", "Walked outside at sunset and noticed the quiet sky.")
                put("title", "Real personal note")
                put("primary_mood_code", "CALM")
                putNull("secondary_mood_code")
                putNull("prompt_id")
                put("linked_habit_id", "user_habit_1")
                put("created_at_utc_ms", now + 7000L)
                put("updated_at_utc_ms", now + 7000L)
            }
            db.insertOrThrow(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, uJournalCv)
        }
    }

    // =========================================================================
    // §10.1 Navigation & Canonical 5-Tab + Secondary Route Transitions
    // =========================================================================

    @Test
    fun canonicalFiveTabsAndSecondaryRoutesTransitionAcrossAllFiveThemeModes() {
        val repo = newRepository()
        val vm = newViewModel(repo)

        val expectedTabs = listOf(
            NavTab.TODAY,
            NavTab.HABITS,
            NavTab.JOURNAL,
            NavTab.INSIGHTS,
            NavTab.SETTINGS
        )
        assertEquals(expectedTabs, NavTab.entries)

        for (mode in ThemeModeOption.entries) {
            vm.setThemeMode(mode)
            repo.appSettings.awaitMatch { it["theme_mode"]?.valueText == mode.settingValue }
            assertEquals(mode.settingValue, repo.getSettingString("theme_mode", ""))

            val (scheme, _) = resolveDayseedColorScheme(mode, systemDark = false, localTime = testClock.nowLocalTime())
            assertNotNull(scheme)
            val artworkBlend = resolveDayseedArtworkBlend(mode, systemDark = false, localTime = testClock.nowLocalTime())
            assertNotNull(artworkBlend)

            for (tab in expectedTabs) {
                vm.selectTab(tab)
                assertEquals(tab, vm.selectedTab.value)
                assertNull(vm.currentSecondaryRoute.value)
            }
        }

        vm.selectTab(NavTab.TODAY)
        vm.openCreateEditHabit(null)
        assertTrue(vm.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)
        vm.navigateBack()
        assertNull(vm.currentSecondaryRoute.value)
        assertEquals(NavTab.TODAY, vm.selectedTab.value)

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.2 Fresh Database, Historical 4c1187a Demo-Data Cleanup & Activity Protection
    // =========================================================================

    @Test
    fun freshDatabaseStartsEmptyWithOnboardingIncompleteAndCleanupMarkedDone() {
        val repo = newRepository()

        assertEquals(0, repo.loadHabitsSync().size)
        assertEquals(0, repo.loadJournalEntriesSync().size)
        assertEquals("false", repo.getSettingString("onboarding_completed", "false"))
        assertEquals("true", repo.getSettingString(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED, "false"))

        repo.dbHelper.close()
    }

    @Test
    fun historicalDemoOnlyDatabaseAutomaticallyCleansUpAndResetsOnboarding() {
        // 1. Test real v1 -> v2 -> v3 upgrade path from an untouched 4c1187a v1 database
        val v1UpgradeDbName = "legacy_v1_upgrade_demo_only.db"
        createHistorical4c1187aV1Database(v1UpgradeDbName, logActivityOnGentleMovement = false)

        val upgradedRepo = DayseedRepository(context, clock = testClock, databaseName = v1UpgradeDbName)
        assertEquals(0, upgradedRepo.loadHabitsSync().size)
        assertEquals(0, upgradedRepo.loadJournalEntriesSync().size)
        assertEquals("false", upgradedRepo.getSettingString("onboarding_completed", "true"))
        assertEquals("true", upgradedRepo.getSettingString(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED, "false"))
        upgradedRepo.dbHelper.close()

        // 2. Test v3 already-migrated database with untouched demo records
        val dbName = "legacy_demo_only.db"
        tempDbNames.add(dbName)
        val helper = DayseedDatabaseHelper(context, dbName)
        val db = helper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, "setting_key = ?", arrayOf(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED))
        seedHistorical4c1187aDemoDataInV3(db, includeUserRecords = false, includeActivityOnGentleMovement = false)

        val removedCount = helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true)
        assertEquals(4, removedCount) // 3 untouched demo habits + 1 starter journal entry

        val repo = DayseedRepository(context, clock = testClock, databaseName = dbName)
        assertEquals(0, repo.loadHabitsSync().size)
        assertEquals(0, repo.loadJournalEntriesSync().size)
        assertEquals("false", repo.getSettingString("onboarding_completed", "true"))
        assertEquals("true", repo.getSettingString(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED, "false"))

        db.rawQuery("PRAGMA foreign_key_check;", null).use { cursor ->
            assertFalse("foreign_key_check must return no violations", cursor.moveToFirst())
        }
        db.rawQuery("PRAGMA integrity_check;", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ok", cursor.getString(0).lowercase())
        }

        repo.dbHelper.close()
        helper.close()
    }

    @Test
    fun demoHabitWithGenuineUserActivityIsPreservedWithAllRecordsIntact() {
        // 1. Verify across real v1 -> v2 -> v3 migration when user logged activity on Gentle Movement
        val v1ActiveDbName = "legacy_v1_demo_with_activity.db"
        createHistorical4c1187aV1Database(v1ActiveDbName, logActivityOnGentleMovement = true)

        val upgradedRepo = DayseedRepository(context, clock = testClock, databaseName = v1ActiveDbName)
        val upgradedHabits = upgradedRepo.loadHabitsSync()
        assertEquals("Only the demo habit with activity must remain", 1, upgradedHabits.size)
        assertEquals("v1_demo_1", upgradedHabits.single().habitId)
        assertEquals("Gentle Movement", upgradedHabits.single().name)
        assertEquals(1, upgradedRepo.habitDayOutcomes.value.count { it.habitId == "v1_demo_1" })
        assertEquals(1, upgradedRepo.habitDateNotes.value.count { it.habitId == "v1_demo_1" })
        assertEquals("Morning walk in the park", upgradedRepo.habitDateNotes.value.single().body)
        // Because 1 habit remains, onboarding_completed stays "true"
        assertEquals("true", upgradedRepo.getSettingString("onboarding_completed", "false"))
        // The untouched starter journal entry was removed
        assertEquals(0, upgradedRepo.loadJournalEntriesSync().size)
        upgradedRepo.dbHelper.close()

        // 2. Verify in v3 when Gentle Movement has an outcome and date note
        val v3ActiveDbName = "legacy_v3_demo_with_activity.db"
        tempDbNames.add(v3ActiveDbName)
        val helper = DayseedDatabaseHelper(context, v3ActiveDbName)
        val db = helper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, "setting_key = ?", arrayOf(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED))
        seedHistorical4c1187aDemoDataInV3(db, includeUserRecords = false, includeActivityOnGentleMovement = true)

        val removedCount = helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true)
        // 2 untouched demo habits (Quiet Reading, Drink Water) + 1 starter journal entry = 3 removed; Gentle Movement preserved!
        assertEquals(3, removedCount)

        val repo = DayseedRepository(context, clock = testClock, databaseName = v3ActiveDbName)
        val remainingHabits = repo.loadHabitsSync()
        assertEquals(1, remainingHabits.size)
        assertEquals("h_demo_1", remainingHabits.single().habitId)
        assertEquals("Gentle Movement", remainingHabits.single().name)
        assertEquals(1, repo.habitDayOutcomes.value.size)
        assertEquals("USUAL", repo.habitDayOutcomes.value.single().effortLevel)
        assertEquals(1, repo.habitDateNotes.value.size)
        assertEquals("User's genuine note on Gentle Movement", repo.habitDateNotes.value.single().body)
        assertEquals("true", repo.getSettingString("onboarding_completed", "false"))

        repo.dbHelper.close()
        helper.close()
    }

    @Test
    fun mixedDemoAndUserDatabaseRemovesOnlyExactUntouchedDemoItemsAndPreservesAllUserRecords() {
        val dbName = "mixed_demo_user.db"
        tempDbNames.add(dbName)
        val helper = DayseedDatabaseHelper(context, dbName)
        val db = helper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, "setting_key = ?", arrayOf(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED))
        seedHistorical4c1187aDemoDataInV3(db, includeUserRecords = true, includeActivityOnGentleMovement = false)

        // Also seed a user-edited starter journal entry (where updated_at_utc_ms > created_at_utc_ms)
        val editedStarterJournalCv = ContentValues().apply {
            put("journal_entry_id", "j_user_edited_starter")
            put("local_date", "2026-03-25")
            put("body", "Started Dayseed today. Excited to track my habits with kindness and without streak anxiety.")
            putNull("title")
            putNull("primary_mood_code")
            putNull("secondary_mood_code")
            put("prompt_id", "What is one small thing that went well today?")
            putNull("linked_habit_id")
            put("created_at_utc_ms", 1_772_000_000_000L)
            put("updated_at_utc_ms", 1_772_000_060_000L) // Edited 60s later by user
        }
        db.insertOrThrow(DayseedDatabaseHelper.TABLE_JOURNAL_ENTRIES, null, editedStarterJournalCv)

        val removedCount = helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true)
        assertEquals(4, removedCount) // 3 untouched demo habits + 1 unedited starter journal

        val repo = DayseedRepository(context, clock = testClock, databaseName = dbName)
        val remainingHabits = repo.loadHabitsSync()
        assertEquals(1, remainingHabits.size)
        assertEquals("user_habit_1", remainingHabits.first().habitId)
        assertEquals("Evening Stretching", remainingHabits.first().name)

        val remainingEntries = repo.habitEntries.value
        assertEquals(1, remainingEntries.size)
        assertEquals("entry_user_1", remainingEntries.first().entryId)
        assertEquals(15, remainingEntries.first().valueUnits)

        val remainingJournals = repo.loadJournalEntriesSync()
        assertEquals(2, remainingJournals.size)
        assertTrue(remainingJournals.any { it.id == "user_journal_1" && it.title == "Real personal note" })
        assertTrue(remainingJournals.any { it.id == "j_user_edited_starter" })

        assertEquals("true", repo.getSettingString("onboarding_completed", "false"))

        // Repeated cleanup (both normal and forced) must be a safe no-op
        assertEquals(0, helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = false))
        assertEquals(0, helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true))
        assertEquals(1, repo.loadHabitsSync().size)
        assertEquals(2, repo.loadJournalEntriesSync().size)

        repo.dbHelper.close()
        helper.close()
    }

    @Test
    fun userCreatedHabitNamedDrinkWaterIsNeverMisclassifiedAsDemoData() {
        val dbName = "user_drink_water.db"
        val repo = newRepository(dbName)
        repo.saveHabitSync(
            Habit(
                habitId = "custom_water_id",
                name = "Drink Water",
                description = "Stay hydrated throughout the day",
                category = "Health",
                colorHex = "#3B7A57",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "glasses",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 8
            ),
            effectiveDateOverride = testClock.todayLocal()
        )

        val removed = repo.dbHelper.cleanupLegacyV1DemoDataIfNeeded(repo.dbHelper.writableDatabase, forceRecheck = true)
        assertEquals(0, removed)
        assertEquals(1, repo.loadHabitsSync().size)
        assertEquals("Drink Water", repo.loadHabitsSync().first().name)

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.3 Evidence-Derived Habit Recording & Progress-Band Derivation
    // =========================================================================

    @Test
    fun quantityAndDurationHabitsDeriveAllD040ProgressBandsFromRecordedValues() {
        val repo = newRepository()
        val baseDate = LocalDate.of(2026, 11, 1)

        repo.saveHabitSync(
            Habit(
                habitId = "h_qty_8",
                name = "Hydration",
                description = "8 glasses a day",
                category = "Health",
                colorHex = "#286143",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "glasses",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 8,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = baseDate
        )

        repo.saveHabitSync(
            Habit(
                habitId = "h_dur_20",
                name = "Meditation",
                description = "20 minutes a day",
                category = "Mindfulness",
                colorHex = "#558B6E",
                trackingMode = TrackingMode.DURATION,
                unitCode = "min",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 20,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = baseDate
        )

        val quantityCases = listOf(
            Triple(baseDate.plusDays(0), 0, ProgressBand.ZERO),
            Triple(baseDate.plusDays(1), 1, ProgressBand.TINY_STEP),
            Triple(baseDate.plusDays(2), 3, ProgressBand.GETTING_STARTED),
            Triple(baseDate.plusDays(3), 4, ProgressBand.HALFWAY),
            Triple(baseDate.plusDays(4), 6, ProgressBand.ALMOST_THERE),
            Triple(baseDate.plusDays(5), 8, ProgressBand.GOAL_MET),
            Triple(baseDate.plusDays(6), 10, ProgressBand.EXTRA),
            Triple(baseDate.plusDays(7), 14, ProgressBand.HUGE)
        )

        for ((date, value, expectedBand) in quantityCases) {
            repo.logHabitEntrySync("h_qty_8", date, value)
            val detail = repo.computeDayDetailSummary(date, testClock.todayLocal())
            val item = detail.habitItems.first { it.habit.habitId == "h_qty_8" }
            assertEquals("Quantity=$value on $date must derive $expectedBand", expectedBand, item.progressBand)
        }

        val durationCases = listOf(
            Triple(baseDate.plusDays(0), 0, ProgressBand.ZERO),
            Triple(baseDate.plusDays(1), 3, ProgressBand.TINY_STEP),
            Triple(baseDate.plusDays(2), 6, ProgressBand.GETTING_STARTED),
            Triple(baseDate.plusDays(3), 10, ProgressBand.HALFWAY),
            Triple(baseDate.plusDays(4), 16, ProgressBand.ALMOST_THERE),
            Triple(baseDate.plusDays(5), 20, ProgressBand.GOAL_MET),
            Triple(baseDate.plusDays(6), 25, ProgressBand.EXTRA),
            Triple(baseDate.plusDays(7), 35, ProgressBand.HUGE)
        )

        for ((date, minutes, expectedBand) in durationCases) {
            repo.logHabitEntrySync("h_dur_20", date, minutes)
            val detail = repo.computeDayDetailSummary(date, testClock.todayLocal())
            val item = detail.habitItems.first { it.habit.habitId == "h_dur_20" }
            assertEquals("Duration=${minutes}m on $date must derive $expectedBand", expectedBand, item.progressBand)
        }

        repo.dbHelper.close()
    }

    @Test
    fun completionOnlyAndLegacyEffortAwareUnifiedCompletionRecordCleanCompletedOutcome() {
        val repo = newRepository()
        val vm = newViewModel(repo)
        val today = testClock.todayLocal()

        repo.saveHabitSync(
            Habit(
                habitId = "h_check_only",
                name = "Morning Sunlight",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = today.minusDays(5)
        )
        val habit = repo.loadHabitsSync().first { it.habitId == "h_check_only" }

        vm.logHabit(habit = habit, date = today, effortLevel = EffortLevel.STANDARD)
        repo.habitDayOutcomes.awaitMatch { outcomes ->
            outcomes.any { it.habitId == habit.habitId && it.localDate == today.toString() }
        }

        val outcome = repo.habitDayOutcomes.value.find {
            it.habitId == habit.habitId && it.localDate == today.toString()
        }
        assertNotNull(outcome)
        assertEquals("COMPLETED", outcome!!.outcomeKind)
        assertNull("COMPLETION_ONLY must not store a fabricated effortLevel", outcome.effortLevel)

        val detail = repo.computeDayDetailSummary(today, today)
        val item = detail.habitItems.first { it.habit.habitId == habit.habitId }
        assertEquals(ProgressBand.GOAL_MET, item.progressBand)
        assertEquals(BigInteger.ONE, item.totalUnits)

        repo.dbHelper.close()
    }

    @Test
    fun legacyEffortAwareHistoricalOutcomesRemainReadableAndDisallowNewHabitCreationByDefault() {
        val repo = newRepository()
        val today = testClock.todayLocal()
        val yesterday = today.minusDays(1)

        var rejectedNewEffortHabit = false
        try {
            repo.saveHabitSync(
                Habit(
                    habitId = "h_disallowed_effort",
                    name = "Disallowed New Effort Habit",
                    trackingMode = TrackingMode.EFFORT_AWARE
                )
            )
        } catch (_: IllegalArgumentException) {
            rejectedNewEffortHabit = true
        }
        assertTrue("New EFFORT_AWARE habit creation must be rejected by default", rejectedNewEffortHabit)

        repo.saveHabitSync(
            Habit(
                habitId = "h_legacy_effort",
                name = "Legacy Reading",
                trackingMode = TrackingMode.EFFORT_AWARE,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = today.minusDays(10),
            allowLegacyEffortModeCreation = true
        )
        repo.logHabitDayOutcomeSync(
            habitId = "h_legacy_effort",
            date = yesterday,
            outcomeKind = "EFFORT_LOGGED",
            effortLevel = "TINY"
        )

        val yesterdayDetail = repo.computeDayDetailSummary(yesterday, today)
        val yesterdayItem = yesterdayDetail.habitItems.first { it.habit.habitId == "h_legacy_effort" }
        assertEquals(DayRecordState.PARTIAL_PROGRESS, yesterdayItem.dayRecordState)
        assertEquals(ProgressBand.TINY_STEP, yesterdayItem.progressBand)
        assertEquals(DerivedDayStatus.COMPLETED, repo.evaluateDerivedDayStatus("h_legacy_effort", yesterday))

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.4 Theme Surface Coherence & Reduced Motion Accessibility
    // =========================================================================

    @Test
    fun lightModeAndSystemLightModeResolveIdenticalColorSchemeAndSurface() {
        val (lightScheme, lightIsDark) = resolveDayseedColorScheme(
            themeMode = ThemeModeOption.LIGHT,
            systemDark = false,
            localTime = LocalTime.of(14, 0)
        )
        val (systemLightScheme, systemLightIsDark) = resolveDayseedColorScheme(
            themeMode = ThemeModeOption.SYSTEM,
            systemDark = false,
            localTime = LocalTime.of(14, 0)
        )
        assertFalse(lightIsDark)
        assertFalse(systemLightIsDark)
        assertEquals(DayJournalSurface, lightScheme.surface)
        assertEquals(systemLightScheme.surface, lightScheme.surface)
        assertEquals(systemLightScheme.background, lightScheme.background)
        assertEquals(systemLightScheme.primary, lightScheme.primary)
        assertEquals(systemLightScheme.surfaceVariant, lightScheme.surfaceVariant)
    }

    @Test
    fun reducedMotionTogglePersistsAndSystemAnimatorScaleIsHonoured() {
        val repo = newRepository()
        val vm = newViewModel(repo)

        assertFalse(vm.isReducedMotionEnabled())
        assertEquals(1200, themeTransitionDurationMillis(vm.isReducedMotionEnabled()))

        vm.setReducedMotionEnabled(true)
        repo.appSettings.awaitMatch { it["reduced_motion_enabled"]?.valueText == "true" }
        assertTrue(vm.isReducedMotionEnabled())
        assertEquals("true", repo.getSettingString("reduced_motion_enabled", "false"))
        assertEquals(0, themeTransitionDurationMillis(vm.isReducedMotionEnabled()))

        vm.setReducedMotionEnabled(false)
        repo.appSettings.awaitMatch { it["reduced_motion_enabled"]?.valueText == "false" }
        assertFalse(vm.isReducedMotionEnabled())

        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f)
        assertTrue("OS animator scale 0f must activate reduced motion", vm.isSystemReducedMotionActive())
        assertTrue("Effective reduced motion must be true when OS scale is 0f", vm.isReducedMotionEnabled())
        assertEquals(0, themeTransitionDurationMillis(vm.isReducedMotionEnabled()))

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.5 Settings Persistence & Dead-Code Cleanup
    // =========================================================================

    @Test
    fun settingsPreferencesPersistAndDeadJournalExportSheetEnumIsRemoved() {
        val repo = newRepository()
        val vm = newViewModel(repo)

        vm.setThemeMode(ThemeModeOption.DARK_DAYPART_ART)
        repo.appSettings.awaitMatch { it["theme_mode"]?.valueText == "DARK_DAYPART_ART" }
        assertEquals("DARK_DAYPART_ART", repo.getSettingString("theme_mode", ""))

        vm.setWeekStart(DayOfWeek.SUNDAY)
        repo.appSettings.awaitMatch { it["week_start"]?.valueText == "SUNDAY" }
        assertEquals("SUNDAY", repo.getSettingString("week_start", ""))

        vm.setQuietHours("22:15", "07:30")
        repo.appSettings.awaitMatch {
            it["quiet_hours_start"]?.valueText == "22:15" &&
                it["quiet_hours_end"]?.valueText == "07:30"
        }
        assertEquals("22:15", repo.getSettingString("quiet_hours_start", ""))
        assertEquals("07:30", repo.getSettingString("quiet_hours_end", ""))

        val sheetNames = SettingsSheet.entries.map { it.name }.toSet()
        assertFalse("Unreachable JOURNAL_EXPORT sheet enum entry must be removed", sheetNames.contains("JOURNAL_EXPORT"))
        assertTrue(sheetNames.containsAll(listOf("APPEARANCE", "WEEK_START", "QUIET_HOURS", "REMINDERS", "PRIVACY", "REMOVED_HABITS", "ABOUT")))

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.6 Cold-Launch Brand Presentation & Build Provenance
    // =========================================================================

    @Test
    fun coldLaunchSceneStateMarksOncePerSessionAndBuildProvenanceMatchesReleaseVersion() {
        val repo = newRepository()
        val vm = newViewModel(repo)

        assertEquals("Dayseed", DAYSEED_BRAND_NAME)
        assertEquals("Your progress is more than your streak.", DAYSEED_OFFICIAL_MOTTO)

        assertFalse(vm.hasShownColdLaunchScene.value)
        vm.markColdLaunchSceneShown()
        assertTrue(vm.hasShownColdLaunchScene.value)

        vm.selectTab(NavTab.HABITS)
        vm.selectTab(NavTab.TODAY)
        assertTrue(vm.hasShownColdLaunchScene.value)

        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("1.0.1", BuildConfig.VERSION_NAME)
        assertEquals("com.aistudio.dayseed.wkqmfl", BuildConfig.APPLICATION_ID)
        assertTrue("GIT_COMMIT_SHA must be non-blank", BuildConfig.GIT_COMMIT_SHA.isNotBlank())

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.7 Live Android Activity & Compose Semantics Runtime Inspection
    // =========================================================================

    @Test
    fun runtimeComposeUiLaunchesNavigatesTabsAndVerifiesScreensAndControls() {
        val dbName = "runtime_ui_verification.db"
        val repo = newRepository(dbName)
        val today = testClock.todayLocal()

        // Seed one measured QUANTITY habit and one legacy EFFORT_AWARE habit for runtime UI inspection
        repo.saveHabitSync(
            Habit(
                habitId = "h_runtime_qty",
                name = "Drink Water",
                description = "Stay hydrated",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "glasses",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 8,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = today.minusDays(3)
        )
        repo.saveHabitSync(
            Habit(
                habitId = "h_runtime_legacy",
                name = "Quiet Reading",
                description = "Legacy effort habit",
                trackingMode = TrackingMode.EFFORT_AWARE,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY
            ),
            effectiveDateOverride = today.minusDays(3),
            allowLegacyEffortModeCreation = true
        )

        val vm = newViewModel(repo)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme(
                themeMode = ThemeModeOption.SOFT_BOTANICAL_AUTO,
                localTime = testClock.nowLocalTime(),
                reducedMotion = vm.isReducedMotionEnabled()
            ) {
                DayseedApp(
                    viewModel = vm,
                    liveLocalTime = testClock.nowLocalTime()
                )
            }
        }

        fun findComposeView(view: View): androidx.compose.ui.platform.AbstractComposeView? {
            if (view is androidx.compose.ui.platform.AbstractComposeView) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val found = findComposeView(view.getChildAt(i))
                    if (found != null) return found
                }
            }
            return null
        }

        fun pumpComposeLayout(recompose: Boolean = false) {
            if (recompose) {
                findComposeView(activity.window.decorView)?.disposeComposition()
            }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
            val decor = activity.window.decorView
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, 1080, 4000)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
        }

        fun findRootForTest(view: View): RootForTest? {
            if (view is RootForTest) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val found = findRootForTest(view.getChildAt(i))
                    if (found != null) return found
                }
            }
            return null
        }

        fun collectAllSemanticsNodes(): List<SemanticsNode> {
            pumpComposeLayout(recompose = false)
            val root = findRootForTest(activity.window.decorView)
            assertNotNull("AndroidComposeView RootForTest must be attached to Activity window", root)
            val result = mutableListOf<SemanticsNode>()
            fun walk(node: SemanticsNode) {
                result.add(node)
                for (child in node.children) {
                    walk(child)
                }
            }
            walk(root!!.semanticsOwner.unmergedRootSemanticsNode)
            return result
        }

        fun collectTestTags(): Set<String> =
            collectAllSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.toSet()

        fun collectRenderedTexts(): List<String> =
            collectAllSemanticsNodes()
                .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
                .map { it.text }

        fun clickTag(tag: String) {
            val node = collectAllSemanticsNodes().firstOrNull {
                it.config.getOrNull(SemanticsProperties.TestTag) == tag
            }
            assertNotNull("Expected clickable node with testTag '$tag'", node)
            val action = node!!.config.getOrNull(SemanticsActions.OnClick)?.action
            assertNotNull("Expected OnClick action on testTag '$tag'", action)
            action!!.invoke()
            pumpComposeLayout(recompose = true)
        }

        // 1. Inspect initial Today screen on cold launch
        val initialTags = collectTestTags()
        val initialTexts = collectRenderedTexts()
        assertTrue("Shared daypart background layer must render on Today", "shared_daypart_background_layer" in initialTags)
        assertTrue("Cold-launch brand card must render on first launch", "dayseed_cold_launch_scene" in initialTags)
        assertTrue("Cold-launch brand lockup must display official motto", initialTexts.contains("Your progress is more than your streak."))

        // Verify habit cards on Today expose unified Complete/Record without manual Tiny/Usual/Extra buttons
        assertTrue("Quantity habit Complete Goal button must be present", "quick_add_goal_h_runtime_qty" in initialTags)
        assertTrue("Quantity habit Record button must be present", "open_record_session_h_runtime_qty" in initialTags)
        assertTrue("Legacy habit Complete button must be present", "habit_done_btn_h_runtime_legacy" in initialTags)
        assertTrue("Legacy habit conversion notice must be present", "legacy_effort_mode_notice_h_runtime_legacy" in initialTags)
        assertFalse("Manual Tiny effort button must NOT be rendered", initialTexts.any { it.contains("🌱 Tiny") })
        assertFalse("Manual Usual effort button must NOT be rendered", initialTexts.any { it.contains("🌿 Usual") })
        assertFalse("Manual Extra effort button must NOT be rendered", initialTexts.any { it.contains("🌳 Extra") })

        // 2. Navigate to Habits tab via bottom navigation bar click
        clickTag("nav_habits")
        val habitsTags = collectTestTags()
        assertTrue("Shared daypart background layer must remain rendered on Habits", "shared_daypart_background_layer" in habitsTags)
        assertTrue("Habits screen must be rendered", "habits_screen" in habitsTags)
        assertEquals(NavTab.HABITS, vm.selectedTab.value)
        assertTrue("Cold-launch scene must mark shown on tab switch", vm.hasShownColdLaunchScene.value)

        // 3. Navigate to Journal tab
        clickTag("nav_journal")
        val journalTags = collectTestTags()
        assertTrue("Shared daypart background layer must remain rendered on Journal", "shared_daypart_background_layer" in journalTags)
        assertEquals(NavTab.JOURNAL, vm.selectedTab.value)

        // 4. Navigate to Insights tab
        clickTag("nav_insights")
        val insightsTags = collectTestTags()
        assertTrue("Shared daypart background layer must remain rendered on Insights", "shared_daypart_background_layer" in insightsTags)
        assertEquals(NavTab.INSIGHTS, vm.selectedTab.value)

        // 5. Navigate to Settings tab and inspect Reduce Motion + Appearance controls
        clickTag("nav_settings")
        val settingsTags = collectTestTags()
        assertTrue("Shared daypart background layer must remain rendered on Settings", "shared_daypart_background_layer" in settingsTags)
        assertTrue("Settings screen must be rendered", "settings_screen" in settingsTags)
        assertTrue("Reduce motion row must be rendered on Settings", "settings_row_reduced_motion" in settingsTags)
        assertTrue("Reduce motion switch must be rendered on Settings", "settings_reduced_motion_switch" in settingsTags)

        // 6. Return to Today and confirm cold-launch brand card does NOT reappear in the same process session
        clickTag("nav_today")
        val returnedTodayTags = collectTestTags()
        assertFalse("Cold-launch brand card must not reappear after being shown in the same session", "dayseed_cold_launch_scene" in returnedTodayTags)

        // 7. Open RecordSessionSheet and confirm no manual Tiny/Usual/Extra effort chips exist
        vm.openRecordSession("h_runtime_qty", today.toString(), null)
        pumpComposeLayout(recompose = true)
        val recordSheetTags = collectTestTags()
        val recordSheetTexts = collectRenderedTexts()
        assertTrue("Shared daypart background layer must remain rendered behind RecordSessionSheet", "shared_daypart_background_layer" in recordSheetTags)
        assertTrue("RecordSessionSheet root must be rendered", "record_session_sheet" in recordSheetTags)
        assertTrue("RecordSessionSheet input must be rendered", "record_session_value_input" in recordSheetTags)
        assertFalse("RecordSessionSheet must not render 🌱 Tiny chip", recordSheetTexts.any { it.contains("🌱 Tiny") })
        assertFalse("RecordSessionSheet must not render 🌿 Usual chip", recordSheetTexts.any { it.contains("🌿 Usual") })
        assertFalse("RecordSessionSheet must not render 🌳 Extra chip", recordSheetTexts.any { it.contains("🌳 Extra") })
        vm.navigateBack()
        pumpComposeLayout(recompose = true)

        // 8. Open CreateEditHabitScreen for legacy EFFORT_AWARE habit and verify conversion guidance banner
        vm.openCreateEditHabit("h_runtime_legacy")
        pumpComposeLayout(recompose = true)
        val editLegacyTags = collectTestTags()
        assertTrue("Shared daypart background layer must remain rendered behind CreateEditHabitScreen", "shared_daypart_background_layer" in editLegacyTags)
        assertTrue("Legacy EFFORT_AWARE habit editor must render conversion banner", "legacy_effort_conversion_banner" in editLegacyTags)
        assertFalse("Legacy editor must not expose tiny_desc_input", "tiny_desc_input" in editLegacyTags)
        vm.navigateBack()

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.8 Responsive Layout & Non-Overlap Verification Across Phone Viewports
    // =========================================================================

    @Test
    fun responsiveLayoutsVerifyJournalHeaderInsightsCardsAndTodayFabAcrossViewports() {
        val dbName = "test_responsive_layouts_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                DayseedApp(viewModel = vm)
            }
        }

        val density = activity.resources.displayMetrics.density

        fun findComposeView(view: View): androidx.compose.ui.platform.ComposeView? {
            if (view is androidx.compose.ui.platform.ComposeView) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val found = findComposeView(view.getChildAt(i))
                    if (found != null) return found
                }
            }
            return null
        }

        fun findRootForTest(view: View): RootForTest? {
            if (view is RootForTest) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val found = findRootForTest(view.getChildAt(i))
                    if (found != null) return found
                }
            }
            return null
        }

        fun pumpViewport(widthDp: Int, heightDp: Int, recompose: Boolean = true) {
            if (recompose) {
                findComposeView(activity.window.decorView)?.disposeComposition()
            }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
            val widthPx = (widthDp * density).toInt()
            val heightPx = (heightDp * density).toInt()
            val decor = activity.window.decorView
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, widthPx, heightPx)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
        }

        fun collectAllNodes(): List<SemanticsNode> {
            val root = findRootForTest(activity.window.decorView)
            assertNotNull("AndroidComposeView RootForTest must be attached", root)
            val result = mutableListOf<SemanticsNode>()
            fun walk(node: SemanticsNode) {
                result.add(node)
                for (child in node.children) {
                    walk(child)
                }
            }
            walk(root!!.semanticsOwner.unmergedRootSemanticsNode)
            return result
        }

        fun findNodeByTag(tag: String): SemanticsNode {
            val node = collectAllNodes().firstOrNull {
                it.config.getOrNull(SemanticsProperties.TestTag) == tag
            }
            assertNotNull("Expected node with testTag '$tag'", node)
            return node!!
        }

        fun collectDescendantNodes(rootNode: SemanticsNode): List<SemanticsNode> {
            val list = mutableListOf<SemanticsNode>()
            fun walk(n: SemanticsNode) {
                list.add(n)
                for (c in n.children) walk(c)
            }
            walk(rootNode)
            return list
        }

        // Test across narrow (320dp), standard (360dp), and wider (411dp) phone viewports
        val viewportWidthsDp = listOf(320, 360, 411)

        for (widthDp in viewportWidthsDp) {
            // 1. TODAY SCREEN (0 habits empty state): FAB must never overlap Today's Seeds heading,
            //    0 of 0 tended progress indicator, or EmptyHabitsCard.
            vm.selectTab(NavTab.TODAY)
            pumpViewport(widthDp = widthDp, heightDp = 680, recompose = true)

            val fabNode = findNodeByTag("add_habit_fab")
            val seedsHeaderNode = findNodeByTag("today_seeds_section_header")
            val seedsHeadingNode = findNodeByTag("today_seeds_heading_text")
            val tendedCountNode = findNodeByTag("today_tended_count_text")
            val emptyCardNode = findNodeByTag("today_empty_habits_card")
            val navBarTodayNode = findNodeByTag("nav_today")

            val fabBounds = fabNode.boundsInRoot
            assertFalse(
                "At ${widthDp}dp (0 habits), add_habit_fab ($fabBounds) must NOT overlap Today's Seeds header (${seedsHeaderNode.boundsInRoot})",
                fabBounds.overlaps(seedsHeaderNode.boundsInRoot)
            )
            assertFalse(
                "At ${widthDp}dp (0 habits), add_habit_fab ($fabBounds) must NOT overlap Today's Seeds heading text (${seedsHeadingNode.boundsInRoot})",
                fabBounds.overlaps(seedsHeadingNode.boundsInRoot)
            )
            assertFalse(
                "At ${widthDp}dp (0 habits), add_habit_fab ($fabBounds) must NOT overlap tended progress indicator (${tendedCountNode.boundsInRoot})",
                fabBounds.overlaps(tendedCountNode.boundsInRoot)
            )
            assertFalse(
                "At ${widthDp}dp (0 habits), add_habit_fab ($fabBounds) must NOT overlap EmptyHabitsCard (${emptyCardNode.boundsInRoot})",
                fabBounds.overlaps(emptyCardNode.boundsInRoot)
            )
            assertTrue(
                "At ${widthDp}dp, add_habit_fab bottom (${fabBounds.bottom}) must stay above bottom NavigationBar top (${navBarTodayNode.boundsInRoot.top})",
                fabBounds.bottom <= navBarTodayNode.boundsInRoot.top
            )

            // 2. JOURNAL SCREEN: "Journal" heading must render on a single line (lineCount == 1)
            //    and never collide with Date Jump or Write buttons.
            vm.selectTab(NavTab.JOURNAL)
            pumpViewport(widthDp = widthDp, heightDp = 760, recompose = true)

            val journalTitleNode = findNodeByTag("journal_header_title")
            val dateJumpNode = findNodeByTag("journal_date_jump_toggle")
            val writeBtnNode = findNodeByTag("journal_add_entry_fab")

            val titleLayoutResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            val gotLayout = journalTitleNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(titleLayoutResults)
            assertEquals("Must retrieve TextLayoutResult for journal_header_title at ${widthDp}dp", true, gotLayout)
            assertEquals(
                "At ${widthDp}dp, 'Journal' heading must remain on 1 line without breaking into 'Journ' / 'al'",
                1,
                titleLayoutResults.first().lineCount
            )
            assertFalse(
                "At ${widthDp}dp, 'Journal' heading must not be ellipsized",
                titleLayoutResults.first().isLineEllipsized(0)
            )
            assertFalse(
                "At ${widthDp}dp, journal_header_title (${journalTitleNode.boundsInRoot}) must not overlap journal_date_jump_toggle (${dateJumpNode.boundsInRoot})",
                journalTitleNode.boundsInRoot.overlaps(dateJumpNode.boundsInRoot)
            )
            assertFalse(
                "At ${widthDp}dp, journal_header_title (${journalTitleNode.boundsInRoot}) must not overlap journal_add_entry_fab (${writeBtnNode.boundsInRoot})",
                journalTitleNode.boundsInRoot.overlaps(writeBtnNode.boundsInRoot)
            )
            assertFalse(
                "At ${widthDp}dp, journal_date_jump_toggle (${dateJumpNode.boundsInRoot}) must not overlap journal_add_entry_fab (${writeBtnNode.boundsInRoot})",
                dateJumpNode.boundsInRoot.overlaps(writeBtnNode.boundsInRoot)
            )

            // 3. INSIGHTS SCREEN: All 6 OverviewStatBox cards must display full descriptions
            //    without ellipsis truncation, and paired cards in each row must have matching heights.
            vm.selectTab(NavTab.INSIGHTS)
            pumpViewport(widthDp = widthDp, heightDp = 1200, recompose = true)

            val statBoxTags = listOf(
                "overview_habit_active_dates",
                "overview_journal_entries_count",
                "overview_daily_opportunities_met",
                "overview_weekly_opportunities_met",
                "overview_unique_habits_completed",
                "overview_planned_rest_count"
            )

            for (statTag in statBoxTags) {
                val statNode = findNodeByTag(statTag)
                val descendants = collectDescendantNodes(statNode)
                var checkedAtLeastOneText = false
                for (desc in descendants) {
                    val getLayout = desc.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: continue
                    val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    if (getLayout.invoke(results) && results.isNotEmpty()) {
                        checkedAtLeastOneText = true
                        val layoutResult = results.first()
                        for (lineIdx in 0 until layoutResult.lineCount) {
                            assertFalse(
                                "At ${widthDp}dp, text inside $statTag must NOT be ellipsized on line $lineIdx ('${layoutResult.layoutInput.text.text}')",
                                layoutResult.isLineEllipsized(lineIdx)
                            )
                        }
                    }
                }
                assertTrue("Expected text nodes inside $statTag at ${widthDp}dp", checkedAtLeastOneText)
            }

            // Paired cards in each row must share balanced height
            val rowPairs = listOf(
                "overview_habit_active_dates" to "overview_journal_entries_count",
                "overview_daily_opportunities_met" to "overview_weekly_opportunities_met",
                "overview_unique_habits_completed" to "overview_planned_rest_count"
            )
            for ((leftTag, rightTag) in rowPairs) {
                val leftBounds = findNodeByTag(leftTag).boundsInRoot
                val rightBounds = findNodeByTag(rightTag).boundsInRoot
                assertEquals(
                    "At ${widthDp}dp, paired OverviewStatBox cards ($leftTag and $rightTag) must have equal height",
                    leftBounds.height,
                    rightBounds.height,
                    1.5f
                )
            }
        }

        // 4. Verify TodayScreen with 1 habit also keeps Today's Seeds header and HabitCard clear of FAB
        val today = testClock.todayLocal()
        repo.setSettingSync("onboarding_completed", "BOOL", "true")
        vm.markColdLaunchSceneShown()
        repo.saveHabitSync(
            Habit(
                habitId = "h_resp_1",
                name = "Morning Hydration",
                description = "Drink a full glass of water",
                trackingMode = TrackingMode.QUANTITY,
                unitCode = "cups",
                targetPeriod = TargetPeriod.DAY,
                targetValue = 2,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#286143",
                sortOrder = 0
            ),
            effectiveDateOverride = today
        )
        vm.selectTab(NavTab.TODAY)
        pumpViewport(widthDp = 360, heightDp = 760, recompose = true)

        val fabWithOneHabit = findNodeByTag("add_habit_fab").boundsInRoot
        val seedsHeaderWithOneHabit = findNodeByTag("today_seeds_section_header").boundsInRoot
        val tendedCountWithOneHabit = findNodeByTag("today_tended_count_text").boundsInRoot
        val habitCardWithOneHabit = findNodeByTag("habit_card_h_resp_1").boundsInRoot
        assertFalse(
            "With 1 habit on TodayScreen, add_habit_fab must not overlap Today's Seeds header",
            fabWithOneHabit.overlaps(seedsHeaderWithOneHabit)
        )
        assertFalse(
            "With 1 habit on TodayScreen, add_habit_fab must not overlap tended count badge",
            fabWithOneHabit.overlaps(tendedCountWithOneHabit)
        )
        assertFalse(
            "With 1 habit on TodayScreen, add_habit_fab ($fabWithOneHabit) must not overlap first HabitCard ($habitCardWithOneHabit)",
            fabWithOneHabit.overlaps(habitCardWithOneHabit)
        )

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }
}
