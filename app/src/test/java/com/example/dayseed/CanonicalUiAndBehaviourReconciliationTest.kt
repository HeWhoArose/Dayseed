package com.example.dayseed

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayRecordState
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.DerivedDayStatus
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.ProgressBand
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.components.DAYSEED_BRAND_NAME
import com.example.dayseed.ui.components.DAYSEED_OFFICIAL_MOTTO
import com.example.dayseed.ui.screens.SettingsSheet
import com.example.dayseed.ui.theme.DayJournalSurface
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigInteger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Comprehensive Robolectric regression test suite for the Canonical UI and
 * Product-Behaviour Reconciliation (D-231):
 * - §10.1 Five-tab canonical navigation and secondary route state transitions across all five theme modes
 * - §10.2 Fresh database and automatic cleanup of historical `4c1187a` v1 demonstration data
 * - §10.3 Evidence-derived habit recording without manual Tiny/Usual/Extra selection
 * - §10.4 Unified DaypartBackgroundLayer artwork resolution, LIGHT vs SYSTEM-light palette coherence, and reduced motion
 * - §10.5 Settings reachability, persistence, and dead-code removal
 * - §10.6 Cold-launch brand presentation and About build provenance
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

    private fun seedHistorical4c1187aDemoDataInV3(db: SQLiteDatabase, includeUserRecords: Boolean) {
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

            if (seed.id == "h_demo_1") {
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
                    put("body", "Demo note")
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

        // Verify secondary routes open and pop cleanly back to primary tab
        vm.selectTab(NavTab.TODAY)
        vm.openCreateEditHabit(null)
        assertTrue(vm.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit)
        vm.navigateBack()
        assertNull(vm.currentSecondaryRoute.value)
        assertEquals(NavTab.TODAY, vm.selectedTab.value)

        repo.dbHelper.close()
    }

    // =========================================================================
    // §10.2 Fresh Database & Automatic Historical 4c1187a Demo-Data Cleanup
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
        val dbName = "legacy_demo_only.db"
        tempDbNames.add(dbName)
        val helper = DayseedDatabaseHelper(context, dbName)
        val db = helper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, "setting_key = ?", arrayOf(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED))
        seedHistorical4c1187aDemoDataInV3(db, includeUserRecords = false)

        val removedCount = helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true)
        assertEquals(4, removedCount) // 3 demo habits + 1 demo journal entry

        val repo = DayseedRepository(context, clock = testClock, databaseName = dbName)
        assertEquals(0, repo.loadHabitsSync().size)
        assertEquals(0, repo.loadJournalEntriesSync().size)
        assertEquals("false", repo.getSettingString("onboarding_completed", "true"))
        assertEquals("true", repo.getSettingString(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED, "false"))

        // Verify PRAGMA foreign_key_check and integrity_check
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
    fun mixedDemoAndUserDatabaseRemovesOnlyExactDemoItemsAndPreservesAllUserRecords() {
        val dbName = "mixed_demo_user.db"
        tempDbNames.add(dbName)
        val helper = DayseedDatabaseHelper(context, dbName)
        val db = helper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_APP_SETTINGS, "setting_key = ?", arrayOf(DayseedDatabaseHelper.SETTING_LEGACY_V1_DEMO_CLEANUP_COMPLETED))
        seedHistorical4c1187aDemoDataInV3(db, includeUserRecords = true)

        val removedCount = helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true)
        assertEquals(4, removedCount)

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
        assertEquals(1, remainingJournals.size)
        assertEquals("user_journal_1", remainingJournals.first().id)
        assertEquals("Real personal note", remainingJournals.first().title)

        // Because user habits remain, onboarding_completed must stay "true"
        assertEquals("true", repo.getSettingString("onboarding_completed", "false"))

        // Idempotency check: running cleanup again must be a no-op
        assertEquals(0, helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = false))
        assertEquals(0, helper.cleanupLegacyV1DemoDataIfNeeded(db, forceRecheck = true))
        assertEquals(1, repo.loadHabitsSync().size)
        assertEquals(1, repo.loadJournalEntriesSync().size)

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

        // 1. Quantity habit with target = 8 glasses/day
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

        // 2. Duration habit with target = 20 minutes/day
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
            Triple(baseDate.plusDays(1), 1, ProgressBand.TINY_STEP),       // 12.5% (>0..<25%)
            Triple(baseDate.plusDays(2), 3, ProgressBand.GETTING_STARTED), // 37.5% (25..<50%)
            Triple(baseDate.plusDays(3), 4, ProgressBand.HALFWAY),         // 50.0% (50..<75%)
            Triple(baseDate.plusDays(4), 6, ProgressBand.ALMOST_THERE),    // 75.0% (75..<100%)
            Triple(baseDate.plusDays(5), 8, ProgressBand.GOAL_MET),        // 100.0%
            Triple(baseDate.plusDays(6), 10, ProgressBand.EXTRA),          // 125.0% (>100..150%)
            Triple(baseDate.plusDays(7), 14, ProgressBand.HUGE)            // 175.0% (>150%)
        )

        for ((date, value, expectedBand) in quantityCases) {
            repo.logHabitEntrySync("h_qty_8", date, value)
            val detail = repo.computeDayDetailSummary(date, testClock.todayLocal())
            val item = detail.habitItems.first { it.habit.habitId == "h_qty_8" }
            assertEquals("Quantity=$value on $date must derive $expectedBand", expectedBand, item.progressBand)
        }

        val durationCases = listOf(
            Triple(baseDate.plusDays(0), 0, ProgressBand.ZERO),
            Triple(baseDate.plusDays(1), 3, ProgressBand.TINY_STEP),       // 15.0%
            Triple(baseDate.plusDays(2), 6, ProgressBand.GETTING_STARTED), // 30.0%
            Triple(baseDate.plusDays(3), 10, ProgressBand.HALFWAY),        // 50.0%
            Triple(baseDate.plusDays(4), 16, ProgressBand.ALMOST_THERE),   // 80.0%
            Triple(baseDate.plusDays(5), 20, ProgressBand.GOAL_MET),       // 100.0%
            Triple(baseDate.plusDays(6), 25, ProgressBand.EXTRA),          // 125.0%
            Triple(baseDate.plusDays(7), 35, ProgressBand.HUGE)            // 175.0%
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

        // Complete via unified EffortLevel.STANDARD path used by HabitCard "Complete" button
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

        // Creating a new EFFORT_AWARE habit without allowLegacyEffortModeCreation must throw
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

        // Pre-existing legacy EFFORT_AWARE habit remains readable and historical outcomes evaluate properly
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

        // Simulate Android OS accessibility disabling animator duration scale (0.0f)
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

        // 1. Theme mode persistence
        vm.setThemeMode(ThemeModeOption.DARK_DAYPART_ART)
        repo.appSettings.awaitMatch { it["theme_mode"]?.valueText == "DARK_DAYPART_ART" }
        assertEquals("DARK_DAYPART_ART", repo.getSettingString("theme_mode", ""))

        // 2. Week start persistence
        vm.setWeekStart(DayOfWeek.SUNDAY)
        repo.appSettings.awaitMatch { it["week_start"]?.valueText == "SUNDAY" }
        assertEquals("SUNDAY", repo.getSettingString("week_start", ""))

        // 3. Quiet hours persistence
        vm.setQuietHours("22:15", "07:30")
        repo.appSettings.awaitMatch {
            it["quiet_hours_start"]?.valueText == "22:15" &&
                it["quiet_hours_end"]?.valueText == "07:30"
        }
        assertEquals("22:15", repo.getSettingString("quiet_hours_start", ""))
        assertEquals("07:30", repo.getSettingString("quiet_hours_end", ""))

        // 4. Verify dead SettingsSheet.JOURNAL_EXPORT enum entry was removed
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

        // Verify official brand constants
        assertEquals("Dayseed", DAYSEED_BRAND_NAME)
        assertEquals("Your progress is more than your streak.", DAYSEED_OFFICIAL_MOTTO)

        // Cold launch starts with hasShownColdLaunchScene = false, then transitions once marked
        assertFalse(vm.hasShownColdLaunchScene.value)
        vm.markColdLaunchSceneShown()
        assertTrue(vm.hasShownColdLaunchScene.value)

        // Navigating across tabs keeps hasShownColdLaunchScene = true so it never replays
        vm.selectTab(NavTab.HABITS)
        vm.selectTab(NavTab.TODAY)
        assertTrue(vm.hasShownColdLaunchScene.value)

        // Verify APK build provenance fields in BuildConfig
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("1.0.1", BuildConfig.VERSION_NAME)
        assertEquals("com.aistudio.dayseed.wkqmfl", BuildConfig.APPLICATION_ID)
        assertTrue("GIT_COMMIT_SHA must be non-blank", BuildConfig.GIT_COMMIT_SHA.isNotBlank())

        repo.dbHelper.close()
    }
}
