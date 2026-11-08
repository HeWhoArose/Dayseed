package com.example.dayseed

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedDatabaseHelper
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ReminderAlarmReceiver
import com.example.dayseed.data.ReminderEngine
import com.example.dayseed.data.ReminderEvaluationResult
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
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
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase 1 + D-231 remediation coverage: the shared 3-per-day cap, per-occurrence idempotency,
 * and exclusive notification posting ownership must hold when several receivers or recovery
 * sweeps with separate SQLite connections execute concurrently, and incomplete SCHEDULED
 * reservations/claims must be recovered or suppressed honestly.
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
        context.deleteDatabase(DayseedDatabaseHelper.DATABASE_NAME)
        val date = LocalDate.of(2026, 3, 15)
        val time = LocalTime.of(10, 0)
        testClock = TestDayseedClock(
            currentLocalDate = date,
            currentLocalTime = time,
            currentUtcMs = date.atTime(time).toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
        shadowOf(context.applicationContext as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
        context.deleteDatabase(DayseedDatabaseHelper.DATABASE_NAME)
    }

    private fun reminderHabit(habitId: String, timeLocal: String = "09:00"): Habit = Habit(
        habitId = habitId,
        name = "Habit $habitId",
        trackingMode = TrackingMode.COMPLETION_ONLY,
        targetPeriod = TargetPeriod.DAY,
        reminderMode = "AT_TIME",
        reminderTimeLocal = timeLocal
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

    // ---- D-231: Concurrent posting ownership across separate SQLite connections ----

    /**
     * D-231 Test A: Two concurrent `ReminderAlarmReceiver.processAlarmSync` executions for the
     * same habit, date, and reminder time across two separate `DayseedRepository` instances
     * backed by the same file-backed SQLite database (`DayseedDatabaseHelper.DATABASE_NAME`).
     */
    @Test
    fun concurrentDuplicateAlarmExecutionPostsOnlyOnce() {
        val main = sharedFileRepo()
        main.saveHabitSync(reminderHabit("h_dup_alarm", timeLocal = "10:00"))
        main.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val date = main.clock.todayLocal()
        val deliveryKey = "HABIT_DAILY:h_dup_alarm:$date"

        val repo1 = sharedFileRepo()
        val repo2 = sharedFileRepo()
        val pool = Executors.newFixedThreadPool(2)
        val startBarrier = CountDownLatch(2)
        val postCallCount = AtomicInteger(0)

        try {
            val controlledPoster: (Context, String, Int, String) -> Boolean = { ctx, tag, id, name ->
                postCallCount.incrementAndGet()
                ReminderNotifier.postHabitReminder(ctx, tag, id, name)
            }

            val f1 = pool.submit(Callable<String> {
                startBarrier.countDown()
                startBarrier.await(10, TimeUnit.SECONDS)
                ReminderAlarmReceiver.processAlarmSync(
                    context = context,
                    repository = repo1,
                    habitIdRaw = "h_dup_alarm",
                    deliveryDateRaw = date.toString(),
                    reminderTimeRaw = "10:00",
                    poster = controlledPoster
                )
            })
            val f2 = pool.submit(Callable<String> {
                startBarrier.countDown()
                startBarrier.await(10, TimeUnit.SECONDS)
                ReminderAlarmReceiver.processAlarmSync(
                    context = context,
                    repository = repo2,
                    habitIdRaw = "h_dup_alarm",
                    deliveryDateRaw = date.toString(),
                    reminderTimeRaw = "10:00",
                    poster = controlledPoster
                )
            })

            val decisions = listOf(f1.get(30, TimeUnit.SECONDS), f2.get(30, TimeUnit.SECONDS))
            assertEquals(1, decisions.count { it == ReminderAlarmReceiver.DECISION_POSTED })
            assertEquals(1, decisions.count { it == ReminderAlarmReceiver.DECISION_ALREADY_DELIVERED })
            assertEquals("Posting callback must be invoked exactly once", 1, postCallCount.get())

            // Verify final ledger state: exactly 1 row, status DELIVERED, null suppression_reason,
            // and shared daily cap consumed once.
            main.dbHelper.readableDatabase.rawQuery(
                "SELECT COUNT(*), MAX(delivery_status), MAX(suppression_reason) FROM reminder_deliveries WHERE delivery_key = ?",
                arrayOf(deliveryKey)
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
                assertEquals("DELIVERED", c.getString(1))
                assertTrue(c.isNull(2))
            }
            main.dbHelper.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM reminder_deliveries WHERE target_period_key = ? AND delivery_status IN ('DELIVERED', 'SCHEDULED')",
                arrayOf(date.toString())
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Daily cap must be consumed exactly once", 1, c.getInt(0))
            }
        } finally {
            pool.shutdownNow()
            repo1.dbHelper.close()
            repo2.dbHelper.close()
            main.dbHelper.close()
        }
    }

    /**
     * D-231 Test B: Two separate SQLite connections where the first connection holds the active
     * posting claim and pauses inside the posting callback BEFORE `DELIVERED` is committed, while
     * the second connection attempts `processAlarmSync` for the same occurrence. Verifies the
     * second connection is rejected by the active DB claim and never invokes the posting callback.
     */
    @Test
    fun concurrentSeparateConnectionsCannotPassPostingGateWhileFirstClaimIsInFlight() {
        val main = sharedFileRepo()
        main.saveHabitSync(reminderHabit("h_inflight", timeLocal = "10:00"))
        main.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val date = main.clock.todayLocal()
        val deliveryKey = "HABIT_DAILY:h_inflight:$date"

        val repo1 = sharedFileRepo()
        val repo2 = sharedFileRepo()
        val pool = Executors.newFixedThreadPool(2)
        val claimAcquiredAndEnteredPost = CountDownLatch(1)
        val allowPostToFinish = CountDownLatch(1)
        val postCallCount = AtomicInteger(0)

        try {
            val winnerFuture = pool.submit(Callable<String> {
                ReminderAlarmReceiver.processAlarmSync(
                    context = context,
                    repository = repo1,
                    habitIdRaw = "h_inflight",
                    deliveryDateRaw = date.toString(),
                    reminderTimeRaw = "10:00",
                    poster = { ctx, tag, id, name ->
                        postCallCount.incrementAndGet()
                        claimAcquiredAndEnteredPost.countDown()
                        allowPostToFinish.await(10, TimeUnit.SECONDS)
                        ReminderNotifier.postHabitReminder(ctx, tag, id, name)
                    }
                )
            })

            assertTrue(
                "First connection should acquire claim and enter poster",
                claimAcquiredAndEnteredPost.await(10, TimeUnit.SECONDS)
            )
            // While repo1 is paused inside poster, verify the DB row is SCHEDULED with an active
            // POSTING_CLAIM:<uuid> token (not yet DELIVERED).
            assertEquals("SCHEDULED", main.getReminderDeliveryStatus(deliveryKey))
            assertNotNull(main.getReminderPostingClaimToken(deliveryKey))

            // Second connection now attempts processAlarmSync while the first claim is active.
            val secondDecision = pool.submit(Callable<String> {
                ReminderAlarmReceiver.processAlarmSync(
                    context = context,
                    repository = repo2,
                    habitIdRaw = "h_inflight",
                    deliveryDateRaw = date.toString(),
                    reminderTimeRaw = "10:00",
                    poster = { _, _, _, _ ->
                        postCallCount.incrementAndGet()
                        true
                    }
                )
            }).get(10, TimeUnit.SECONDS)

            assertEquals(ReminderAlarmReceiver.DECISION_ALREADY_DELIVERED, secondDecision)
            assertEquals("Non-owner must never invoke poster", 1, postCallCount.get())

            // Allow the owner to finish posting and commit DELIVERED.
            allowPostToFinish.countDown()
            assertEquals(ReminderAlarmReceiver.DECISION_POSTED, winnerFuture.get(10, TimeUnit.SECONDS))
            assertEquals("DELIVERED", main.getReminderDeliveryStatus(deliveryKey))
            assertNull(main.getReminderPostingClaimToken(deliveryKey))
            assertEquals(1, postCallCount.get())
        } finally {
            allowPostToFinish.countDown()
            pool.shutdownNow()
            repo1.dbHelper.close()
            repo2.dbHelper.close()
            main.dbHelper.close()
        }
    }

    /**
     * D-231 Test C: Receiver vs. recovery sweep (`reconcilePendingReminderPostsSync`) coordination.
     * 1. An active receiver claim is NOT stolen or re-posted by `reconcilePendingReminderPostsSync()`.
     * 2. Once a claim is genuinely stale/abandoned beyond `maxPendingAgeMs` and eligibility gates
     *    remain open, `reconcilePendingReminderPostsSync()` atomically claims and recovers it once.
     */
    @Test
    fun receiverAndRecoverySweepCoordinateThroughPostingOwnership() {
        val main = sharedFileRepo()
        main.saveHabitSync(reminderHabit("h_coord_live", timeLocal = "10:00"))
        main.saveHabitSync(reminderHabit("h_coord_stale", timeLocal = "10:00"))
        main.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val date = main.clock.todayLocal()
        val liveKey = "HABIT_DAILY:h_coord_live:$date"
        val staleKey = "HABIT_DAILY:h_coord_stale:$date"

        val receiverRepo = sharedFileRepo()
        val sweepRepo1 = sharedFileRepo()
        val sweepRepo2 = sharedFileRepo()
        val pool = Executors.newFixedThreadPool(2)
        val receiverInFlight = CountDownLatch(1)
        val releaseReceiver = CountDownLatch(1)
        val receiverPosts = AtomicInteger(0)
        val sweepPostsWhileLive = AtomicInteger(0)

        try {
            // Part 1: Receiver holds an active in-flight claim for h_coord_live while sweep runs.
            val receiverFuture = pool.submit(Callable<String> {
                ReminderAlarmReceiver.processAlarmSync(
                    context = context,
                    repository = receiverRepo,
                    habitIdRaw = "h_coord_live",
                    deliveryDateRaw = date.toString(),
                    reminderTimeRaw = "10:00",
                    poster = { ctx, tag, id, name ->
                        receiverPosts.incrementAndGet()
                        receiverInFlight.countDown()
                        releaseReceiver.await(10, TimeUnit.SECONDS)
                        ReminderNotifier.postHabitReminder(ctx, tag, id, name)
                    }
                )
            })

            assertTrue(receiverInFlight.await(10, TimeUnit.SECONDS))
            val liveToken = main.getReminderPostingClaimToken(liveKey)
            assertNotNull(liveToken)

            // Sweep runs on another connection while the receiver's claim is active.
            sweepRepo1.reconcilePendingReminderPostsSync(
                poster = { _, _, _, _ ->
                    sweepPostsWhileLive.incrementAndGet()
                    true
                }
            )
            assertEquals("Sweep must not post an actively claimed reminder", 0, sweepPostsWhileLive.get())
            assertEquals("Sweep must not steal the active claim token", liveToken, main.getReminderPostingClaimToken(liveKey))

            releaseReceiver.countDown()
            assertEquals(ReminderAlarmReceiver.DECISION_POSTED, receiverFuture.get(10, TimeUnit.SECONDS))
            assertEquals(1, receiverPosts.get())
            assertEquals("DELIVERED", main.getReminderDeliveryStatus(liveKey))

            // Part 2: h_coord_stale is claimed, then abandoned (process crash before post finishes).
            val staleClaim = receiverRepo.evaluateAndClaimHabitReminderForPostingSync("h_coord_stale", date)
            assertEquals(DayseedRepository.ReminderPostingClaimStatus.CLAIMED, staleClaim.status)
            assertEquals("SCHEDULED", main.getReminderDeliveryStatus(staleKey))

            // Advance clock by 35 minutes (> 30m maxPendingAgeMs), still on the same day at 10:35.
            testClock.currentLocalTime = LocalTime.of(10, 35)
            testClock.currentUtcMs += 35L * 60L * 1000L

            // Two recovery sweeps race on the expired claim across separate SQLite connections.
            val sweepBarrier = CountDownLatch(2)
            val staleRecoveryPosts = AtomicInteger(0)
            val s1 = pool.submit(Callable<Unit> {
                sweepBarrier.countDown()
                sweepBarrier.await(10, TimeUnit.SECONDS)
                sweepRepo1.reconcilePendingReminderPostsSync(
                    poster = { ctx, tag, id, name ->
                        staleRecoveryPosts.incrementAndGet()
                        ReminderNotifier.postHabitReminder(ctx, tag, id, name)
                    }
                )
            })
            val s2 = pool.submit(Callable<Unit> {
                sweepBarrier.countDown()
                sweepBarrier.await(10, TimeUnit.SECONDS)
                sweepRepo2.reconcilePendingReminderPostsSync(
                    poster = { ctx, tag, id, name ->
                        staleRecoveryPosts.incrementAndGet()
                        ReminderNotifier.postHabitReminder(ctx, tag, id, name)
                    }
                )
            })
            s1.get(10, TimeUnit.SECONDS)
            s2.get(10, TimeUnit.SECONDS)

            assertEquals("Stale claim must be recovered and posted exactly once", 1, staleRecoveryPosts.get())
            assertEquals("DELIVERED", main.getReminderDeliveryStatus(staleKey))
            assertNull(main.getReminderPostingClaimToken(staleKey))
        } finally {
            releaseReceiver.countDown()
            pool.shutdownNow()
            receiverRepo.dbHelper.close()
            sweepRepo1.dbHelper.close()
            sweepRepo2.dbHelper.close()
            main.dbHelper.close()
        }
    }

    /**
     * D-231 Test D: Failed posting releases the claim, records `SUPPRESSED`/`POST_FAILED` (never
     * `DELIVERED`), does not consume the shared 3/day cap, and remains deterministic and
     * idempotent on repeated processing.
     */
    @Test
    fun failedPostingMarksPostFailedReleasesClaimDoesNotConsumeCapAndIsIdempotent() {
        repository.saveHabitSync(reminderHabit("h_fail_post", timeLocal = "10:00"))
        repository.setHabitSyncListForCapCheck()
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val date = repository.clock.todayLocal()
        val failKey = "HABIT_DAILY:h_fail_post:$date"
        val postAttempts = AtomicInteger(0)

        val decision1 = ReminderAlarmReceiver.processAlarmSync(
            context = context,
            repository = repository,
            habitIdRaw = "h_fail_post",
            deliveryDateRaw = date.toString(),
            reminderTimeRaw = "10:00",
            poster = { _, _, _, _ ->
                postAttempts.incrementAndGet()
                false
            }
        )
        assertEquals(ReminderAlarmReceiver.DECISION_POST_FAILED, decision1)
        assertEquals(1, postAttempts.get())
        assertEquals("SUPPRESSED", repository.getReminderDeliveryStatus(failKey))
        assertNull("Failed post must not leave an active claim", repository.getReminderPostingClaimToken(failKey))

        repository.dbHelper.readableDatabase.rawQuery(
            "SELECT delivery_status, suppression_reason, delivered_at_utc_ms FROM reminder_deliveries WHERE delivery_key = ?",
            arrayOf(failKey)
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("SUPPRESSED", c.getString(0))
            assertEquals("POST_FAILED", c.getString(1))
            assertTrue("delivered_at_utc_ms must remain NULL on failed post", c.isNull(2))
        }

        // Repeated processing of the same occurrence is idempotent and does not invoke poster again.
        val decision2 = ReminderAlarmReceiver.processAlarmSync(
            context = context,
            repository = repository,
            habitIdRaw = "h_fail_post",
            deliveryDateRaw = date.toString(),
            reminderTimeRaw = "10:00",
            poster = { _, _, _, _ ->
                postAttempts.incrementAndGet()
                true
            }
        )
        assertEquals(ReminderAlarmReceiver.DECISION_SUPPRESSED, decision2)
        assertEquals("Idempotent replay of SUPPRESSED/POST_FAILED must not re-invoke poster", 1, postAttempts.get())

        // Verify the failed post did NOT consume a slot in the shared 3/day cap: three other
        // habits on the same day can all still reserve and deliver.
        listOf("h_cap_1", "h_cap_2", "h_cap_3").forEach { id ->
            val d = ReminderAlarmReceiver.processAlarmSync(
                context = context,
                repository = repository,
                habitIdRaw = id,
                deliveryDateRaw = date.toString(),
                reminderTimeRaw = "10:00"
            )
            assertEquals("Habit $id should still deliver within 3/day cap", ReminderAlarmReceiver.DECISION_POSTED, d)
            assertEquals("DELIVERED", repository.getReminderDeliveryStatus("HABIT_DAILY:$id:$date"))
        }
    }

    private fun DayseedRepository.setHabitSyncListForCapCheck() {
        saveHabitSync(reminderHabit("h_cap_1", timeLocal = "10:00"))
        saveHabitSync(reminderHabit("h_cap_2", timeLocal = "10:00"))
        saveHabitSync(reminderHabit("h_cap_3", timeLocal = "10:00"))
    }

    /**
     * D-231 Test E: Simulates process death immediately after `evaluateAndClaimHabitReminderForPostingSync`
     * acquires a posting claim (`POSTING_CLAIM:<uuid>`) and before `DELIVERED`/`SUPPRESSED` is
     * committed. Verifies:
     * 1. While the lease is fresh (`< maxPendingAgeMs`), neither a duplicate receiver nor the
     *    recovery sweep double-posts.
     * 2. After the lease expires (`>= maxPendingAgeMs`), the recovery sweep claims and posts once.
     * 3. A superseded claim token from the crashed/hung execution cannot later overwrite the
     *    recovered ledger state.
     */
    @Test
    fun processCrashAfterClaimAcquisitionRecoversAfterLeaseExpiryAndRejectsSupersededToken() {
        repository.saveHabitSync(reminderHabit("h_crash", timeLocal = "10:00"))
        repository.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        val date = repository.clock.todayLocal()
        val key = "HABIT_DAILY:h_crash:$date"

        // Step 1: First execution acquires the claim, then "crashes" before posting/finalising.
        val firstClaim = repository.evaluateAndClaimHabitReminderForPostingSync("h_crash", date)
        assertEquals(DayseedRepository.ReminderPostingClaimStatus.CLAIMED, firstClaim.status)
        val crashedToken = firstClaim.claimToken
        assertNotNull(crashedToken)
        assertEquals("SCHEDULED", repository.getReminderDeliveryStatus(key))

        // Step 2: While lease is still fresh (+5 minutes < 30 minutes), duplicate receiver and
        // recovery sweep both leave the active claim alone and never call poster.
        testClock.currentLocalTime = LocalTime.of(10, 5)
        testClock.currentUtcMs += 5L * 60L * 1000L
        val freshPosts = AtomicInteger(0)
        val dupDecision = ReminderAlarmReceiver.processAlarmSync(
            context = context,
            repository = repository,
            habitIdRaw = "h_crash",
            deliveryDateRaw = date.toString(),
            reminderTimeRaw = "10:00",
            poster = { _, _, _, _ ->
                freshPosts.incrementAndGet()
                true
            }
        )
        assertEquals(ReminderAlarmReceiver.DECISION_ALREADY_DELIVERED, dupDecision)
        repository.reconcilePendingReminderPostsSync(
            poster = { _, _, _, _ ->
                freshPosts.incrementAndGet()
                true
            }
        )
        assertEquals(0, freshPosts.get())
        assertEquals("SCHEDULED", repository.getReminderDeliveryStatus(key))

        // Step 3: Advance past the 30-minute lease (+30 more minutes -> 10:35 local).
        // Recovery sweep now claims the expired reservation and posts once.
        testClock.currentLocalTime = LocalTime.of(10, 35)
        testClock.currentUtcMs += 30L * 60L * 1000L
        val recoveredPosts = AtomicInteger(0)
        repository.reconcilePendingReminderPostsSync(
            poster = { ctx, tag, id, name ->
                recoveredPosts.incrementAndGet()
                ReminderNotifier.postHabitReminder(ctx, tag, id, name)
            }
        )
        assertEquals(1, recoveredPosts.get())
        assertEquals("DELIVERED", repository.getReminderDeliveryStatus(key))
        assertNull(repository.getReminderPostingClaimToken(key))

        // Step 4: Superseded token from the first execution cannot overwrite DELIVERED to SUPPRESSED.
        val staleOverwrite = repository.markReminderDeliveryNotDelivered(
            deliveryKey = key,
            suppressionReason = "POST_FAILED",
            nowUtcMs = testClock.nowUtcMs(),
            claimToken = crashedToken
        )
        assertFalse("Superseded claim token must be rejected", staleOverwrite)
        assertEquals("DELIVERED", repository.getReminderDeliveryStatus(key))
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
