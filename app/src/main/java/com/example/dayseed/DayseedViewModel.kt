package com.example.dayseed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.JournalExportFormat
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.theme.ThemeModeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

enum class NavTab {
    TODAY,
    HISTORY,
    INSIGHTS,
    SETTINGS
}

class DayseedViewModel(application: Application) : AndroidViewModel(application) {
    val repository = DayseedRepository(application)

    val habits = repository.habits
    val habitVersions = repository.habitVersions
    val habitEntries = repository.habitEntries
    val habitDayOutcomes = repository.habitDayOutcomes
    val habitDateNotes = repository.habitDateNotes
    val journalEntries = repository.journalEntries
    val journalDrafts = repository.journalDrafts
    val appSettings = repository.appSettings
    val comebackInfo = repository.comebackInfo
    val growthMetrics = repository.growthMetrics
    val restoreConflicts = repository.restoreConflicts
    val activeUndoToken = repository.activeUndoToken

    private val _selectedTab = MutableStateFlow(NavTab.TODAY)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _selectedHistoryDate = MutableStateFlow(repository.clock.todayLocal())
    val selectedHistoryDate: StateFlow<LocalDate> = _selectedHistoryDate.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Dialog & Sheet States
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

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun selectHistoryDate(date: LocalDate) {
        _selectedHistoryDate.value = date
    }

    fun dismissComeback() {
        repository.dismissComeback()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun logHabit(
        habit: Habit,
        date: LocalDate = repository.clock.todayLocal(),
        outcome: DailyOutcome = DailyOutcome.COMPLETED,
        effortLevel: EffortLevel = EffortLevel.USUAL,
        note: String = ""
    ) {
        viewModelScope.launch {
            val mode = habit.trackingMode.canonical()
            if (mode == TrackingMode.COMPLETION_ONLY) {
                repository.logHabitDayOutcome(
                    habitId = habit.habitId,
                    date = date,
                    outcomeKind = "COMPLETED",
                    effortLevel = null,
                    note = note.takeIf { it.isNotBlank() }
                )
                _userMessage.value = "✓ ${habit.name}: Completed"
            } else {
                val eff = when (effortLevel) {
                    EffortLevel.TINY -> "TINY"
                    EffortLevel.EXTRA -> "EXTRA"
                    else -> "USUAL"
                }
                repository.logHabitDayOutcome(
                    habitId = habit.habitId,
                    date = date,
                    outcomeKind = "EFFORT_LOGGED",
                    effortLevel = eff,
                    note = note.takeIf { it.isNotBlank() }
                )
                _userMessage.value = "${effortLevel.symbol()} ${habit.name}: ${effortLevel.displayName()} recorded"
            }
        }
    }

    fun logHabitQuantityOrDuration(
        habit: Habit,
        valueUnits: Int,
        date: LocalDate = repository.clock.todayLocal(),
        entryNote: String? = null
    ) {
        viewModelScope.launch {
            repository.logHabitEntry(
                habitId = habit.habitId,
                date = date,
                valueUnits = valueUnits,
                entryNote = entryNote
            )
            val unitLabel = habit.unitCode ?: "units"
            _userMessage.value = if (valueUnits == 0) {
                "Logged 0 $unitLabel for ${habit.name} (honest check-in)"
            } else {
                "Added +$valueUnits $unitLabel for ${habit.name}"
            }
        }
    }

    fun updateHabitEntry(entryId: String, newValueUnits: Int, newEntryNote: String?) {
        viewModelScope.launch {
            repository.updateHabitEntry(entryId, newValueUnits, newEntryNote)
            _userMessage.value = "Entry updated"
        }
    }

    fun deleteHabitEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteHabitEntry(entryId)
            _userMessage.value = "Entry deleted"
        }
    }

    fun saveHabitDateNote(habitId: String, date: LocalDate, body: String) {
        viewModelScope.launch {
            repository.saveHabitDateNote(habitId, date, body)
            _userMessage.value = "Habit note saved"
        }
    }

    fun triggerUndo() {
        val token = activeUndoToken.value ?: return
        viewModelScope.launch {
            val ok = repository.executeUndo(token)
            _userMessage.value = if (ok) "Action undone" else "Undo window expired or record changed"
        }
    }

    fun removeRecord(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            repository.removeRecord(habitId, date)
            _userMessage.value = "Record reset"
        }
    }

    fun openAddHabit() {
        _editingHabit.value = null
        _isAddHabitOpen.value = true
    }

    fun openEditHabit(habit: Habit) {
        _editingHabit.value = habit
        _isAddHabitOpen.value = true
    }

    fun closeAddEditHabit() {
        _editingHabit.value = null
        _isAddHabitOpen.value = false
    }

    fun saveHabit(habit: Habit, applyTodayWhenLogged: Boolean = true) {
        viewModelScope.launch {
            val savedVer = repository.saveHabit(habit, applyTodayWhenLogged = applyTodayWhenLogged)
            closeAddEditHabit()
            _userMessage.value = "Habit saved (effective ${savedVer.effectiveFromLocalDate})"
        }
    }

    fun toggleHabitArchive(habitId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitArchive(habitId, archived)
            _userMessage.value = if (archived) "Habit archived" else "Habit restored"
        }
    }

    fun updateHabitLifecycle(habitId: String, state: HabitLifecycleState) {
        viewModelScope.launch {
            repository.updateHabitLifecycleSync(habitId, state)
            _userMessage.value = "Habit updated to ${state.name.lowercase()}"
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            _userMessage.value = "Habit permanently deleted (linked reflections preserved)"
        }
    }

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
            repository.setSetting("week_start", "ENUM", dayOfWeek.name)
            _userMessage.value = "Week start set to ${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
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
