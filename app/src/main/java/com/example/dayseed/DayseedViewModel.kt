package com.example.dayseed

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.GroveTreeState
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitConfigEffectivePolicy
import com.example.dayseed.data.HabitConfigurationLockedTodayException
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitStreakRun
import com.example.dayseed.data.HabitVersion
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.LongestActiveStreakSelection
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class NavTab {
    TODAY,
    HABITS,
    JOURNAL,
    INSIGHTS,
    SETTINGS
}

enum class HabitsFilterState(val label: String, val lifecycleState: HabitLifecycleState?) {
    ALL("All", null),
    ACTIVE("Active", HabitLifecycleState.ACTIVE),
    PAUSED("Paused", HabitLifecycleState.PAUSED),
    COMPLETED("Completed", HabitLifecycleState.COMPLETED),
    ARCHIVED("Archived", HabitLifecycleState.ARCHIVED)
}

sealed class SecondaryRoute(
    open val originTab: NavTab,
    open val parentRoute: SecondaryRoute?
) {
    data class HabitDetail(
        val habitId: String,
        val selectedDate: LocalDate,
        override val originTab: NavTab,
        override val parentRoute: SecondaryRoute? = null
    ) : SecondaryRoute(originTab, parentRoute)

    data class CreateEditHabit(
        val habitId: String?,
        override val originTab: NavTab,
        override val parentRoute: SecondaryRoute? = null
    ) : SecondaryRoute(originTab, parentRoute)

    data class RecordSession(
        val habitId: String,
        val date: LocalDate,
        val entryId: String? = null,
        override val originTab: NavTab,
        override val parentRoute: SecondaryRoute? = null
    ) : SecondaryRoute(originTab, parentRoute)

    data class LivingGrove(
        val selectedHabitId: String? = null,
        val highlightedRunId: String? = null,
        override val originTab: NavTab,
        override val parentRoute: SecondaryRoute? = null
    ) : SecondaryRoute(originTab, parentRoute)

    data class CollectedMomentsDetail(
        val yearMonth: YearMonth,
        val selectedDate: LocalDate? = null,
        override val originTab: NavTab,
        override val parentRoute: SecondaryRoute? = null
    ) : SecondaryRoute(originTab, parentRoute)
}

