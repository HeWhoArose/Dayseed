package com.example.dayseed

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ReminderEvaluationResult
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Phase 1 remediation coverage: the shared 3-per-day cap and the per-occurrence idempotency
 * must hold when several receivers with separate SQLite connections evaluate concurrently,
 * and incomplete SCHEDULED reservations must be recovered honestly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedReminderConcurrencyTest {

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
        shadowOf(context.applicationContext as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    private fun reminderHabit(habitId: String): Habit = Habit(
        habitId = habitId,
        name = "Habit $habitId",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        reminderMode = "AT_TIME",
        reminderTimeLocal = "09:00"
    )

    /** Repositories that share one SQLite file so separate connections race for real. */
    private fun sharedFileRepo(): DayseedRepository = DayseedRepository(
        context,
        clock = testClock,
        databaseName = DayseedDatabaseHelper.DATABASE_NAME
    )

    // ---- Concurrent evaluations across separate connections ----

    @Test
    fun concurrentSharedDailyCapHoldsAcrossSeparateConnections() {
        val habitIds = listOf("h_c1", "h_c2", "h_c3", "h_c4")
        val main = sharedFileRepo()
        habitIds.forEach { main.saveHabitSync(reminderHabit(it)) }
        val date = main.clock.todayLocal()

        // Four receivers, each with its own repository and therefore its own SQLite
        // connection, fire concurrently on the same database file.
        val connections = habitIds.indices.map { _ -> sharedFileRepo() }
        val pool = Executors.newFixedThreadPool(habitIds.size)
        val barrier = CountDownLatch(habitIds.size)
        try {
            val futures = connections.map { repo ->
                pool.submit(Callable<List<ReminderEvaluationResult>> {
                    barrier.countDown()
                    barrier.await(10, TimeUnit.SECONDS)
                    repo.evaluateTodayRemindersSync(date)
                })
            }
            val results = futures.map { it.get(60, TimeUnit.SECONDS) }.asSequence().flatten().toList()

            // Every connection evaluates the full day's sweep (4 habits each), so 16
            // evaluations in total; each habit must have reached exactly one outcome,
            // because each sweep is one serialised transaction (cap count + writes).
            assertEquals(16, results.size)
            results.groupBy { it.habitId }.forEach { (habitId, evals) ->
                assertEquals("habit $habitId got inconsistent statuses across connections", 1, evals.map { it.deliveryStatus }.distinct().size)
            }

            // The final ledger is the authority: the shared 3/day budget across all habits
            // means at most three slots can ever be reserved (SCHEDULED), with one habit
            // suppressed (DAILY_CAP_REACHED). An unserialised count/insert would let a 4th
            // reservation slip in.
            val ledger = mutableListOf<Pair<String, String>>()
            main.dbHelper.readableDatabase.rawQuery(
                "SELECT delivery_status, COUNT(*) FROM reminder_deliveries " +
                    "WHERE target_period_key = ? GROUP BY delivery_status",
                arrayOf(date.toString())
            ).use { c ->
                while (c.moveToNext()) ledger.add(c.getString(0) to c.getInt(1).toString())
            }
            val scheduledInLedger = ledger.firstOrNull { it.first == "SCHEDULED" }?.second?.toInt() ?: 0
            val suppressedInLedger = ledger.firstOrNull { it.first == "SUPPRESSED" }?.second?.toInt() ?: 0
            assertEquals(4, scheduledInLedger + suppressedInLedger)
            assertEquals(3, scheduledInLedger)

            // And each connection's per-habit evaluation agrees with that final ledger row.
            val ledgerByHabit = mutableMapOf<String, String>()
            main.dbHelper.readableDatabase.rawQuery(
                "SELECT habit_id, delivery_status FROM reminder_deliveries WHERE target_period_key = ?",
                arrayOf(date.toString())
            ).use { c ->
                while (c.moveToNext()) ledgerByHabit[c.getString(0)] = c.getString(1)
            }
            results.groupBy { it.habitId }.forEach { (habitId, evals) ->
                assertEquals(ledgerByHabit[habitId], evals.first().deliveryStatus)
            }
        } finally {
            pool.shutdownNow()
            connections.forEach { it.dbHelper.close() }
            main.dbHelper.close()
        }
    }

    @Test
    fun concurrentSameOccurrenceEvaluationsNeverDuplicateOrFlipDelivery() {
        val main = sharedFileRepo()
        main.saveHabitSync(reminderHabit("h_dup"))
        val date = main.clock.todayLocal()

        val connections = (0 until 3).map { _ -> sharedFileRepo() }
        val pool = Executors.newFixedThreadPool(3)
        val barrier = CountDownLatch(3)
        try {
            val futures = connections.map { repo ->
                pool.submit(Callable<List<ReminderEvaluationResult>> {
                    barrier.countDown()
                    barrier.await(10, TimeUnit.SECONDS)
                    repo.evaluateTodayRemindersSync(date)
                })
            }
            val results = futures.map { it.get(60, TimeUnit.SECONDS) }

            // All replays of one occurrence agree on the reserved state; nothing becomes
            // DELIVERED at evaluation time and no extra rows exist (delivery_key is the PK).
            results.forEach { r ->
                val target = r.firstOrNull { it.habitId == "h_dup" }
                assertNotNull(target)
                assertEquals("SCHEDULED", target!!.deliveryStatus)
            }
            main.dbHelper.readableDatabase.rawQuery(
                "SELECT COUNT(*), MAX(delivery_status) FROM reminder_deliveries WHERE delivery_key = ?",
                arrayOf("HABIT_DAILY:h_dup:$date")
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
                assertEquals("SCHEDULED", c.getString(1))
            }
        } finally {
            pool.shutdownNow()
            connections.forEach { it.dbHelper.close() }
            main.dbHelper.close()
        }
    }

    // ---- Incomplete reservation recovery ----

    private fun makeReservationStale() {
        // Plain SQL literal: SQLite does not accept Kotlin-style underscores in numbers.
        repository.dbHelper.writableDatabase.execSQL(
            "UPDATE reminder_deliveries SET attempted_at_utc_ms = attempted_at_utc_ms - 4000000, " +
                "scheduled_at_utc_ms = scheduled_at_utc_ms - 4000000 " +
                "WHERE delivery_key = 'HABIT_DAILY:h_rec:${repository.clock.todayLocal()}'"
        )
    }

    private fun seededPendingReservation() {
        repository.saveHabitSync(reminderHabit("h_rec"))
        repository.setSettingSync("reminders_enabled", "ENUM", "true")
        val evaluations = repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
        val target = evaluations.firstOrNull { it.habitId == "h_rec" }
        assertNotNull(target)
        assertEquals("SCHEDULED", target!!.deliveryStatus)
    }

    @Test
    fun stalePendingReservationIsRepostedOnlyWhenStillEligible() {
        seededPendingReservation()
        makeReservationStale()

        repository.reconcilePendingReminderPostsSync()

        // Fully eligible at recovery time: the notification is actually posted through
        // NotificationManager and the row is promoted — but only now, after the post.
        val status = repository.getReminderDeliveryStatus(
            "HABIT_DAILY:h_rec:${repository.clock.todayLocal()}"
        )
        assertEquals("DELIVERED", status)
    }

    @Test
    fun stalePendingReservationDuringQuietHoursIsCorrectedNotDelivered() {
        testClock.currentLocalTime = LocalTime.of(22, 0)
        seededPendingReservation()
        makeReservationStale()

        repository.reconcilePendingReminderPostsSync()

        val status = repository.getReminderDeliveryStatus(
            "HABIT_DAILY:h_rec:${repository.clock.todayLocal()}"
        )
        assertEquals("SUPPRESSED", status)
    }

    @Test
    fun freshPendingReservationIsLeftToTheReceiverByReconcile() {
        seededPendingReservation()
        // Reservation is fresh (attempted_at = now): the receiver's own post owns it.
        repository.reconcilePendingReminderPostsSync()
        val status = repository.getReminderDeliveryStatus(
            "HABIT_DAILY:h_rec:${repository.clock.todayLocal()}"
        )
        assertEquals("SCHEDULED", status)
    }

    // ---- Replay discipline ----

    @Test
    fun replayOfDeliveredRowStaysDeliveredAndDoesNotReserveAgain() {
        repository.saveHabitSync(reminderHabit("h_replay"))
        val key = "HABIT_DAILY:h_replay:${repository.clock.todayLocal()}"
        repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
        repository.markReminderDeliveryDelivered(key, testClock.nowUtcMs())

        val replay = repository.evaluateTodayRemindersSync(repository.clock.todayLocal())
            .firstOrNull { it.habitId == "h_replay" }
        assertEquals("DELIVERED", replay?.deliveryStatus)

        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM reminder_deliveries WHERE delivery_key = ? AND delivery_status = 'DELIVERED'",
            arrayOf(key)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
    }
}
