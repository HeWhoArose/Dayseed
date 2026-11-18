package com.example.dayseed

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitDurationKind
import com.example.dayseed.data.HabitEndReason
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitLifetimeCalculator
import com.example.dayseed.data.HabitLifetimeStatus
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.ReminderEngine
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.screens.CreateEditHabitScreen
import com.example.dayseed.ui.screens.HabitDetailScreen
import com.example.dayseed.ui.theme.DayseedTheme
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
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HabitLifecycleAndEndDateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var dbName: String
    private lateinit var mutableClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        dbName = "test_habit_lifecycle_${UUID.randomUUID()}.db"
        mutableClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 1),
            currentLocalTime = LocalTime.of(8, 0),
            currentUtcMs = 1_762_000_000_000L
        )
        repository = DayseedRepository(context, clock = mutableClock, databaseName = dbName)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
        context.deleteDatabase(dbName)
    }

    private fun activeTodayHabitIds(repo: DayseedRepository = repository): Set<String> {
        return repo.habits.value
            .filter { it.lifecycleState == HabitLifecycleState.ACTIVE }
            .map { it.habitId }
            .toSet()
    }

    private fun findComposeView(view: View): androidx.compose.ui.platform.ComposeView? {
        if (view is androidx.compose.ui.platform.ComposeView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findComposeView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun findRootForTest(view: View): RootForTest? {
        if (view is RootForTest) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findRootForTest(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun getAllWindowViews(activity: ComponentActivity): List<View> {
        val decor = activity.window.decorView
        val views = runCatching {
            val wmGlobalClass = Class.forName("android.view.WindowManagerGlobal")
            val instance = wmGlobalClass.getMethod("getInstance").invoke(null)
            val mViewsField = wmGlobalClass.getDeclaredField("mViews").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            (mViewsField.get(instance) as? List<View>)?.toList()
        }.getOrNull()
        if (!views.isNullOrEmpty()) {
            val idx = views.indexOf(decor)
            if (idx >= 0) {
                return views.subList(idx, views.size)
            }
        }
        return listOf(decor)
    }

    private fun pumpActivity(
        activity: ComponentActivity,
        widthDp: Int = 420,
        heightDp: Int = 2400
    ) {
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60))
        val density = activity.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        for (decor in getAllWindowViews(activity)) {
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            decor.layout(0, 0, widthPx, heightPx)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60))
    }

    private fun collectAllNodes(activity: ComponentActivity): List<SemanticsNode> {
        val result = mutableListOf<SemanticsNode>()
        fun collectRoots(view: View): List<RootForTest> {
            val roots = mutableListOf<RootForTest>()
            if (view is RootForTest) roots.add(view)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    roots.addAll(collectRoots(view.getChildAt(i)))
                }
            }
            return roots
        }
        val roots = getAllWindowViews(activity).flatMap { collectRoots(it) }
        assertTrue("AndroidComposeView RootForTest must be attached", roots.isNotEmpty())
        fun walk(node: SemanticsNode) {
            result.add(node)
            for (child in node.children) {
                walk(child)
            }
        }
        for (root in roots) {
            walk(root.semanticsOwner.unmergedRootSemanticsNode)
        }
        return result
    }

    private fun findNodeByTagOrNull(activity: ComponentActivity, tag: String): SemanticsNode? {
        return collectAllNodes(activity).firstOrNull {
            it.config.getOrNull(SemanticsProperties.TestTag) == tag
        }
    }

    private fun findNodeByTag(activity: ComponentActivity, tag: String): SemanticsNode {
        val node = findNodeByTagOrNull(activity, tag)
        assertNotNull("Expected SemanticsNode with testTag '$tag'", node)
        return node!!
    }

    private fun nodeText(node: SemanticsNode): String {
        val texts = mutableListOf<String>()
        fun collectText(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { texts.add(it.text) }
            n.config.getOrNull(SemanticsProperties.EditableText)?.let { texts.add(it.text) }
            for (child in n.children) {
                collectText(child)
            }
        }
        collectText(node)
        return texts.joinToString(" ")
    }

    private fun clickTag(activity: ComponentActivity, tag: String) {
        pumpActivity(activity)
        val node = findNodeByTag(activity, tag)
        val clickAction = node.config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Expected OnClick action on node '$tag'", clickAction)
        clickAction!!.invoke()
        pumpActivity(activity)
    }

    private fun setTextTag(activity: ComponentActivity, tag: String, text: String) {
        pumpActivity(activity)
        val node = findNodeByTag(activity, tag)
        val setTextAction = node.config.getOrNull(SemanticsActions.SetText)?.action
        assertNotNull("Expected SetText action on node '$tag'", setTextAction)
        setTextAction!!.invoke(AnnotatedString(text))
        pumpActivity(activity)
    }

    // =========================================================================
    // 1. HABIT CREATION & CALENDAR-AWARE DURATION ARITHMETIC
    // =========================================================================

    @Test
    fun creation_allDurationOptionsAndCalendarAwareDateArithmetic_workAndRejectInvalidInputs() {
        val start = LocalDate.of(2026, 1, 31)
        mutableClock.currentLocalDate = start

        // 1. No end date (default)
        val ongoing = Habit(
            habitId = "h_ongoing",
            name = "Morning Stretch",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            durationKind = HabitDurationKind.NONE
        )
        repository.saveHabitSync(ongoing)
        val savedOngoing = repository.habits.value.first { it.habitId == "h_ongoing" }
        assertEquals(HabitDurationKind.NONE, savedOngoing.durationKind)
        assertNull(savedOngoing.scheduledEndLocalDate)
        assertNull(savedOngoing.durationValue)
        assertEquals(HabitLifetimeStatus.ONGOING, savedOngoing.lifetimeStatus)
        assertEquals("Ongoing", savedOngoing.lifetimeStatusLabel)

        // 2. Days duration (30 days from 2026-01-31 -> 2026-03-02)
        val byDays = Habit(
            habitId = "h_days",
            name = "Drink 4 cups of water",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cups",
            targetValue = 4,
            durationKind = HabitDurationKind.DAYS,
            durationValue = 30
        )
        repository.saveHabitSync(byDays)
        val savedDays = repository.habits.value.first { it.habitId == "h_days" }
        assertEquals("2026-03-02", savedDays.scheduledEndLocalDate)
        assertEquals(HabitDurationKind.DAYS, savedDays.durationKind)
        assertEquals(30, savedDays.durationValue)
        assertEquals(HabitLifetimeStatus.SCHEDULED_TO_END, savedDays.lifetimeStatus)

        // 3. Weeks duration (2 weeks from 2026-01-31 -> 2026-02-14)
        val byWeeks = Habit(
            habitId = "h_weeks",
            name = "Evening Walk",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetValue = 20,
            durationKind = HabitDurationKind.WEEKS,
            durationValue = 2
        )
        repository.saveHabitSync(byWeeks)
        val savedWeeks = repository.habits.value.first { it.habitId == "h_weeks" }
        assertEquals("2026-02-14", savedWeeks.scheduledEndLocalDate)
        assertEquals(HabitDurationKind.WEEKS, savedWeeks.durationKind)
        assertEquals(2, savedWeeks.durationValue)

        // 4. Months duration (calendar-aware: 1 month from Jan 31, 2026 -> Feb 28, 2026; 3 months -> Apr 30, 2026)
        assertEquals(
            LocalDate.of(2026, 2, 28),
            HabitLifetimeCalculator.calculateEndDateResult(start, HabitDurationKind.MONTHS, 1, null).getOrThrow()
        )
        val byMonths = Habit(
            habitId = "h_months",
            name = "Read 10 pages",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetValue = 10,
            durationKind = HabitDurationKind.MONTHS,
            durationValue = 3
        )
        repository.saveHabitSync(byMonths)
        val savedMonths = repository.habits.value.first { it.habitId == "h_months" }
        assertEquals("2026-04-30", savedMonths.scheduledEndLocalDate)
        assertEquals(HabitDurationKind.MONTHS, savedMonths.durationKind)
        assertEquals(3, savedMonths.durationValue)

        // 5. Years duration (calendar-aware: 1 year from 2024-02-29 leap day -> 2025-02-28)
        assertEquals(
            LocalDate.of(2025, 2, 28),
            HabitLifetimeCalculator.calculateEndDateResult(
                LocalDate.of(2024, 2, 29),
                HabitDurationKind.YEARS,
                1,
                null
            ).getOrThrow()
        )
        val byYears = Habit(
            habitId = "h_years",
            name = "Practice Spanish",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            durationKind = HabitDurationKind.YEARS,
            durationValue = 1
        )
        repository.saveHabitSync(byYears)
        val savedYears = repository.habits.value.first { it.habitId == "h_years" }
        assertEquals("2027-01-31", savedYears.scheduledEndLocalDate)
        assertEquals(HabitDurationKind.YEARS, savedYears.durationKind)
        assertEquals(1, savedYears.durationValue)

        // 6. Specific calendar end date (2026-12-25)
        val bySpecificDate = Habit(
            habitId = "h_specific",
            name = "Holiday Reading Goal",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetValue = 15,
            durationKind = HabitDurationKind.SPECIFIC_DATE,
            scheduledEndLocalDate = "2026-12-25"
        )
        repository.saveHabitSync(bySpecificDate)
        val savedSpecific = repository.habits.value.first { it.habitId == "h_specific" }
        assertEquals("2026-12-25", savedSpecific.scheduledEndLocalDate)
        assertEquals(HabitDurationKind.SPECIFIC_DATE, savedSpecific.durationKind)
        assertNull(savedSpecific.durationValue)

        // 7. Reject invalid or past end dates
        assertTrue(HabitLifetimeCalculator.calculateEndDateResult(start, HabitDurationKind.DAYS, 0, null).isFailure)
        assertTrue(HabitLifetimeCalculator.calculateEndDateResult(start, HabitDurationKind.WEEKS, -2, null).isFailure)
        assertTrue(HabitLifetimeCalculator.calculateEndDateResult(start, HabitDurationKind.SPECIFIC_DATE, null, "not-a-date").isFailure)
        assertTrue(HabitLifetimeCalculator.calculateEndDateResult(start, HabitDurationKind.SPECIFIC_DATE, null, "2026-01-30").isFailure)

        val pastEndHabit = Habit(
            habitId = "h_past_invalid",
            name = "Invalid Past End",
            startLocalDate = "2026-01-31",
            scheduledEndLocalDate = "2026-01-15",
            durationKind = HabitDurationKind.SPECIFIC_DATE
        )
        val saveFailed = runCatching { repository.saveHabitSync(pastEndHabit) }.isFailure
        assertTrue("Saving a habit with an end date before its start date must fail", saveFailed)
    }

    // =========================================================================
    // 2. LIFECYCLE PERSISTENCE, EDITING, REMOVAL & V3->V4 MIGRATION
    // =========================================================================

    @Test
    fun persistence_reloadEditRemoveEndDateAndMigrateV3ToV4_preservesDataAndCompatibility() {
        mutableClock.currentLocalDate = LocalDate.of(2026, 11, 1)

        val habit = Habit(
            habitId = "h_persist",
            name = "Journal 10 min",
            trackingMode = TrackingMode.DURATION,
            unitCode = "min",
            targetValue = 10,
            durationKind = HabitDurationKind.DAYS,
            durationValue = 14
        )
        repository.saveHabitSync(habit)
        assertEquals("2026-11-15", repository.habits.value.first { it.habitId == "h_persist" }.scheduledEndLocalDate)

        // Reload from disk with a new DayseedRepository instance
        repository.dbHelper.close()
        val reloadedRepo = DayseedRepository(context, clock = mutableClock, databaseName = dbName)
        try {
            val reloaded = reloadedRepo.habits.value.first { it.habitId == "h_persist" }
            assertEquals("2026-11-01", reloaded.startLocalDate)
            assertEquals("2026-11-15", reloaded.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.DAYS, reloaded.durationKind)
            assertEquals(14, reloaded.durationValue)
            assertEquals(HabitLifetimeStatus.SCHEDULED_TO_END, reloaded.lifetimeStatus)

            // Edit existing scheduled end date to a specific date
            val edited = reloadedRepo.updateHabitEndDateSync(
                habitId = "h_persist",
                durationKind = HabitDurationKind.SPECIFIC_DATE,
                durationValue = null,
                specificEndDate = "2026-12-31"
            )
            assertEquals("2026-12-31", edited.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.SPECIFIC_DATE, edited.durationKind)
            assertNull(edited.durationValue)

            // Remove scheduled end date and return to ongoing
            val cleared = reloadedRepo.clearHabitEndDateSync("h_persist")
            assertNull(cleared.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.NONE, cleared.durationKind)
            assertNull(cleared.durationValue)
            assertEquals(HabitLifecycleState.ACTIVE, cleared.lifecycleState)
            assertEquals(HabitLifetimeStatus.ONGOING, cleared.lifetimeStatus)
        } finally {
            reloadedRepo.dbHelper.close()
        }

        // Verify v3 -> v4 schema migration preserves existing v3 habits and backfills start_local_date
        val v3DbName = "test_v3_upgrade_${UUID.randomUUID()}.db"
        try {
            val v3SeedHelper = object : SQLiteOpenHelper(context, v3DbName, null, 3) {
                override fun onCreate(db: SQLiteDatabase) {
                    val helper = DayseedDatabaseHelper(context, null)
                    helper.createV3Schema(db)
                    db.execSQL("INSERT INTO habits (habit_id, created_at_utc_ms, updated_at_utc_ms, category, lifetime_goal_days) VALUES ('h_legacy_v3', 1000, 1000, 'Health', 66);")
                    db.execSQL("INSERT INTO habit_versions VALUES ('v_legacy_1', 'h_legacy_v3', '2026-10-10', 'Legacy Water', 'Drink water', 'QUANTITY', 'cups', 'DAY', 4, 'DAILY', NULL, NULL, 'ACTIVE', 'OFF', NULL, 'Tiny', 'Usual', 'Extra', '#286143', 0, 1000);")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            v3SeedHelper.writableDatabase.close()

            val upgradedRepo = DayseedRepository(context, clock = mutableClock, databaseName = v3DbName)
            try {
                val migratedHabit = upgradedRepo.habits.value.first { it.habitId == "h_legacy_v3" }
                assertEquals("2026-10-10", migratedHabit.startLocalDate)
                assertNull(migratedHabit.scheduledEndLocalDate)
                assertEquals(HabitDurationKind.NONE, migratedHabit.durationKind)
                assertEquals(HabitLifetimeStatus.ONGOING, migratedHabit.lifetimeStatus)
            } finally {
                upgradedRepo.dbHelper.close()
            }
        } finally {
            context.deleteDatabase(v3DbName)
        }
    }

    // =========================================================================
    // 3. AUTOMATIC COMPLETION ON SCHEDULED END DATE
    // =========================================================================

    @Test
    fun automaticCompletion_reachingScheduledEndDate_transitionsToCompletedAndPreservesHistory() {
        val startDate = LocalDate.of(2026, 11, 1)
        mutableClock.currentLocalDate = startDate

        val habit = Habit(
            habitId = "h_auto_complete",
            name = "7-Day Reading Sprint",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetValue = 10,
            durationKind = HabitDurationKind.DAYS,
            durationValue = 7 // Scheduled end date = 2026-11-08
        )
        repository.saveHabitSync(habit)
        assertEquals("2026-11-08", repository.habits.value.first().scheduledEndLocalDate)

        // Log qualifying entries on days 1..7 (2026-11-01 .. 2026-11-07) so it reaches a 7-day Grove tree
        for (offset in 0..6L) {
            val d = startDate.plusDays(offset)
            mutableClock.currentLocalDate = d
            mutableClock.currentUtcMs += 86_400_000L
            repository.logHabitEntrySync("h_auto_complete", d, 10, "Read on $d")
        }
        repository.saveHabitDateNoteSync("h_auto_complete", startDate, "Started strong")
        repository.saveJournalEntrySync(
            JournalEntry(
                journalEntryId = "j_linked_1",
                localDate = startDate.toString(),
                title = "Day 1",
                body = "Enjoyed the chapter",
                linkedHabitId = "h_auto_complete"
            )
        )

        // On 2026-11-07 (before end date), habit is still ACTIVE and on Today screen
        assertEquals(HabitLifecycleState.ACTIVE, repository.habits.value.first().lifecycleState)
        assertTrue(activeTodayHabitIds().contains("h_auto_complete"))

        // Advance clock to scheduled end date (2026-11-08) and evaluate
        mutableClock.currentLocalDate = LocalDate.of(2026, 11, 8)
        val completedCount = repository.evaluateScheduledHabitEndingsSync()
        assertEquals(1, completedCount)

        val completedHabit = repository.habits.value.first { it.habitId == "h_auto_complete" }
        assertEquals(HabitLifecycleState.COMPLETED, completedHabit.lifecycleState)
        assertEquals("2026-11-08", completedHabit.actualEndLocalDate)
        assertEquals(HabitEndReason.SCHEDULED_COMPLETED, completedHabit.endReason)
        assertEquals(HabitLifetimeStatus.COMPLETED, completedHabit.lifetimeStatus)
        assertEquals("Completed", completedHabit.lifetimeStatusLabel)

        // Removed from active Today habits
        assertFalse(activeTodayHabitIds().contains("h_auto_complete"))
        // Does not reappear on future dates via date navigation
        assertFalse(repository.getHabitsRelevantForDate(LocalDate.of(2026, 11, 9)).any { it.first.habitId == "h_auto_complete" })
        // Accessible on past dates where entries were logged
        assertTrue(repository.getHabitsRelevantForDate(LocalDate.of(2026, 11, 3)).any { it.first.habitId == "h_auto_complete" })

        // Historical entries, streak runs, date notes, and journal associations remain intact
        assertEquals(7, repository.habitEntries.value.count { it.habitId == "h_auto_complete" })
        assertEquals(1, repository.habitDateNotes.value.count { it.habitId == "h_auto_complete" })
        assertEquals("h_auto_complete", repository.journalEntries.value.first().linkedHabitId)
        val groveRuns = repository.habitStreakRuns.value.filter { it.habitId == "h_auto_complete" && it.qualifiedForGrove }
        assertEquals(1, groveRuns.size)
        assertEquals(7, groveRuns.first().completedStreakCount)

        // Repeated evaluation is idempotent
        val secondRunCount = repository.evaluateScheduledHabitEndingsSync()
        assertEquals(0, secondRunCount)
    }

    // =========================================================================
    // 4. MANUAL COMPLETION & EARLY ENDING WITHOUT COMPLETION
    // =========================================================================

    @Test
    fun manualEnding_markAsCompletedVsEndWithoutCompletion_distinguishesStatesAndPreservesHistory() {
        val today = LocalDate.of(2026, 11, 5)
        mutableClock.currentLocalDate = today

        val habitComplete = Habit(
            habitId = "h_manual_complete",
            name = "Finish Book",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "pages",
            targetValue = 10
        )
        val habitEnded = Habit(
            habitId = "h_manual_ended",
            name = "Cold Shower Trial",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            durationKind = HabitDurationKind.DAYS,
            durationValue = 30
        )
        repository.saveHabitSync(habitComplete)
        repository.saveHabitSync(habitEnded)

        repository.logHabitEntrySync("h_manual_complete", today, 15, "Finished final chapter")
        repository.logHabitDayOutcomeSync("h_manual_ended", today, "COMPLETED", null, "Tried it")

        // 1. End early as Completed
        val endedAsCompleted = repository.endHabitImmediatelySync("h_manual_complete", markAsCompleted = true)
        assertEquals(HabitLifecycleState.COMPLETED, endedAsCompleted.lifecycleState)
        assertEquals(today.toString(), endedAsCompleted.actualEndLocalDate)
        assertEquals(HabitEndReason.MANUAL_COMPLETED, endedAsCompleted.endReason)
        assertEquals(HabitLifetimeStatus.COMPLETED, endedAsCompleted.lifetimeStatus)
        assertEquals("Completed", endedAsCompleted.lifetimeStatusLabel)

        // 2. End early without completion
        val endedWithoutCompletion = repository.endHabitImmediatelySync("h_manual_ended", markAsCompleted = false)
        assertEquals(HabitLifecycleState.ENDED, endedWithoutCompletion.lifecycleState)
        assertEquals(today.toString(), endedWithoutCompletion.actualEndLocalDate)
        assertEquals(HabitEndReason.MANUAL_ENDED, endedWithoutCompletion.endReason)
        assertEquals(HabitLifetimeStatus.ENDED, endedWithoutCompletion.lifetimeStatus)
        assertEquals("Ended", endedWithoutCompletion.lifetimeStatusLabel)

        // Neither appears on Today
        val todayIds = activeTodayHabitIds()
        assertFalse(todayIds.contains("h_manual_complete"))
        assertFalse(todayIds.contains("h_manual_ended"))

        // Neither reappears on future date navigation
        val tomorrow = today.plusDays(1)
        assertFalse(repository.getHabitsRelevantForDate(tomorrow).any { it.first.habitId == "h_manual_complete" })
        assertFalse(repository.getHabitsRelevantForDate(tomorrow).any { it.first.habitId == "h_manual_ended" })

        // Historical entries and outcomes on `today` are preserved
        assertEquals(1, repository.habitEntries.value.count { it.habitId == "h_manual_complete" })
        assertEquals(1, repository.habitDayOutcomes.value.count { it.habitId == "h_manual_ended" })
    }

    // =========================================================================
    // 5. REMINDER CANCELLATION & ISOLATION
    // =========================================================================

    @Test
    fun reminders_cancelledOnScheduledOrManualEnding_andActiveHabitsRemainUnaffected() {
        val start = LocalDate.of(2026, 11, 1)
        mutableClock.currentLocalDate = start
        mutableClock.currentLocalTime = LocalTime.of(7, 30)

        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "BOOL", "true")

        val activeHabit = Habit(
            habitId = "h_rem_active",
            name = "Active Reminder Habit",
            reminderMode = "AT_TIME",
            reminderTimeLocal = "09:00"
        )
        val expiringHabit = Habit(
            habitId = "h_rem_expiring",
            name = "Expiring Reminder Habit",
            reminderMode = "AT_TIME",
            reminderTimeLocal = "09:00",
            durationKind = HabitDurationKind.DAYS,
            durationValue = 3 // Ends 2026-11-04
        )
        val manualEndHabit = Habit(
            habitId = "h_rem_manual",
            name = "Manual End Reminder Habit",
            reminderMode = "AT_TIME",
            reminderTimeLocal = "09:00"
        )
        repository.saveHabitSync(activeHabit)
        repository.saveHabitSync(expiringHabit)
        repository.saveHabitSync(manualEndHabit)

        ReminderScheduler.scheduleAll(context, repository)
        val initialScheduled = repository.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
        assertTrue(initialScheduled.contains("h_rem_active"))
        assertTrue(initialScheduled.contains("h_rem_expiring"))
        assertTrue(initialScheduled.contains("h_rem_manual"))

        // Manually end `h_rem_manual` without completion
        repository.endHabitImmediatelySync("h_rem_manual", markAsCompleted = false)
        val afterManualEndScheduled = repository.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
        assertFalse("Manually ended habit must be removed from scheduled alarm IDs", afterManualEndScheduled.contains("h_rem_manual"))
        assertTrue("Active habit must remain scheduled", afterManualEndScheduled.contains("h_rem_active"))

        // Advance clock to 2026-11-04 (scheduled end date of h_rem_expiring) and run ReminderScheduler.scheduleAll
        mutableClock.currentLocalDate = LocalDate.of(2026, 11, 4)
        ReminderScheduler.scheduleAll(context, repository)

        val afterExpiryScheduled = repository.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
        assertFalse("Automatically completed habit must be removed from scheduled alarm IDs", afterExpiryScheduled.contains("h_rem_expiring"))
        assertTrue("Other active habit's reminder must remain scheduled", afterExpiryScheduled.contains("h_rem_active"))

        val expiredHabit = repository.habits.value.first { it.habitId == "h_rem_expiring" }
        assertEquals(HabitLifecycleState.COMPLETED, expiredHabit.lifecycleState)
        assertEquals("OFF", expiredHabit.reminderMode)

        // Verify ReminderEngine also rejects delivery for ENDED or COMPLETED habits
        val endedEffective = repository.getEffectiveHabitVersion("h_rem_manual", LocalDate.of(2026, 11, 4))!!
        val gateResult = ReminderEngine.evaluateHabitReminder(
            db = repository.dbHelper.writableDatabase,
            habitVersion = endedEffective,
            targetDate = LocalDate.of(2026, 11, 4),
            isScheduledOpportunity = true,
            weeklyTargetMet = false,
            notificationsAvailable = true,
            alreadyLoggedToday = false,
            quietStartLocal = ReminderEngine.DEFAULT_QUIET_START,
            quietEndLocal = ReminderEngine.DEFAULT_QUIET_END,
            dailyCap = 3,
            nowUtcMs = mutableClock.nowUtcMs()
        )
        assertEquals("SUPPRESSED", gateResult.deliveryStatus)
        assertEquals("REMINDER_OFF_OR_INACTIVE", gateResult.suppressionReason)
    }

    // =========================================================================
    // 6. BACKUP & RESTORE (REPLACE AND MERGE)
    // =========================================================================

    @Test
    fun backupAndRestore_replaceAndMerge_preservesLifecycleFieldsAndStopsEndedReminders() {
        mutableClock.currentLocalDate = LocalDate.of(2026, 11, 10)
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "BOOL", "true")

        val scheduledHabit = Habit(
            habitId = "h_bk_scheduled",
            name = "Scheduled Habit",
            startLocalDate = "2026-11-01",
            scheduledEndLocalDate = "2026-12-01",
            durationKind = HabitDurationKind.DAYS,
            durationValue = 30,
            reminderMode = "AT_TIME",
            reminderTimeLocal = "08:30"
        )
        val endedHabit = Habit(
            habitId = "h_bk_ended",
            name = "Ended Early Habit",
            startLocalDate = "2026-11-01",
            scheduledEndLocalDate = "2026-11-15",
            durationKind = HabitDurationKind.WEEKS,
            durationValue = 2
        )
        repository.saveHabitSync(scheduledHabit)
        repository.saveHabitSync(endedHabit)
        repository.endHabitImmediatelySync("h_bk_ended", markAsCompleted = false)

        val backupJson = repository.exportPortableBackupSync()

        // Test REPLACE restore into a clean database
        val replaceDbName = "test_lifecycle_replace_${UUID.randomUUID()}.db"
        val replaceRepo = DayseedRepository(context, clock = mutableClock, databaseName = replaceDbName)
        try {
            val replaceRes = replaceRepo.restorePortableBackupSync(backupJson, restoreMode = "REPLACE")
            assertEquals("COMPLETED", replaceRes.sessionState)

            val restoredScheduled = replaceRepo.habits.value.first { it.habitId == "h_bk_scheduled" }
            assertEquals("2026-11-01", restoredScheduled.startLocalDate)
            assertEquals("2026-12-01", restoredScheduled.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.DAYS, restoredScheduled.durationKind)
            assertEquals(30, restoredScheduled.durationValue)
            assertEquals(HabitLifetimeStatus.SCHEDULED_TO_END, restoredScheduled.lifetimeStatus)

            val restoredEnded = replaceRepo.habits.value.first { it.habitId == "h_bk_ended" }
            assertEquals(HabitLifecycleState.ENDED, restoredEnded.lifecycleState)
            assertEquals("2026-11-10", restoredEnded.actualEndLocalDate)
            assertEquals(HabitEndReason.MANUAL_ENDED, restoredEnded.endReason)
            assertEquals(HabitLifetimeStatus.ENDED, restoredEnded.lifetimeStatus)

            // Re-enable reminders after restore (restore forces reminders_enabled off for safety)
            replaceRepo.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "BOOL", "true")
            ReminderScheduler.scheduleAll(context, replaceRepo)
            val scheduledIds = replaceRepo.getSettingString(ReminderScheduler.SETTING_SCHEDULED_ALARM_IDS, "")
            assertTrue(scheduledIds.contains("h_bk_scheduled"))
            assertFalse(scheduledIds.contains("h_bk_ended"))
        } finally {
            replaceRepo.dbHelper.close()
            context.deleteDatabase(replaceDbName)
        }

        // Test MERGE restore into another database
        val mergeDbName = "test_lifecycle_merge_${UUID.randomUUID()}.db"
        val mergeRepo = DayseedRepository(context, clock = mutableClock, databaseName = mergeDbName)
        try {
            val mergeRes = mergeRepo.restorePortableBackupSync(backupJson, restoreMode = "MERGE")
            assertTrue(mergeRes.sessionState in listOf("COMPLETED", "AWAITING_CONFLICT_RESOLUTION"))
            val mergedScheduled = mergeRepo.habits.value.first { it.habitId == "h_bk_scheduled" }
            assertEquals("2026-12-01", mergedScheduled.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.DAYS, mergedScheduled.durationKind)
            assertEquals(30, mergedScheduled.durationValue)
            val mergedEnded = mergeRepo.habits.value.first { it.habitId == "h_bk_ended" }
            assertEquals(HabitLifecycleState.ENDED, mergedEnded.lifecycleState)
            assertEquals("2026-11-10", mergedEnded.actualEndLocalDate)
            assertEquals(HabitEndReason.MANUAL_ENDED, mergedEnded.endReason)
        } finally {
            mergeRepo.dbHelper.close()
            context.deleteDatabase(mergeDbName)
        }
    }

    private fun <T> kotlinx.coroutines.flow.StateFlow<T>.awaitMatch(
        timeoutMs: Long = 3_000L,
        predicate: (T) -> Boolean
    ): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val current = this.value
            if (predicate(current)) return current
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(25))
            Thread.sleep(10)
        }
        val finalVal = this.value
        assertTrue("StateFlow did not match predicate within ${timeoutMs}ms (last value=$finalVal)", predicate(finalVal))
        return finalVal
    }

    // =========================================================================
    // 7. UI FLOW VERIFICATION (CREATE HABIT DURATION & DETAIL END CONFIRMATION)
    // =========================================================================

    @Test
    fun uiFlows_createHabitDurationAndDetailEndConfirmationDialog_workEndToEnd() {
        mutableClock.currentLocalDate = LocalDate.of(2026, 11, 1)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DayseedViewModel(app, customRepository = repository, ioDispatcher = Dispatchers.Unconfined)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()

        try {
            activity.setContent {
                DayseedTheme {
                    val route by vm.currentSecondaryRoute.collectAsState()
                    if (route is SecondaryRoute.HabitDetail) {
                        val detailRoute = route as SecondaryRoute.HabitDetail
                        HabitDetailScreen(
                            viewModel = vm,
                            habitId = detailRoute.habitId,
                            selectedLocalDate = detailRoute.selectedDate.toString(),
                            onSelectDate = {},
                            onOpenRecordSession = { _, _, _ -> },
                            onOpenEditHabit = {},
                            onOpenLivingGrove = {},
                            onNavigateBack = { vm.navigateBack() }
                        )
                    } else {
                        CreateEditHabitScreen(
                            viewModel = vm,
                            habitId = null,
                            onNavigateBack = {}
                        )
                    }
                }
            }
            pumpActivity(activity)

            // 1. Create a habit with a 30-day duration via CreateEditHabitScreen
            setTextTag(activity, "habit_name_input", "Drink 4 cups of water")
            clickTag(activity, "habit_duration_kind_DAYS")
            setTextTag(activity, "habit_duration_value_input", "30")

            val calculatedNode = findNodeByTag(activity, "habit_calculated_end_date_text")
            assertTrue(
                "Expected calculated end date preview to contain 2026-12-01, was '${nodeText(calculatedNode)}'",
                nodeText(calculatedNode).contains("2026-12-01")
            )

            clickTag(activity, "save_habit_btn")
            repository.habits.awaitMatch { it.isNotEmpty() }

            val createdHabit = repository.habits.value.first()
            assertEquals("2026-12-01", createdHabit.scheduledEndLocalDate)
            assertEquals(HabitDurationKind.DAYS, createdHabit.durationKind)
            assertEquals(30, createdHabit.durationValue)

            // 2. Open HabitDetailScreen and verify End this habit confirmation dialog (Cancel vs End Without Completion)
            vm.openHabitDetail(createdHabit.habitId, LocalDate.of(2026, 11, 1))
            pumpActivity(activity)

            val badgeNode = findNodeByTag(activity, "detail_lifecycle_badge")
            assertTrue(
                "Expected detail_lifecycle_badge to contain 'Scheduled to end', was '${nodeText(badgeNode)}'",
                nodeText(badgeNode).contains("Scheduled to end")
            )

            clickTag(activity, "detail_end_habit_btn")

            // Dialog is displayed with all 3 choices
            assertNotNull(findNodeByTagOrNull(activity, "end_habit_confirm_dialog"))
            assertNotNull(findNodeByTagOrNull(activity, "confirm_end_habit_completed_btn"))
            assertNotNull(findNodeByTagOrNull(activity, "confirm_end_habit_ended_btn"))
            clickTag(activity, "cancel_end_habit_btn")

            // Cancelling leaves the habit ACTIVE
            assertEquals(HabitLifecycleState.ACTIVE, repository.habits.value.first().lifecycleState)

            // Open dialog again and choose "End Without Completion"
            clickTag(activity, "detail_end_habit_btn")
            clickTag(activity, "confirm_end_habit_ended_btn")
            repository.habits.awaitMatch { it.firstOrNull()?.lifecycleState == HabitLifecycleState.ENDED }
            pumpActivity(activity)

            val endedHabit = repository.habits.value.first()
            assertEquals(HabitLifecycleState.ENDED, endedHabit.lifecycleState)
            assertEquals(HabitEndReason.MANUAL_ENDED, endedHabit.endReason)
            assertNull(
                "Active daily completion action must be hidden for ended habits",
                findNodeByTagOrNull(activity, "detail_record_session_btn")
            )
        } finally {
            controller.pause().stop()
        }
    }
}