class DayseedViewModel @JvmOverloads constructor(
    application: Application,
    customRepository: DayseedRepository? = null
) : AndroidViewModel(application) {
    val repository = customRepository ?: DayseedRepository(application)

    val habits = repository.habits
    val habitVersions = repository.habitVersions
    val habitEntries = repository.habitEntries
    val habitDayOutcomes = repository.habitDayOutcomes
    val habitDateNotes = repository.habitDateNotes
    val habitDateExceptions = repository.habitDateExceptions
    val habitStreakRuns = repository.habitStreakRuns
    val streakRuns = repository.habitStreakRuns
    val weekStartHistory = repository.weekStartHistory
    val habitSummaries = repository.habitSummaries
    val journalEntries = repository.journalEntries
    val journalDrafts = repository.journalDrafts
    val appSettings = repository.appSettings
    val comebackInfo = repository.comebackInfo
    val growthMetrics = repository.growthMetrics
    val restoreConflicts = repository.restoreConflicts
    val activeUndoToken = repository.activeUndoToken

    // Primary 5-Destination Navigation State (§4.1)
    private val _selectedTab = MutableStateFlow(NavTab.TODAY)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _tabHistoryStack = MutableStateFlow<List<NavTab>>(emptyList())
    val tabHistoryStack: StateFlow<List<NavTab>> = _tabHistoryStack.asStateFlow()

    // Secondary Navigation Stack (§4.2 & §4.3)
    private val _secondaryRouteStack = MutableStateFlow<List<SecondaryRoute>>(emptyList())
    val secondaryRouteStack: StateFlow<List<SecondaryRoute>> = _secondaryRouteStack.asStateFlow()

    private val _currentSecondaryRoute = MutableStateFlow<SecondaryRoute?>(null)
    val currentSecondaryRoute: StateFlow<SecondaryRoute?> = _currentSecondaryRoute.asStateFlow()

    // Preserved Screen Context State (§4.3)
    private val _selectedHistoryDate = MutableStateFlow(repository.clock.todayLocal())
    val selectedHistoryDate: StateFlow<LocalDate> = _selectedHistoryDate.asStateFlow()
    val selectedDate: StateFlow<LocalDate> = _selectedHistoryDate.asStateFlow()

    private val _selectedHabitId = MutableStateFlow<String?>(null)
    val selectedHabitId: StateFlow<String?> = _selectedHabitId.asStateFlow()

    private val _selectedEntryId = MutableStateFlow<String?>(null)
    val selectedEntryId: StateFlow<String?> = _selectedEntryId.asStateFlow()

    private val _habitsFilterState = MutableStateFlow(HabitsFilterState.ALL)
    val habitsFilterState: StateFlow<HabitsFilterState> = _habitsFilterState.asStateFlow()
    val habitsFilter: StateFlow<HabitsFilterState> = _habitsFilterState.asStateFlow()

    private val _habitsSearchQuery = MutableStateFlow("")
    val habitsSearchQuery: StateFlow<String> = _habitsSearchQuery.asStateFlow()

    private val _journalSearchQuery = MutableStateFlow("")
    val journalSearchQuery: StateFlow<String> = _journalSearchQuery.asStateFlow()

    private val _journalSelectedMoodFilter = MutableStateFlow<String?>(null)
    val journalSelectedMoodFilter: StateFlow<String?> = _journalSelectedMoodFilter.asStateFlow()
    val journalMoodFilter: StateFlow<String?> = _journalSelectedMoodFilter.asStateFlow()

    private val _todayNotScheduledExpanded = MutableStateFlow(false)
    val todayNotScheduledExpanded: StateFlow<Boolean> = _todayNotScheduledExpanded.asStateFlow()

    private val _scrollPositions = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val scrollPositions: StateFlow<Map<String, Pair<Int, Int>>> = _scrollPositions.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _lastSaveError = MutableStateFlow<String?>(null)
    val lastSaveError: StateFlow<String?> = _lastSaveError.asStateFlow()
    val lastErrorMessage: StateFlow<String?>
        get() = lastSaveError

    // Compatibility & Dialog States
    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    private val _isAddHabitOpen = MutableStateFlow(false)
    val isAddHabitOpen: StateFlow<Boolean> = _isAddHabitOpen.asStateFlow()

    private val _isJournalEditorOpen = MutableStateFlow(false)
    val isJournalEditorOpen: StateFlow<Boolean> = _isJournalEditorOpen.asStateFlow()

    private val _editingJournalEntry = MutableStateFlow<JournalEntry?>(null)
    val editingJournalEntry: StateFlow<JournalEntry?> = _editingJournalEntry.asStateFlow()

    private val _journalEditorDate = MutableStateFlow(repository.clock.todayLocal())
    val journalEditorDate: StateFlow<LocalDate> = _journalEditorDate.asStateFlow()

    private val _journalInitialLinkedHabitId = MutableStateFlow<String?>(null)
    val journalInitialLinkedHabitId: StateFlow<String?> = _journalInitialLinkedHabitId.asStateFlow()

    private val _journalInitialPromptId = MutableStateFlow<String?>(null)
    val journalInitialPromptId: StateFlow<String?> = _journalInitialPromptId.asStateFlow()

    // Quick-action idempotency guard (§5.1)
    private var lastQuickActionSignature: String? = null
    private var lastQuickActionAtUtcMs: Long = Long.MIN_VALUE
    private var lastQuickActionIdempotencyKey: String? = null

    fun saveScrollPosition(destinationKey: String, index: Int, offset: Int) {
        _scrollPositions.value = _scrollPositions.value.toMutableMap().apply {
            put(destinationKey, index to offset)
        }
    }

    fun getScrollPosition(destinationKey: String): Pair<Int, Int> {
        return _scrollPositions.value[destinationKey] ?: (0 to 0)
    }

    // =========================================================================
    // NAVIGATION & BACK STACK (§4.1–§4.3)
    // =========================================================================

    fun selectTab(tab: NavTab) {
        val currentTab = _selectedTab.value
        if (_secondaryRouteStack.value.isNotEmpty()) {
            _secondaryRouteStack.value = emptyList()
            _currentSecondaryRoute.value = null
            _isAddHabitOpen.value = false
            _editingHabit.value = null
        }
        if (currentTab != tab) {
            _tabHistoryStack.value = (_tabHistoryStack.value.filter { it != tab } + currentTab).takeLast(5)
            _selectedTab.value = tab
        }
    }

    private fun pushSecondaryRoute(route: SecondaryRoute) {
        val updated = _secondaryRouteStack.value + route
        _secondaryRouteStack.value = updated
        _currentSecondaryRoute.value = route
    }

    fun openHabitDetail(habitId: String, date: LocalDate = _selectedHistoryDate.value) {
        _selectedHabitId.value = habitId
        _selectedHistoryDate.value = date
        val parent = _secondaryRouteStack.value.lastOrNull()
        pushSecondaryRoute(
            SecondaryRoute.HabitDetail(
                habitId = habitId,
                selectedDate = date,
                originTab = _selectedTab.value,
                parentRoute = parent
            )
        )
    }

    fun openHabitDetail(habitId: String, localDateStr: String) {
        val parsed = runCatching { LocalDate.parse(localDateStr) }.getOrElse { _selectedHistoryDate.value }
        openHabitDetail(habitId, parsed)
    }

    fun selectDetailDate(habitId: String, localDateStr: String) {
        val parsed = runCatching { LocalDate.parse(localDateStr) }.getOrElse { _selectedHistoryDate.value }
        selectHistoryDate(parsed)
    }

    fun openCreateEditHabit(habitId: String? = null) {
        _lastSaveError.value = null
        val habit = habitId?.let { id -> _habitsOrNull(id) }
        _editingHabit.value = habit
        _selectedHabitId.value = habitId ?: _selectedHabitId.value
        _isAddHabitOpen.value = true
        val parent = _secondaryRouteStack.value.lastOrNull()
        pushSecondaryRoute(
            SecondaryRoute.CreateEditHabit(
                habitId = habitId,
                originTab = _selectedTab.value,
                parentRoute = parent
            )
        )
    }

    fun openRecordSession(
        habitId: String,
        date: LocalDate = _selectedHistoryDate.value,
        entryId: String? = null
    ) {
        _selectedHabitId.value = habitId
        _selectedHistoryDate.value = date
        _selectedEntryId.value = entryId
        val parent = _secondaryRouteStack.value.lastOrNull()
        pushSecondaryRoute(
            SecondaryRoute.RecordSession(
                habitId = habitId,
                date = date,
                entryId = entryId,
                originTab = _selectedTab.value,
                parentRoute = parent
            )
        )
    }

    fun openRecordSession(
        habitId: String,
        localDateStr: String,
        entryId: String? = null
    ) {
        val parsed = runCatching { LocalDate.parse(localDateStr) }.getOrElse { _selectedHistoryDate.value }
        openRecordSession(habitId, parsed, entryId)
    }

    fun openLivingGrove(
        selectedHabitId: String? = null,
        highlightedRunId: String? = null
    ) {
        if (selectedHabitId != null) {
            _selectedHabitId.value = selectedHabitId
        }
        val parent = _secondaryRouteStack.value.lastOrNull()
        pushSecondaryRoute(
            SecondaryRoute.LivingGrove(
                selectedHabitId = selectedHabitId,
                highlightedRunId = highlightedRunId,
                originTab = _selectedTab.value,
                parentRoute = parent
            )
        )
    }

    fun openCollectedMomentsDetail(
        yearMonth: YearMonth = YearMonth.from(repository.clock.todayLocal()),
        selectedDate: LocalDate? = null
    ) {
        if (selectedDate != null) {
            _selectedHistoryDate.value = selectedDate
        }
        val parent = _secondaryRouteStack.value.lastOrNull()
        pushSecondaryRoute(
            SecondaryRoute.CollectedMomentsDetail(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                originTab = _selectedTab.value,
                parentRoute = parent
            )
        )
    }

    /**
     * Handles both system Back and top-bar Back consistently (§4.3):
     * 1. Closes open Journal Editor dialog if active.
     * 2. Pops the top SecondaryRoute and restores the exact parent route or origin tab + context state.
     * 3. Otherwise pops tab history or returns to Today.
     */
    fun navigateBack(): Boolean {
        if (_isJournalEditorOpen.value) {
            closeJournalEditor()
            return true
        }
        val stack = _secondaryRouteStack.value
        if (stack.isNotEmpty()) {
            val popped = stack.last()
            val remaining = stack.dropLast(1)
            _secondaryRouteStack.value = remaining
            val newTop = remaining.lastOrNull()
            _currentSecondaryRoute.value = newTop

            if (popped is SecondaryRoute.CreateEditHabit) {
                _editingHabit.value = null
                _isAddHabitOpen.value = false
            }
            if (popped is SecondaryRoute.RecordSession) {
                _selectedEntryId.value = null
            }

            when (newTop) {
                is SecondaryRoute.HabitDetail -> {
                    _selectedHabitId.value = newTop.habitId
                    _selectedHistoryDate.value = newTop.selectedDate
                    _selectedTab.value = newTop.originTab
                }
                is SecondaryRoute.CreateEditHabit -> {
                    _editingHabit.value = newTop.habitId?.let { _habitsOrNull(it) }
                    _isAddHabitOpen.value = true
                    _selectedTab.value = newTop.originTab
                }
                is SecondaryRoute.RecordSession -> {
                    _selectedHabitId.value = newTop.habitId
                    _selectedHistoryDate.value = newTop.date
                    _selectedEntryId.value = newTop.entryId
                    _selectedTab.value = newTop.originTab
                }
                is SecondaryRoute.LivingGrove -> {
                    if (newTop.selectedHabitId != null) {
                        _selectedHabitId.value = newTop.selectedHabitId
                    }
                    _selectedTab.value = newTop.originTab
                }
                is SecondaryRoute.CollectedMomentsDetail -> {
                    if (newTop.selectedDate != null) {
                        _selectedHistoryDate.value = newTop.selectedDate
                    }
                    _selectedTab.value = newTop.originTab
                }
                null -> {
                    _selectedTab.value = popped.originTab
                }
            }
            return true
        }

        if (_isAddHabitOpen.value) {
            closeAddEditHabit()
            return true
        }

        val tabHistory = _tabHistoryStack.value
        if (tabHistory.isNotEmpty()) {
            val prevTab = tabHistory.last()
            _tabHistoryStack.value = tabHistory.dropLast(1)
            _selectedTab.value = prevTab
            return true
        }

        if (_selectedTab.value != NavTab.TODAY) {
            _selectedTab.value = NavTab.TODAY
            return true
        }

        return false
    }

    private fun _habitsOrNull(habitId: String): Habit? {
        return habits.value.find { it.habitId == habitId }
    }

    fun selectHistoryDate(date: LocalDate) {
        _selectedHistoryDate.value = date
        val top = _currentSecondaryRoute.value
        if (top is SecondaryRoute.HabitDetail) {
            val updatedTop = top.copy(selectedDate = date)
            _secondaryRouteStack.value = _secondaryRouteStack.value.dropLast(1) + updatedTop
            _currentSecondaryRoute.value = updatedTop
        }
    }

    fun selectDate(date: LocalDate) = selectHistoryDate(date)

    fun setHabitsFilter(filter: HabitsFilterState) {
        _habitsFilterState.value = filter
    }

    fun setHabitsSearchQuery(query: String) {
        _habitsSearchQuery.value = query
    }

    fun setJournalSearchQuery(query: String) {
        _journalSearchQuery.value = query
    }

    fun setJournalMoodFilter(moodCode: String?) {
        _journalSelectedMoodFilter.value = moodCode
    }

    fun setTodayNotScheduledExpanded(expanded: Boolean) {
        _todayNotScheduledExpanded.value = expanded
    }

    fun selectLongestActiveRun(): LongestActiveStreakSelection? {
        return repository.selectLongestActiveRun()
    }

    fun dismissComeback() {
        repository.dismissComeback()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearSaveError() {
        _lastSaveError.value = null
    }

    // =========================================================================
    // HABIT LOGGING, SESSION MANAGEMENT, REORDERING & UNDO (§5–§9)
    // =========================================================================

    fun logHabit(
        habit: Habit,
        date: LocalDate = repository.clock.todayLocal(),
        outcome: DailyOutcome = DailyOutcome.COMPLETED,
        effortLevel: EffortLevel = EffortLevel.USUAL,
        note: String = "",
        idempotencyKey: String? = null
    ): Boolean {
        val mode = habit.trackingMode.canonical()
        val nowMs = repository.clock.nowUtcMs()
        val sig = "OUTCOME:${habit.habitId}:$date:${outcome.name}:${effortLevel.name}:$note"
        val resolvedKey = if (idempotencyKey != null) {
            idempotencyKey
        } else if (sig == lastQuickActionSignature &&
            lastQuickActionIdempotencyKey != null &&
            (nowMs - lastQuickActionAtUtcMs) in 0..350L
        ) {
            // Rapid duplicate tap within 350ms reuses exact idempotency key from the first tap
            lastQuickActionIdempotencyKey!!
        } else {
            val newKey = "quick_outcome_${habit.habitId}_${date}_${nowMs}_${UUID.randomUUID()}"
            lastQuickActionSignature = sig
            lastQuickActionAtUtcMs = nowMs
            lastQuickActionIdempotencyKey = newKey
            newKey
        }

        return try {
            if (mode == TrackingMode.COMPLETION_ONLY) {
                repository.logHabitDayOutcomeSync(
                    habitId = habit.habitId,
                    date = date,
                    outcomeKind = "COMPLETED",
                    effortLevel = null,
                    note = note.takeIf { it.isNotBlank() },
                    idempotencyKey = resolvedKey
                )
                _userMessage.value = "✓ ${habit.name}: Completed"
            } else {
                val eff = when (effortLevel) {
                    EffortLevel.TINY -> "TINY"
                    EffortLevel.EXTRA -> "EXTRA"
                    else -> "USUAL"
                }
                repository.logHabitDayOutcomeSync(
                    habitId = habit.habitId,
                    date = date,
                    outcomeKind = "EFFORT_LOGGED",
                    effortLevel = eff,
                    note = note.takeIf { it.isNotBlank() },
                    idempotencyKey = resolvedKey
                )
                _userMessage.value = "${effortLevel.symbol()} ${habit.name}: ${effortLevel.displayName()} recorded"
            }
            true
        } catch (e: Exception) {
            _userMessage.value = e.message ?: "Unable to record activity"
            false
        }
    }

    fun logHabitQuantityOrDuration(
        habit: Habit,
        valueUnits: Int,
        date: LocalDate = repository.clock.todayLocal(),
        entryNote: String? = null,
        idempotencyKey: String? = null
    ): Boolean {
        val nowMs = repository.clock.nowUtcMs()
        val sig = "ENTRY:${habit.habitId}:$date:$valueUnits:${entryNote.orEmpty()}"
        val resolvedKey = if (idempotencyKey != null) {
            idempotencyKey
        } else if (sig == lastQuickActionSignature &&
            lastQuickActionIdempotencyKey != null &&
            (nowMs - lastQuickActionAtUtcMs) in 0..350L
        ) {
            lastQuickActionIdempotencyKey!!
        } else {
            val newKey = "quick_entry_${habit.habitId}_${date}_${valueUnits}_${nowMs}_${UUID.randomUUID()}"
            lastQuickActionSignature = sig
            lastQuickActionAtUtcMs = nowMs
            lastQuickActionIdempotencyKey = newKey
            newKey
        }

        return try {
            repository.logHabitEntrySync(
                habitId = habit.habitId,
                date = date,
                valueUnits = valueUnits,
                entryNote = entryNote,
                idempotencyKey = resolvedKey
            )
            val unitLabel = habit.unitCode ?: "units"
            _userMessage.value = if (valueUnits == 0) {
                "Logged 0 $unitLabel for ${habit.name} (honest check-in)"
            } else {
                "Added +$valueUnits $unitLabel for ${habit.name}"
            }
            true
        } catch (e: Exception) {
            _userMessage.value = e.message ?: "Unable to log session"
            false
        }
    }

    fun updateHabitEntry(entryId: String, newValueUnits: Int, newEntryNote: String?): Boolean {
        return try {
            val undoToken = repository.updateHabitEntrySync(entryId, newValueUnits, newEntryNote)
            if (undoToken == null) {
                _userMessage.value = "Session no longer exists and could not be updated"
                false
            } else {
                _userMessage.value = "Session updated"
                true
            }
        } catch (e: Exception) {
            _userMessage.value = e.message ?: "Unable to update session"
            false
        }
    }

    fun deleteHabitEntry(entryId: String): Boolean {
        return try {
            val token = repository.deleteHabitEntryWithUndoSync(entryId)
            if (token != null) {
                _userMessage.value = "Session deleted"
                true
            } else {
                _userMessage.value = "Unable to delete session"
                false
            }
        } catch (e: Exception) {
            _userMessage.value = e.message ?: "Unable to delete session"
            false
        }
    }

    fun deleteHabitEntryWithUndo(entryId: String) {
        deleteHabitEntry(entryId)
    }

    fun saveHabitDateNote(habitId: String, date: LocalDate, body: String) {
        repository.saveHabitDateNoteSync(habitId, date, body)
        _userMessage.value = "Habit note saved"
    }

    fun triggerUndo() {
        val token = activeUndoToken.value ?: return
        val ok = repository.executeUndoSync(token)
        _userMessage.value = if (ok) "Action undone" else "Undo window expired or record changed"
    }

    fun executeUndo() = triggerUndo()

    fun refreshAllSync() {
        repository.refreshAllSync()
    }

    fun removeRecord(habitId: String, date: LocalDate) {
        repository.removeRecordSync(habitId, date)
        _userMessage.value = "Record reset"
    }

    fun openAddHabit() {
        openCreateEditHabit(null)
    }

    fun openEditHabit(habit: Habit) {
        openCreateEditHabit(habit.habitId)
    }

    fun closeAddEditHabit() {
        _lastSaveError.value = null
        _editingHabit.value = null
        _isAddHabitOpen.value = false
        if (_currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit) {
            navigateBack()
        }
    }

    fun takeRestDayToday(habit: Habit, note: String? = null) {
        try {
            repository.setRestDayExceptionSync(habit.habitId, repository.clock.todayLocal(), note)
            _userMessage.value = "Rest day taken for ${habit.name} (streak held)"
        } catch (e: Exception) {
            _userMessage.value = e.message ?: "Unable to take a rest day today"
        }
    }

    fun clearRestDayToday(habit: Habit) {
        repository.clearRestDayExceptionSync(habit.habitId, repository.clock.todayLocal())
        _userMessage.value = "Rest day cleared for ${habit.name}"
    }

    fun saveHabit(
        habit: Habit,
        applyTodayWhenLogged: Boolean = true,
        policy: HabitConfigEffectivePolicy? = null
    ): HabitVersion? {
        _lastSaveError.value = null
        return try {
            val savedVer = repository.saveHabitSync(
                habit = habit,
                applyTodayWhenLogged = applyTodayWhenLogged,
                policy = policy
            )
            closeAddEditHabit()
            _userMessage.value = "Habit saved (effective ${savedVer.effectiveFromLocalDate})"
            savedVer
        } catch (e: Exception) {
            val msg = e.message ?: "Unable to save habit"
            _lastSaveError.value = msg
            _userMessage.value = msg
            null
        }
    }

    fun reorderHabit(habitId: String, moveUp: Boolean) {
        repository.reorderHabitSync(habitId, moveUp)
    }

    fun toggleHabitArchive(habitId: String, archived: Boolean) {
        val state = if (archived) HabitLifecycleState.ARCHIVED else HabitLifecycleState.ACTIVE
        repository.updateHabitLifecycleSync(habitId, state)
        _userMessage.value = if (archived) "Habit archived" else "Habit restored"
    }

    fun updateHabitLifecycle(habitId: String, state: HabitLifecycleState) {
        repository.updateHabitLifecycleSync(habitId, state)
        val stateWord = when (state) {
            HabitLifecycleState.ACTIVE -> "active"
            HabitLifecycleState.PAUSED -> "paused"
            HabitLifecycleState.COMPLETED -> "completed"
            HabitLifecycleState.ARCHIVED -> "archived"
        }
        _userMessage.value = "Habit marked as $stateWord"
    }

    fun deleteHabit(habitId: String) {
        repository.deleteHabitPermanentlySync(habitId)
        val filteredStack = _secondaryRouteStack.value.filterNot { route ->
            (route is SecondaryRoute.CreateEditHabit && route.habitId == habitId) ||
                (route is SecondaryRoute.HabitDetail && route.habitId == habitId)
        }
        _secondaryRouteStack.value = filteredStack
        _currentSecondaryRoute.value = filteredStack.lastOrNull()
        if (_editingHabit.value?.habitId == habitId) {
            _editingHabit.value = null
        }
        if (_currentSecondaryRoute.value !is SecondaryRoute.CreateEditHabit) {
            _isAddHabitOpen.value = false
        }
        _lastSaveError.value = null
        _userMessage.value = "Habit permanently deleted (8s undo available)"
    }

    // =========================================================================
    // LIVING GROVE ONE-SHOT DORMANT TRANSITION & REDUCED MOTION (§10.2, §11.2)
    // =========================================================================

    fun getAcknowledgedTerminalRunIds(): Set<String> {
        val raw = repository.getSettingString("acknowledged_terminal_run_ids", "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun unacknowledgedDormantRuns(habitId: String? = null): List<HabitStreakRun> {
        val ack = getAcknowledgedTerminalRunIds()
        return habitStreakRuns.value.filter { run ->
            !run.isActive &&
                run.qualifiedForGrove &&
                run.treeState == GroveTreeState.DORMANT_TREE &&
                (habitId == null || run.habitId == habitId) &&
                !ack.contains(run.runId)
        }
    }

    fun getUnacknowledgedDormantRunForHabit(habitId: String): HabitStreakRun? {
        return unacknowledgedDormantRuns(habitId).firstOrNull()
    }

    fun acknowledgeDormantRun(runId: String) {
        val updated = getAcknowledgedTerminalRunIds() + runId
        repository.setSettingSync("acknowledged_terminal_run_ids", "STRING", updated.joinToString(","))
    }

    fun acknowledgeDormantTransition(runId: String) = acknowledgeDormantRun(runId)

    fun isReducedMotionEnabled(): Boolean {
        val appSetting = repository.getSettingString("reduced_motion_enabled", "false")
        if (appSetting.equals("true", ignoreCase = true) || appSetting == "1") {
            return true
        }
        return try {
            val scale = Settings.Global.getFloat(
                getApplication<Application>().contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // JOURNAL & SETTINGS OPERATIONS
    // =========================================================================

    fun openJournalEditor(
        entry: JournalEntry? = null,
        forDate: LocalDate = repository.clock.todayLocal(),
        linkedHabitId: String? = null,
        promptId: String? = null
    ) {
        _editingJournalEntry.value = entry
        _journalEditorDate.value = entry?.date ?: forDate
        _journalInitialLinkedHabitId.value = entry?.linkedHabitId ?: linkedHabitId
        _journalInitialPromptId.value = entry?.promptId ?: promptId
        _isJournalEditorOpen.value = true
    }

    fun closeJournalEditor() {
        _editingJournalEntry.value = null
        _journalInitialLinkedHabitId.value = null
        _journalInitialPromptId.value = null
        _isJournalEditorOpen.value = false
    }

    fun saveJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.saveJournalEntry(entry)
            closeJournalEditor()
            _userMessage.value = "Daily reflection saved ✍️"
        }
    }

    fun deleteJournalEntry(id: String) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
            closeJournalEditor()
            _userMessage.value = "Reflection deleted"
        }
    }

    fun setThemeMode(mode: ThemeModeOption) {
        viewModelScope.launch {
            repository.setSetting("theme_mode", "ENUM", mode.settingValue)
            _userMessage.value = "Theme set to ${mode.label}"
        }
    }

    fun setWeekStart(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            val entry = repository.setWeekStart(dayOfWeek)
            _userMessage.value = "Week start set to ${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} (effective ${entry.effectiveFromLocalDate})"
        }
    }

    fun setQuietHours(startLocal: String, endLocal: String) {
        viewModelScope.launch {
            repository.setSetting("quiet_hours_start", "TIME_LOCAL", startLocal)
            repository.setSetting("quiet_hours_end", "TIME_LOCAL", endLocal)
            _userMessage.value = "Quiet hours updated ($startLocal – $endLocal)"
        }
    }

    fun resolveConflict(conflictId: String, resolution: ConflictResolution) {
        viewModelScope.launch {
            repository.resolveRestoreConflictSync(conflictId, resolution)
            _userMessage.value = "Conflict resolved (${resolution.name})"
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            _userMessage.value = "All local data deleted"
        }
    }
}
