package com.example.dayseed.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class DayseedRepository(context: Context) {
    private val dbHelper = DayseedDatabaseHelper(context.applicationContext)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _records = MutableStateFlow<List<DailyRecord>>(emptyList())
    val records: StateFlow<List<DailyRecord>> = _records.asStateFlow()

    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    private val _comebackInfo = MutableStateFlow<ComebackInfo?>(null)
    val comebackInfo: StateFlow<ComebackInfo?> = _comebackInfo.asStateFlow()

    private val _growthMetrics = MutableStateFlow(GrowthMetrics())
    val growthMetrics: StateFlow<GrowthMetrics> = _growthMetrics.asStateFlow()

    init {
        coroutineScope.launch {
            refreshAll()
            checkComebackFlow()
        }
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        loadHabits()
        loadRecords()
        loadJournalEntries()
        computeGrowthMetrics()
    }

    private fun loadHabits() {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Habit>()
        val cursor = db.query(
            DayseedDatabaseHelper.TABLE_HABITS,
            null, null, null, null, null,
            "${DayseedDatabaseHelper.COL_HABIT_CREATED} ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_ID)
            val nameIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_NAME)
            val descIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_DESCRIPTION)
            val modeIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_MODE)
            val tinyIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_TINY)
            val usualIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_USUAL)
            val extraIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_EXTRA)
            val schedIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_SCHEDULE)
            val archIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_ARCHIVED)
            val colorIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_COLOR)
            val crIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_HABIT_CREATED)

            while (c.moveToNext()) {
                val scheduleRaw = c.getString(schedIdx) ?: ""
                val schedDays = scheduleRaw.split(",")
                    .mapNotNull { name ->
                        try { DayOfWeek.valueOf(name.trim()) } catch (_: Exception) { null }
                    }.toSet().ifEmpty { DayOfWeek.values().toSet() }

                val trackingMode = try {
                    TrackingMode.valueOf(c.getString(modeIdx))
                } catch (_: Exception) {
                    TrackingMode.EFFORT_LEVELS
                }

                list.add(
                    Habit(
                        id = c.getString(idIdx),
                        name = c.getString(nameIdx),
                        description = c.getString(descIdx) ?: "",
                        trackingMode = trackingMode,
                        tinyDescription = c.getString(tinyIdx) ?: "Deliberately small action",
                        usualDescription = c.getString(usualIdx) ?: "The ordinary intended action",
                        extraDescription = c.getString(extraIdx) ?: "Optional stretch action",
                        scheduleDays = schedDays,
                        isArchived = c.getInt(archIdx) == 1,
                        colorHex = c.getString(colorIdx) ?: "#286143",
                        createdAt = c.getLong(crIdx)
                    )
                )
            }
        }
        _habits.value = list
    }

    private fun loadRecords() {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<DailyRecord>()
        val cursor = db.query(
            DayseedDatabaseHelper.TABLE_RECORDS,
            null, null, null, null, null,
            "${DayseedDatabaseHelper.COL_RECORD_DATE} DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_ID)
            val habIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_HABIT_ID)
            val dateIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_DATE)
            val outIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_OUTCOME)
            val effIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_EFFORT)
            val noteIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_NOTE)
            val upIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_RECORD_UPDATED)

            while (c.moveToNext()) {
                val outcome = try {
                    DailyOutcome.valueOf(c.getString(outIdx))
                } catch (_: Exception) {
                    DailyOutcome.COMPLETED
                }
                val effort = try {
                    EffortLevel.valueOf(c.getString(effIdx))
                } catch (_: Exception) {
                    EffortLevel.USUAL
                }
                val date = try {
                    LocalDate.parse(c.getString(dateIdx))
                } catch (_: Exception) {
                    LocalDate.now()
                }

                list.add(
                    DailyRecord(
                        id = c.getString(idIdx),
                        habitId = c.getString(habIdx),
                        date = date,
                        outcome = outcome,
                        effortLevel = effort,
                        note = c.getString(noteIdx) ?: "",
                        updatedAt = c.getLong(upIdx)
                    )
                )
            }
        }
        _records.value = list
    }

    private fun loadJournalEntries() {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<JournalEntry>()
        val cursor = db.query(
            DayseedDatabaseHelper.TABLE_JOURNAL,
            null, null, null, null, null,
            "${DayseedDatabaseHelper.COL_JOURNAL_DATE} DESC, ${DayseedDatabaseHelper.COL_JOURNAL_CREATED} DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_ID)
            val dateIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_DATE)
            val prIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_PROMPT)
            val cntIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_CONTENT)
            val crIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_CREATED)
            val upIdx = c.getColumnIndexOrThrow(DayseedDatabaseHelper.COL_JOURNAL_UPDATED)

            while (c.moveToNext()) {
                val date = try {
                    LocalDate.parse(c.getString(dateIdx))
                } catch (_: Exception) {
                    LocalDate.now()
                }
                list.add(
                    JournalEntry(
                        id = c.getString(idIdx),
                        date = date,
                        prompt = c.getString(prIdx) ?: "",
                        content = c.getString(cntIdx),
                        createdAt = c.getLong(crIdx),
                        updatedAt = c.getLong(upIdx)
                    )
                )
            }
        }
        _journalEntries.value = list
    }

    private fun computeGrowthMetrics() {
        val allRecords = _records.value
        val journals = _journalEntries.value

        val activeDates = mutableSetOf<LocalDate>()
        var completedCount = 0
        var tiny = 0
        var usual = 0
        var extra = 0
        var rest = 0

        for (record in allRecords) {
            when (record.outcome) {
                DailyOutcome.COMPLETED -> {
                    activeDates.add(record.date)
                    completedCount++
                    when (record.effortLevel) {
                        EffortLevel.TINY -> tiny++
                        EffortLevel.USUAL -> usual++
                        EffortLevel.EXTRA -> extra++
                        EffortLevel.STANDARD -> usual++
                    }
                }
                DailyOutcome.PLANNED_REST -> {
                    activeDates.add(record.date)
                    rest++
                }
                DailyOutcome.SKIPPED -> {
                    // Intentionally recorded skip is still showing up
                    activeDates.add(record.date)
                }
            }
        }

        for (journal in journals) {
            activeDates.add(journal.date)
        }

        // Period opportunities: past 7 days opportunities for active habits
        val today = LocalDate.now()
        val activeHabits = _habits.value.filter { !it.isArchived }
        var pastWeekOpportunities = 0
        var pastWeekCompletions = 0

        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            for (h in activeHabits) {
                if (h.isScheduledFor(d)) {
                    pastWeekOpportunities++
                    val rec = allRecords.find { it.habitId == h.id && it.date == d }
                    if (rec != null && rec.outcome == DailyOutcome.COMPLETED) {
                        pastWeekCompletions++
                    }
                }
            }
        }

        _growthMetrics.value = GrowthMetrics(
            totalActiveDates = activeDates.size,
            totalHabitsCompleted = completedCount,
            tinyCount = tiny,
            usualCount = usual,
            extraCount = extra,
            plannedRestCount = rest,
            journalEntriesCount = journals.size,
            currentPeriodCompletions = pastWeekCompletions,
            currentPeriodOpportunities = pastWeekOpportunities
        )
    }

    private fun checkComebackFlow() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DayseedDatabaseHelper.TABLE_PREFS,
            arrayOf(DayseedDatabaseHelper.COL_PREF_VALUE),
            "${DayseedDatabaseHelper.COL_PREF_KEY} = ?",
            arrayOf("last_opened_date"),
            null, null, null
        )
        var lastOpened: LocalDate? = null
        cursor.use { c ->
            if (c.moveToFirst()) {
                val str = c.getString(0)
                lastOpened = try { LocalDate.parse(str) } catch (_: Exception) { null }
            }
        }

        val today = LocalDate.now()
        if (lastOpened != null) {
            val daysAway = ChronoUnit.DAYS.between(lastOpened, today).toInt()
            if (daysAway >= 3) {
                _comebackInfo.value = when {
                    daysAway in 3..13 -> ComebackInfo(
                        daysAway = daysAway,
                        title = "Good to see you again.",
                        message = "Your earlier progress is still here. If it helps, choose one small thing for today — or take a look around first.",
                        primaryActionLabel = "Choose one small step",
                        secondaryActionLabel = "Review habits"
                    )
                    daysAway in 14..89 -> ComebackInfo(
                        daysAway = daysAway,
                        title = "You can pick this up from here.",
                        message = "Nothing you recorded has been erased. You can start with a tiny action, adjust a habit, or simply look at what you’ve already done.",
                        primaryActionLabel = "Start small",
                        secondaryActionLabel = "Review progress"
                    )
                    else -> ComebackInfo(
                        daysAway = daysAway,
                        title = "A new day, whenever you’re ready.",
                        message = "Your history is still yours. There’s no catch-up required. Start fresh with one small step, change your plan, or continue whenever it suits you.",
                        primaryActionLabel = "Choose a small step",
                        secondaryActionLabel = "Adjust habits"
                    )
                }
            }
        }

        // Update last opened date to today
        val writeDb = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DayseedDatabaseHelper.COL_PREF_KEY, "last_opened_date")
            put(DayseedDatabaseHelper.COL_PREF_VALUE, today.toString())
        }
        writeDb.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_PREFS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun dismissComeback() {
        _comebackInfo.value = null
    }

    // --- Habit Operations ---
    suspend fun saveHabit(habit: Habit) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DayseedDatabaseHelper.COL_HABIT_ID, habit.id)
            put(DayseedDatabaseHelper.COL_HABIT_NAME, habit.name)
            put(DayseedDatabaseHelper.COL_HABIT_DESCRIPTION, habit.description)
            put(DayseedDatabaseHelper.COL_HABIT_MODE, habit.trackingMode.name)
            put(DayseedDatabaseHelper.COL_HABIT_TINY, habit.tinyDescription)
            put(DayseedDatabaseHelper.COL_HABIT_USUAL, habit.usualDescription)
            put(DayseedDatabaseHelper.COL_HABIT_EXTRA, habit.extraDescription)
            put(DayseedDatabaseHelper.COL_HABIT_SCHEDULE, habit.scheduleDays.joinToString(",") { it.name })
            put(DayseedDatabaseHelper.COL_HABIT_ARCHIVED, if (habit.isArchived) 1 else 0)
            put(DayseedDatabaseHelper.COL_HABIT_COLOR, habit.colorHex)
            put(DayseedDatabaseHelper.COL_HABIT_CREATED, habit.createdAt)
        }
        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_HABITS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshAll()
    }

    suspend fun toggleHabitArchive(habitId: String, archived: Boolean) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DayseedDatabaseHelper.COL_HABIT_ARCHIVED, if (archived) 1 else 0)
        }
        db.update(DayseedDatabaseHelper.TABLE_HABITS, cv, "${DayseedDatabaseHelper.COL_HABIT_ID} = ?", arrayOf(habitId))
        refreshAll()
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_HABITS, "${DayseedDatabaseHelper.COL_HABIT_ID} = ?", arrayOf(habitId))
        db.delete(DayseedDatabaseHelper.TABLE_RECORDS, "${DayseedDatabaseHelper.COL_RECORD_HABIT_ID} = ?", arrayOf(habitId))
        refreshAll()
    }

    // --- Daily Records Operations ---
    suspend fun logRecord(
        habitId: String,
        date: LocalDate,
        outcome: DailyOutcome,
        effortLevel: EffortLevel,
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val existing = _records.value.find { it.habitId == habitId && it.date == date }
        val id = existing?.id ?: UUID.randomUUID().toString()

        val cv = ContentValues().apply {
            put(DayseedDatabaseHelper.COL_RECORD_ID, id)
            put(DayseedDatabaseHelper.COL_RECORD_HABIT_ID, habitId)
            put(DayseedDatabaseHelper.COL_RECORD_DATE, date.toString())
            put(DayseedDatabaseHelper.COL_RECORD_OUTCOME, outcome.name)
            put(DayseedDatabaseHelper.COL_RECORD_EFFORT, effortLevel.name)
            put(DayseedDatabaseHelper.COL_RECORD_NOTE, note)
            put(DayseedDatabaseHelper.COL_RECORD_UPDATED, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_RECORDS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshAll()
    }

    suspend fun removeRecord(habitId: String, date: LocalDate) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            DayseedDatabaseHelper.TABLE_RECORDS,
            "${DayseedDatabaseHelper.COL_RECORD_HABIT_ID} = ? AND ${DayseedDatabaseHelper.COL_RECORD_DATE} = ?",
            arrayOf(habitId, date.toString())
        )
        refreshAll()
    }

    // --- Journal Operations ---
    suspend fun saveJournalEntry(entry: JournalEntry) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DayseedDatabaseHelper.COL_JOURNAL_ID, entry.id)
            put(DayseedDatabaseHelper.COL_JOURNAL_DATE, entry.date.toString())
            put(DayseedDatabaseHelper.COL_JOURNAL_PROMPT, entry.prompt)
            put(DayseedDatabaseHelper.COL_JOURNAL_CONTENT, entry.content)
            put(DayseedDatabaseHelper.COL_JOURNAL_CREATED, entry.createdAt)
            put(DayseedDatabaseHelper.COL_JOURNAL_UPDATED, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            DayseedDatabaseHelper.TABLE_JOURNAL,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshAll()
    }

    suspend fun deleteJournalEntry(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_JOURNAL, "${DayseedDatabaseHelper.COL_JOURNAL_ID} = ?", arrayOf(id))
        refreshAll()
    }

    // --- Privacy: Export & Deletion ---
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "Dayseed")

        val habitsArray = JSONArray()
        for (h in _habits.value) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("description", h.description)
                put("trackingMode", h.trackingMode.name)
                put("tinyDescription", h.tinyDescription)
                put("usualDescription", h.usualDescription)
                put("extraDescription", h.extraDescription)
                put("scheduleDays", JSONArray(h.scheduleDays.map { it.name }))
                put("isArchived", h.isArchived)
                put("colorHex", h.colorHex)
                put("createdAt", h.createdAt)
            }
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val recordsArray = JSONArray()
        for (r in _records.value) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("habitId", r.habitId)
                put("date", r.date.toString())
                put("outcome", r.outcome.name)
                put("effortLevel", r.effortLevel.name)
                put("note", r.note)
                put("updatedAt", r.updatedAt)
            }
            recordsArray.put(obj)
        }
        root.put("records", recordsArray)

        val journalsArray = JSONArray()
        for (j in _journalEntries.value) {
            val obj = JSONObject().apply {
                put("id", j.id)
                put("date", j.date.toString())
                put("prompt", j.prompt)
                put("content", j.content)
                put("createdAt", j.createdAt)
                put("updatedAt", j.updatedAt)
            }
            journalsArray.put(obj)
        }
        root.put("journals", journalsArray)

        root.toString(2)
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DayseedDatabaseHelper.TABLE_HABITS, null, null)
        db.delete(DayseedDatabaseHelper.TABLE_RECORDS, null, null)
        db.delete(DayseedDatabaseHelper.TABLE_JOURNAL, null, null)
        db.delete(DayseedDatabaseHelper.TABLE_PREFS, null, null)
        refreshAll()
    }
}
