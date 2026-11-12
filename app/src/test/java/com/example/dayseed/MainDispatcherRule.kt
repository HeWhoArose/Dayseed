package com.example.dayseed

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Sets [Dispatchers.Main] to [UnconfinedTestDispatcher] for the duration of a
 * test, so [androidx.lifecycle.viewModelScope]-launched DayseedViewModel
 * mutations execute deterministically: each launch body runs inline (on the
 * calling thread) until its first REAL suspension (the repository's internal
 * withContext(Dispatchers.IO) hop), and any continuation after that hop —
 * including post-withContext tails such as deleteHabit's route cleanup —
 * resumes on the IO worker thread, never on the paused test main queue.
 * Therefore a single event-driven await ([awaitMatch]) per mutation observes
 * the full effect (repository StateFlows, userMessage, navigation state)
 * without sleeps and without scheduler-ordering races.
 *
 * Usage:
 * ```
 * @get:Rule val mainDispatcherRule = MainDispatcherRule()
 * viewModel.mutate(...)
 * mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
 * <flow>.awaitMatch { post-state predicate }   // event-driven completion
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/**
 * Event-driven wait (no sleeps, no polling) for an observable post-mutation
 * state: collects [this] flow until [predicate] matches. ViewModel mutations
 * run their repository work on the REAL IO pool, so after
 * `mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()` the only
 * reliable completion signal is the mutation's own observable effect
 * (userMessage or a repository StateFlow updated by the internal
 * refreshAllSync hop). The predicate must not match the PRE-call value.
 */
fun <T> Flow<T>.awaitMatch(predicate: (T) -> Boolean): T = runBlocking {
    withTimeout(10_000) { first { predicate(it) } }
}

/**
 * A dispatcher that records the thread it dispatches on and then delegates to
 * a real background dispatcher. Used to prove that ViewModel mutation bodies
 * execute off the (test) main thread.
 */
class RecordingDispatcher(private val delegate: CoroutineDispatcher = Dispatchers.IO) : CoroutineDispatcher() {
    private val recorded = mutableListOf<Thread>()

    val usedThreads: List<Thread> get() = synchronized(recorded) { recorded.toList() }

    override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
        // Record the thread that actually EXECUTES the block, not the caller
        // of dispatch: wrapping lets the delegate move execution to its own
        // thread before the recording happens.
        val wrapped = Runnable {
            synchronized(recorded) { recorded.add(Thread.currentThread()) }
            block.run()
        }
        delegate.dispatch(context, wrapped)
    }

    override fun isDispatchNeeded(context: kotlin.coroutines.CoroutineContext): Boolean =
        delegate.isDispatchNeeded(context)
}

/**
 * A dispatcher that queues dispatch requests without executing them until
 * [releaseAll] is called. Used to hold a ViewModel coroutine mid-flight so a
 * test can cancel the ViewModel (ViewModelStore.clear) while the operation is
 * pending and prove the database is never touched.
 */
class GateDispatcher : CoroutineDispatcher() {
    private val lock = java.util.concurrent.locks.ReentrantLock()
    private val hasPending = lock.newCondition()
    private val queue = mutableListOf<Runnable>()

    override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
        lock.lock()
        try {
            queue.add(block)
            hasPending.signalAll()
        } finally {
            lock.unlock()
        }
    }

    override fun isDispatchNeeded(context: kotlin.coroutines.CoroutineContext): Boolean = true

    val pendingCount: Int
        get() {
            lock.lock()
            try {
                return queue.size
            } finally {
                lock.unlock()
            }
        }

    fun releaseAll() {
        lock.lock()
        val toRun = try {
            queue.toList().also { queue.clear() }
        } finally {
            lock.unlock()
        }
        for (block in toRun) block.run()
    }

    /**
     * Blocks (event-driven, no sleeps) until at least [minCount] blocks are
     * queued on this dispatcher, e.g. a coroutine resumed after a real-IO hop
     * re-dispatched here. Returns false on timeout instead of throwing.
     */
    fun awaitPending(minCount: Int = 1, timeoutMs: Long = 10_000): Boolean {
        val deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000
        lock.lock()
        try {
            while (queue.size < minCount) {
                val remaining = deadlineNanos - System.nanoTime()
                if (remaining <= 0) return false
                hasPending.await(remaining, java.util.concurrent.TimeUnit.NANOSECONDS)
            }
            return true
        } finally {
            lock.unlock()
        }
    }
}
