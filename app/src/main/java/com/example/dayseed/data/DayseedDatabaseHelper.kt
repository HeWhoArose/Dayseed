package com.example.dayseed.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class DayseedDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "dayseed.db"
        private const val DATABASE_VERSION = 1

        // Habits Table
        const val TABLE_HABITS = "habits"
        const val COL_HABIT_ID = "id"
        const val COL_HABIT_NAME = "name"
        const val COL_HABIT_DESCRIPTION = "description"
        const val COL_HABIT_MODE = "tracking_mode"
        const val COL_HABIT_TINY = "tiny_desc"
        const val COL_HABIT_USUAL = "usual_desc"
        const val COL_HABIT_EXTRA = "extra_desc"
        const val COL_HABIT_SCHEDULE = "schedule_days"
        const val COL_HABIT_ARCHIVED = "is_archived"
        const val COL_HABIT_COLOR = "color_hex"
        const val COL_HABIT_CREATED = "created_at"

        // Daily Records Table
        const val TABLE_RECORDS = "daily_records"
        const val COL_RECORD_ID = "id"
        const val COL_RECORD_HABIT_ID = "habit_id"
        const val COL_RECORD_DATE = "date"
        const val COL_RECORD_OUTCOME = "outcome"
        const val COL_RECORD_EFFORT = "effort_level"
        const val COL_RECORD_NOTE = "note"
        const val COL_RECORD_UPDATED = "updated_at"

        // Journal Entries Table
        const val TABLE_JOURNAL = "journal_entries"
        const val COL_JOURNAL_ID = "id"
        const val COL_JOURNAL_DATE = "date"
        const val COL_JOURNAL_PROMPT = "prompt"
        const val COL_JOURNAL_CONTENT = "content"
        const val COL_JOURNAL_CREATED = "created_at"
        const val COL_JOURNAL_UPDATED = "updated_at"

        // Preferences Table
        const val TABLE_PREFS = "app_preferences"
        const val COL_PREF_KEY = "pref_key"
        const val COL_PREF_VALUE = "pref_value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_HABITS (
                $COL_HABIT_ID TEXT PRIMARY KEY,
                $COL_HABIT_NAME TEXT NOT NULL,
                $COL_HABIT_DESCRIPTION TEXT,
                $COL_HABIT_MODE TEXT NOT NULL,
                $COL_HABIT_TINY TEXT,
                $COL_HABIT_USUAL TEXT,
                $COL_HABIT_EXTRA TEXT,
                $COL_HABIT_SCHEDULE TEXT NOT NULL,
                $COL_HABIT_ARCHIVED INTEGER DEFAULT 0,
                $COL_HABIT_COLOR TEXT,
                $COL_HABIT_CREATED INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_RECORDS (
                $COL_RECORD_ID TEXT PRIMARY KEY,
                $COL_RECORD_HABIT_ID TEXT NOT NULL,
                $COL_RECORD_DATE TEXT NOT NULL,
                $COL_RECORD_OUTCOME TEXT NOT NULL,
                $COL_RECORD_EFFORT TEXT NOT NULL,
                $COL_RECORD_NOTE TEXT,
                $COL_RECORD_UPDATED INTEGER NOT NULL,
                UNIQUE($COL_RECORD_HABIT_ID, $COL_RECORD_DATE)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_JOURNAL (
                $COL_JOURNAL_ID TEXT PRIMARY KEY,
                $COL_JOURNAL_DATE TEXT NOT NULL,
                $COL_JOURNAL_PROMPT TEXT,
                $COL_JOURNAL_CONTENT TEXT NOT NULL,
                $COL_JOURNAL_CREATED INTEGER NOT NULL,
                $COL_JOURNAL_UPDATED INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_PREFS (
                $COL_PREF_KEY TEXT PRIMARY KEY,
                $COL_PREF_VALUE TEXT NOT NULL
            )
        """.trimIndent())

        // Seed initial friendly starter habits
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle future schema migrations cleanly
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val allDays = DayOfWeek.values().joinToString(",") { it.name }

        // Habit 1: Gentle Movement
        val h1 = ContentValues().apply {
            put(COL_HABIT_ID, UUID.randomUUID().toString())
            put(COL_HABIT_NAME, "Gentle Movement")
            put(COL_HABIT_DESCRIPTION, "Move your body with kindness, no workout pressure")
            put(COL_HABIT_MODE, TrackingMode.EFFORT_LEVELS.name)
            put(COL_HABIT_TINY, "Roll shoulders or 1 deep stretch")
            put(COL_HABIT_USUAL, "10-minute walk or gentle yoga")
            put(COL_HABIT_EXTRA, "25-minute walk in fresh air")
            put(COL_HABIT_SCHEDULE, allDays)
            put(COL_HABIT_ARCHIVED, 0)
            put(COL_HABIT_COLOR, "#286143")
            put(COL_HABIT_CREATED, now)
        }
        db.insert(TABLE_HABITS, null, h1)

        // Habit 2: Quiet Reading
        val h2 = ContentValues().apply {
            put(COL_HABIT_ID, UUID.randomUUID().toString())
            put(COL_HABIT_NAME, "Quiet Reading")
            put(COL_HABIT_DESCRIPTION, "Feed curiosity without finishing a whole book")
            put(COL_HABIT_MODE, TrackingMode.EFFORT_LEVELS.name)
            put(COL_HABIT_TINY, "Read 1 paragraph or sentence")
            put(COL_HABIT_USUAL, "Read 1 chapter (10 mins)")
            put(COL_HABIT_EXTRA, "Read for 30 minutes in peace")
            put(COL_HABIT_SCHEDULE, allDays)
            put(COL_HABIT_ARCHIVED, 0)
            put(COL_HABIT_COLOR, "#3B6955")
            put(COL_HABIT_CREATED, now)
        }
        db.insert(TABLE_HABITS, null, h2)

        // Habit 3: Hydration Seed
        val h3 = ContentValues().apply {
            put(COL_HABIT_ID, UUID.randomUUID().toString())
            put(COL_HABIT_NAME, "Drink Water")
            put(COL_HABIT_DESCRIPTION, "A cup of water to refresh your body")
            put(COL_HABIT_MODE, TrackingMode.COMPLETION_ONLY.name)
            put(COL_HABIT_TINY, "")
            put(COL_HABIT_USUAL, "")
            put(COL_HABIT_EXTRA, "")
            put(COL_HABIT_SCHEDULE, allDays)
            put(COL_HABIT_ARCHIVED, 0)
            put(COL_HABIT_COLOR, "#4A7C59")
            put(COL_HABIT_CREATED, now)
        }
        db.insert(TABLE_HABITS, null, h3)

        // Seed a sample journal entry for today
        val todayStr = LocalDate.now().toString()
        val journal = ContentValues().apply {
            put(COL_JOURNAL_ID, UUID.randomUUID().toString())
            put(COL_JOURNAL_DATE, todayStr)
            put(COL_JOURNAL_PROMPT, "What is one small thing that went well today?")
            put(COL_JOURNAL_CONTENT, "Started Dayseed today. Excited to track my habits with kindness and without streak anxiety.")
            put(COL_JOURNAL_CREATED, now)
            put(COL_JOURNAL_UPDATED, now)
        }
        db.insert(TABLE_JOURNAL, null, journal)
    }
}
