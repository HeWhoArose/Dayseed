package com.example.dayseed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dayseed.data.DailyOutcome
import com.example.dayseed.data.DailyRecord
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.EffortLevel
import com.example.dayseed.data.Habit
import com.example.dayseed.data.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val records = repository.records
    val journalEntries = repository.journalEntries
    val comebackInfo = repository.comebackInfo
    val growthMetrics = repository.growthMetrics

    private val _selectedTab = MutableStateFlow(NavTab.TODAY)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _selectedHistoryDate = MutableStateFlow(LocalDate.now())
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
        date: LocalDate = LocalDate.now(),
        outcome: DailyOutcome = DailyOutcome.COMPLETED,
        effortLevel: EffortLevel = EffortLevel.USUAL,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.logRecord(habit.id, date, outcome, effortLevel, note)
            val message = when (outcome) {
                DailyOutcome.COMPLETED -> "${effortLevel.symbol()} ${habit.name}: ${effortLevel.displayName()} recorded"
                DailyOutcome.SKIPPED -> "Recorded skip for ${habit.name}. Progress is still yours."
                DailyOutcome.PLANNED_REST -> "Rest day recorded for ${habit.name} 🌿"
            }
            _userMessage.value = message
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

    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            repository.saveHabit(habit)
            closeAddEditHabit()
            _userMessage.value = "Habit saved"
        }
    }

    fun toggleHabitArchive(habitId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitArchive(habitId, archived)
            _userMessage.value = if (archived) "Habit archived" else "Habit restored"
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            _userMessage.value = "Habit and its logs removed"
        }
    }

    fun openJournalEditor(entry: JournalEntry? = null, forDate: LocalDate = LocalDate.now()) {
        _editingJournalEntry.value = entry ?: journalEntries.value.find { it.date == forDate }
        _isJournalEditorOpen.value = true
    }

    fun closeJournalEditor() {
        _editingJournalEntry.value = null
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

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            _userMessage.value = "All local data deleted"
        }
    }
}
