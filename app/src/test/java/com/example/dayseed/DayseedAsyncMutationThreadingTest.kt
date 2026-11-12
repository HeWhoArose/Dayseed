package com.example.dayseed

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * R3 follow-up: deterministic evidence that ViewModel mutations execute off
 * the main thread and that the UI-facing methods return without synchronously
 * waiting for the database.
 *
 * Mechanism (no sleeps, no timing dependence):
 *  - [MainDispatcherRule] pins Dispatchers.Main to an UnconfinedTestDispatcher,
 *    so each viewModelScope-launched mutation runs inline at the call site
 *    until its first REAL suspension (the repository's internal
 *    withContext(Dispatchers.IO) hop). A UI-facing method therefore returns
 *    while the database work is still in flight — direct evidence of
 *    "returns without waiting" (asserted via the absent completion message).
 *    Post-hop tails resume on the IO worker, never on a paused main queue.
 *  - The repository's own suspend functions hop to the real Dispatchers.IO,
 *    so after advancing the virtual scheduler the tests await the mutation's
 *    own observable completion signal (userMessage / repository StateFlow
 *    value) with a timeout — event-driven, not timing-based.
 *  - [RecordingDispatcher] records the thread each dispatched block executes
 *    on, proving the mutation body runs off the main thread.
 *  - [GateDispatcher] can hold a dispatch forever, so a ViewModel can be
 *    cleared (viewModelScope cancelled) mid-flight and the test can prove the
 *    pending database operation never executes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedAsyncMutationThreadingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: com.example.dayseed.data.DayseedRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 3, 15),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = LocalDate.of(2026, 3, 15)
                .atTime(10, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        repository = com.example.dayseed.data.DayseedRepository(
            context = app,
            clock = testClock,
            databaseName = null
        )
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    /** Event-driven wait helper is inlined at each call site (withTimeout + first). */

    private fun newViewModel(ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO): DayseedViewModel =
        DayseedViewModel(app, customRepository = repository, ioDispatcher = ioDispatcher)

    private fun quantityHabit(id: String, target: Int = 10) = Habit(
        habitId = id,
        name = "Habit $id",
        trackingMode = TrackingMode.QUANTITY,
        unitCode = "units",
        targetPeriod = TargetPeriod.DAY,
        targetValue = target,
        reminderMode = "OFF"
    )

    private fun completionHabit(id: String) = Habit(
        habitId = id,
        name = "Habit $id",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        reminderMode = "OFF"
    )

    // ------------------------------------------------------------------------
    // 1. Non-blocking return + background execution + off-main-thread proof
    // ------------------------------------------------------------------------

    @Test
    fun logHabitReturnsImmediatelyAndRunsLater() {
        val viewModel = newViewModel()
        viewModel.repository.saveHabitSync(
            completionHabit("h_async_log"),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        // Fire-and-forget: the method must return while the DB op is still queued.
        viewModel.logHabit(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_async_log" },
            date = LocalDate.of(2026, 3, 15)
        )
        assertNull("mutation must return without waiting for the DB write (no completion message yet)",
            viewModel.userMessage.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        // Event-driven completion wait (repository hops to the real IO pool).
        val message = runBlocking {
            withTimeout(10_000) { viewModel.userMessage.filterNotNull().first() }
        }
        assertTrue("user message should confirm completion", message.contains("Completed"))
        val summary = repository.loadHabitSummariesSync()["h_async_log"]!!
        assertEquals(
            "COMPLETED",
            repository.evaluateDerivedDayStatus("h_async_log", LocalDate.of(2026, 3, 15)).name
        )
    }

    @Test
    fun logHabitMutationBodyExecutesOffTheMainThread() {
        val recording = RecordingDispatcher()
        val viewModel = newViewModel(ioDispatcher = recording)
        val mainThread = Thread.currentThread()
        viewModel.repository.saveHabitSync(
            completionHabit("h_async_main"),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        viewModel.logHabit(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_async_main" },
            date = LocalDate.of(2026, 3, 15)
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        val message = runBlocking {
            withTimeout(10_000) { viewModel.userMessage.filterNotNull().first() }
        }
        assertTrue(message.contains("Completed"))

        // The mutation body dispatched on background threads, never the main thread.
        val used = recording.usedThreads
        assertTrue("mutation must dispatch at least once", used.isNotEmpty())
        assertTrue(
            "mutation body must not run on the main thread (threads: ${used.joinToString { it.name }})",
            used.none { it === mainThread || it.name == "main" }
        )
    }

    @Test
    fun logHabitQuantityOrDurationRecordsEntryAsynchronously() {
        val viewModel = newViewModel()
        viewModel.repository.saveHabitSync(
            quantityHabit("h_async_qty"),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )

        viewModel.logHabitQuantityOrDuration(
            habit = viewModel.repository.loadHabitsSync().single { it.habitId == "h_async_qty" },
            valueUnits = 5,
            date = LocalDate.of(2026, 3, 15),
            entryNote = "async"
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking {
            withTimeout(10_000) { viewModel.userMessage.filterNotNull().first { it.contains("+5") } }
        }

        assertEquals(5, repository.loadHabitSummariesSync()["h_async_qty"]!!.todayTotalUnits.toInt())
        assertTrue(repository.habitEntries.value.any { it.habitId == "h_async_qty" && it.valueUnits == 5 })
    }

    @Test
    fun failedDatabaseWriteSurfacesInStateAndLeavesDataUnchanged() {
        val viewModel = newViewModel()
        // Blank name is rejected by the repository validator -> save fails.
        val result = runBlocking {
            viewModel.saveHabitAsync(Habit(habitId = "h_bad", name = "   ", trackingMode = TrackingMode.COMPLETION_ONLY))
        }
        assertNull("failed save must return null", result)
        assertNotNull(viewModel.lastSaveError.value)
        assertEquals(0, repository.loadHabitsSync().size)
    }

    // ------------------------------------------------------------------------
    // 2. Habit entry edit, delete and undo through the async path
    // ------------------------------------------------------------------------

    @Test
    fun entryEditDeleteAndUndoWorkThroughAsyncPaths() {
        val viewModel = newViewModel()
        viewModel.repository.saveHabitSync(
            quantityHabit("h_async_undo"),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )
        viewModel.repository.logHabitEntrySync("h_async_undo", LocalDate.of(2026, 3, 15), 5, "original")

        // Edit asynchronously.
        val entry = repository.habitEntries.value.single { it.habitId == "h_async_undo" }
        viewModel.updateHabitEntry(entry.entryId, 8, "edited")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.userMessage.first { it == "Session updated" } } }
        assertEquals(8, repository.habitEntries.value.single { it.habitId == "h_async_undo" }.valueUnits)

        // Delete asynchronously (soft-delete creates an undo token).
        viewModel.deleteHabitEntry(entry.entryId)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.userMessage.first { it == "Session deleted" } } }
        assertTrue(repository.habitEntries.value.none { it.habitId == "h_async_undo" })

        // Undo asynchronously restores the deleted session.
        viewModel.triggerUndo()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.userMessage.first { it == "Action undone" } } }
        val restored = repository.habitEntries.value.single { it.habitId == "h_async_undo" }
        assertEquals(8, restored.valueUnits)
        assertEquals("edited", restored.entryNote)
    }

    // ------------------------------------------------------------------------
    // 3. Habit save through the async path + UI navigation state
    // ------------------------------------------------------------------------

    @Test
    fun saveHabitAsyncPersistsAndClosesEditorWhileWrapperIsFireAndForget() {
        val viewModel = newViewModel()
        viewModel.openCreateEditHabit(null)
        assertTrue(viewModel.isAddHabitOpen.value)

        val saved = runBlocking { viewModel.saveHabitAsync(quantityHabit("h_async_save")) }
        assertNotNull(saved)
        assertEquals("Habit saved (effective 2026-03-15)", viewModel.userMessage.value)
        assertFalse("editor must close after a successful save", viewModel.isAddHabitOpen.value)
        assertEquals(1, repository.loadHabitsSync().size)
    }

    @Test
    fun saveHabitWrapperDropsDuplicateSubmissionsWhileFirstIsInFlight() {
        val gate = GateDispatcher()
        val viewModel = newViewModel(ioDispatcher = gate)
        viewModel.openCreateEditHabit(null)

        val habit = quantityHabit("h_dup_save")
        viewModel.saveHabit(habit)   // first save queued (gate not released)
        assertTrue("first save must be marked in-flight", viewModel.isSaveInFlight.value)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()   // let the launch body reach withContext(gate)
        viewModel.saveHabit(habit)   // duplicate tap while first is in flight -> dropped
        assertEquals("duplicate submission must not enqueue a second save", 1, gate.pendingCount)

        gate.releaseAll()
        // Drive the gated save to completion step by step (event-driven):
        // 1. released above, the gate body runs until repository.saveHabit
        //    suspends on its real Dispatchers.IO hop;
        assertTrue("IO hop must re-dispatch its continuation on the gate", gate.awaitPending())
        // 2. released again, the body finishes (closes the editor, sets the message).
        gate.releaseAll()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()   // launch body's finally clears the guard
        assertEquals("Habit saved (effective 2026-03-15)", viewModel.userMessage.value)
        assertFalse("save must complete and clear the in-flight guard", viewModel.isSaveInFlight.value)
        assertEquals(1, repository.habitVersions.value.count { it.habitId == "h_dup_save" })

        // After completion, a new save is accepted again.
        viewModel.saveHabit(habit.copy(name = "Renamed"))
        assertTrue("new save accepted after the first completes", viewModel.isSaveInFlight.value)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        gate.releaseAll()
        assertTrue("rename IO hop must re-dispatch on the gate", gate.awaitPending())
        gate.releaseAll()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Renamed", repository.loadHabitsSync().single { it.habitId == "h_dup_save" }.name)
        assertFalse(viewModel.isSaveInFlight.value)
    }

    // ------------------------------------------------------------------------
    // 4. Journal save + draft preservation through the async path
    // ------------------------------------------------------------------------

    @Test
    fun journalSaveFailurePreservesDraftAndRetrySucceeds() {
        val viewModel = newViewModel()
        val today = LocalDate.of(2026, 3, 15)
        repository.saveJournalDraftSync(
            draftId = "draft_async",
            localDate = today,
            body = "Async draft body"
        )
        // The editor must be open (as it would be from the UI) so we can prove
        // it STAYS open on failure and closes only on success.
        viewModel.openJournalEditor(entry = null, forDate = today, draftId = "draft_async")
        repository.injectedJournalSaveFailure = { IllegalStateException("Simulated write failure") }

        val candidate = com.example.dayseed.data.JournalEntry(
            journalEntryId = java.util.UUID.randomUUID().toString(),
            localDate = today.toString(),
            title = "Async title",
            body = "Async draft body",
            primaryMoodCode = "FOCUSED"
        )
        viewModel.saveJournalEntry(candidate, sourceDraftId = "draft_async")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.lastJournalSaveError.first { it != null } } }
        assertTrue("editor stays open on failure", viewModel.isJournalEditorOpen.value)
        assertEquals("draft must survive the failed save", 1, repository.journalDrafts.value.size)
        assertEquals("Async draft body", repository.journalDrafts.value.single().body)

        repository.injectedJournalSaveFailure = null
        viewModel.saveJournalEntry(candidate, sourceDraftId = "draft_async")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.userMessage.first { it == "Daily reflection saved ✍️" } } }
        assertFalse(viewModel.isJournalEditorOpen.value)
        assertEquals(1, repository.journalEntries.value.size)
        assertEquals(0, repository.journalDrafts.value.size)
    }

    // ------------------------------------------------------------------------
    // 5. Cancellation: ViewModel cleared while operation pending
    // ------------------------------------------------------------------------

    @Test
    fun clearedViewModelCancelsPendingMutationAndDatabaseIsUntouched() {
        val gate = GateDispatcher()
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DayseedViewModel(app, customRepository = repository, ioDispatcher = gate) as T
            }
        }
        val provider = ViewModelProvider(store, factory)
        val viewModel = provider.get(DayseedViewModel::class.java)

        viewModel.repository.saveHabitSync(
            quantityHabit("h_cancel"),
            effectiveDateOverride = LocalDate.of(2026, 3, 15)
        )
        viewModel.updateHabitLifecycle("h_cancel", HabitLifecycleState.PAUSED)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()   // let the launch body reach withContext(gate)
        assertEquals("operation is queued, not running", 1, gate.pendingCount)

        // Clearing the store cancels viewModelScope; the pending mutation must be dropped.
        store.clear()
        gate.releaseAll()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals("lifecycle must be untouched after cancellation",
            "ACTIVE", repository.loadHabitsSync().single { it.habitId == "h_cancel" }.lifecycleState.name)
    }

    // ------------------------------------------------------------------------
    // 6. Settings mutations remain functional through the async path
    // ------------------------------------------------------------------------

    @Test
    fun settingsMutationsRunAsynchronouslyAndPersist() {
        val viewModel = newViewModel()
        viewModel.setThemeMode(com.example.dayseed.ui.theme.ThemeModeOption.SYSTEM)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        runBlocking { withTimeout(10_000) { viewModel.userMessage.filterNotNull().first { it.startsWith("Theme set to") } } }
        assertEquals(com.example.dayseed.ui.theme.ThemeModeOption.SYSTEM.settingValue,
            repository.getSettingString("theme_mode", ""))
    }
}
