package com.example.dayseed.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DayseedMigrationBlockedException(message: String) : IllegalStateException(message)

class DayseedDatabaseHelper(
    context: Context,
    databaseName: String? = DATABASE_NAME,
    private val migrationEvaluationDateProvider: () -> LocalDate = { LocalDate.now() }
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dayseed.db"
        const val DATABASE_VERSION = 3

        const val TABLE_HABITS = "habits"
        const val TABLE_HABIT_VERSIONS = "habit_versions"
        const val TABLE_HABIT_ENTRIES = "habit_entries"
        const val TABLE_HABIT_DAY_OUTCOMES = "habit_day_outcomes"
        const val TABLE_HABIT_DATE_NOTES = "habit_date_notes"
        const val TABLE_HABIT_DATE_EXCEPTIONS = "habit_date_exceptions"
        const val TABLE_HABIT_STREAK_RUNS = "habit_streak_runs"
        const val TABLE_WEEK_START_HISTORY = "week_start_history"
        const val TABLE_JOURNAL_ENTRIES = "journal_entries"
        const val TABLE_JOURNAL_DRAFTS = "journal_drafts"
        const val TABLE_APP_SETTINGS = "app_settings"
        const val TABLE_REMINDER_DELIVERIES = "reminder_deliveries"
        const val TABLE_RESTORE_SESSIONS = "restore_sessions"
        const val TABLE_RESTORE_CONFLICTS = "restore_conflicts"
        const val TABLE_NOTIFICATION_CANCEL_OUTBOX = "notification_cancel_outbox"

        val ALL_V3_TABLES = setOf(
            TABLE_HABITS,
            TABLE_HABIT_VERSIONS,
            TABLE_HABIT_ENTRIES,
            TABLE_HABIT_DAY_OUTCOMES,
            TABLE_HABIT_DATE_NOTES,
            TABLE_HABIT_DATE_EXCEPTIONS,
            TABLE_HABIT_STREAK_RUNS,
            TABLE_WEEK_START_HISTORY,
            TABLE_JOURNAL_ENTRIES,
            TABLE_JOURNAL_DRAFTS,
            TABLE_APP_SETTINGS,
            TABLE_REMINDER_DELIVERIES,
            TABLE_RESTORE_SESSIONS,
            TABLE_RESTORE_CONFLICTS,
            TABLE_NOTIFICATION_CANCEL_OUTBOX
        )

        val ALL_V3_INDEXES = setOf(
            "idx_habit_versions_lookup",
            "idx_habit_entries_habit_date",
            "idx_habit_entries_date",
            "idx_habit_outcomes_date",
            "idx_habit_notes_date",
            "idx_habit_exceptions_habit_date",
            "idx_streak_runs_habit_active",
            "idx_streak_runs_grove",
            "idx_journal_entries_date",
            "idx_journal_entries_linked_habit",
            "idx_journal_drafts_date",
            "idx_reminder_deliveries_habit_period",
            "idx_cancel_outbox_status"
        )
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
        if (!db.isReadOnly && db.version == DATABASE_VERSION) {
            reconcileExistingV3SchemaIfNeeded(db)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")
        createV3Schema(db)
        seedDefaultSettingsIfEmpty(db, System.currentTimeMillis())
        seedDefaultWeekStartBaselineIfEmpty(db, System.currentTimeMillis())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("PRAGMA foreign_keys = ON;")
        if (oldVersion < 1 || newVersion > DATABASE_VERSION) {
            throw DayseedMigrationBlockedException("Unsupported schema upgrade path: $oldVersion -> $newVersion")
        }
        if (oldVersion < 2) {
            migrateV1ToV2FailClosed(db)
        }
        if (oldVersion < 3) {
            migrateV2ToV3FailClosed(db, migrationEvaluationDateProvider())
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw DayseedMigrationBlockedException("Database downgrade is prohibited ($oldVersion -> $newVersion).")
    }

    // =========================================================================
    // V3 SCHEMA CREATION (15 TABLES + 13 NAMED INDEXES + TRIGGERS)
    // =========================================================================

    fun createV3Schema(db: SQLiteDatabase) {
        // 1. habits
        db.execSQL(
            """
            CREATE TABLE $TABLE_HABITS (
                habit_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(habit_id)) > 0),
                created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
                updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms),
                category TEXT,
                lifetime_goal_days INTEGER CHECK (lifetime_goal_days IS NULL OR (lifetime_goal_days >= 1 AND lifetime_goal_days <= 36500))
            );
            """.trimIndent()
        )

        // 2. habit_versions
        db.execSQL(buildHabitVersionsTableSql(TABLE_HABIT_VERSIONS))

        // 3. habit_entries
        db.execSQL(buildHabitEntriesTableSql(TABLE_HABIT_ENTRIES))

        // 4. habit_day_outcomes
        db.execSQL(buildHabitDayOutcomesTableSql(TABLE_HABIT_DAY_OUTCOMES))

        // 5. habit_date_notes
        db.execSQL(buildHabitDateNotesTableSql(TABLE_HABIT_DATE_NOTES))

        // 6. habit_date_exceptions
        db.execSQL(buildHabitDateExceptionsTableSql(TABLE_HABIT_DATE_EXCEPTIONS))

        // 7. habit_streak_runs
        db.execSQL(buildHabitStreakRunsTableSql(TABLE_HABIT_STREAK_RUNS))

        // 8. week_start_history
        db.execSQL(buildWeekStartHistoryTableSql(TABLE_WEEK_START_HISTORY))

        // 9. journal_entries
        db.execSQL(buildJournalEntriesTableSql(TABLE_JOURNAL_ENTRIES))

        // 10. journal_drafts
        db.execSQL(buildJournalDraftsTableSql(TABLE_JOURNAL_DRAFTS))

        // 11. app_settings
        db.execSQL(
            """
            CREATE TABLE $TABLE_APP_SETTINGS (
                setting_key TEXT NOT NULL PRIMARY KEY CHECK (length(trim(setting_key)) > 0),
                value_type TEXT NOT NULL CHECK (value_type IN ('STRING', 'INT', 'BOOL', 'JSON', 'ENUM', 'TIME_LOCAL')),
                value_text TEXT NOT NULL,
                schema_version INTEGER NOT NULL DEFAULT 1 CHECK (schema_version >= 1),
                updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= 0)
            );
            """.trimIndent()
        )

        // 12. reminder_deliveries
        db.execSQL(buildReminderDeliveriesTableSql(TABLE_REMINDER_DELIVERIES))

        // 13. restore_sessions
        db.execSQL(
            """
            CREATE TABLE $TABLE_RESTORE_SESSIONS (
                restore_session_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(restore_session_id)) > 0),
                restore_mode TEXT NOT NULL CHECK (restore_mode IN ('REPLACE', 'MERGE')),
                format_version INTEGER NOT NULL CHECK (format_version >= 1),
                source_schema_version INTEGER NOT NULL CHECK (source_schema_version >= 1),
                session_state TEXT NOT NULL CHECK (
                    session_state IN (
                        'STARTED',
                        'VALIDATED',
                        'AWAITING_CONFLICT_RESOLUTION',
                        'APPLYING',
                        'COMPLETED',
                        'FAILED',
                        'ROLLED_BACK'
                    )
                ),
                started_at_utc_ms INTEGER NOT NULL CHECK (started_at_utc_ms >= 0),
                updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= started_at_utc_ms),
                manifest_json TEXT NOT NULL,
                safety_backup_ref TEXT,
                safety_backup_verified INTEGER NOT NULL DEFAULT 0 CHECK (safety_backup_verified IN (0, 1))
            );
            """.trimIndent()
        )

        // 14. restore_conflicts
        db.execSQL(buildRestoreConflictsTableSql(TABLE_RESTORE_CONFLICTS))

        // 15. notification_cancel_outbox
        db.execSQL(buildNotificationCancelOutboxTableSql(TABLE_NOTIFICATION_CANCEL_OUTBOX))

        createV3Indexes(db)
        createV3Triggers(db)
    }

    private fun buildRestoreConflictsTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            conflict_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(conflict_id)) > 0),
            restore_session_id TEXT NOT NULL REFERENCES $TABLE_RESTORE_SESSIONS(restore_session_id) ON DELETE CASCADE,
            record_type TEXT NOT NULL CHECK (
                record_type IN (
                    'habit',
                    'habit_version',
                    'habit_entry',
                    'habit_day_outcome',
                    'habit_date_note',
                    'habit_date_exception',
                    'habit_streak_run',
                    'journal_entry',
                    'journal_draft',
                    'app_setting',
                    'week_start_history'
                )
            ),
            record_key TEXT NOT NULL CHECK (length(trim(record_key)) > 0),
            current_payload_json TEXT NOT NULL,
            backup_payload_json TEXT NOT NULL,
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0)
        );
        """.trimIndent()

    private fun buildHabitVersionsTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            version_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(version_id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            effective_from_local_date TEXT NOT NULL CHECK (
                length(effective_from_local_date) = 10
                AND effective_from_local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            name TEXT NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 80),
            description TEXT CHECK (description IS NULL OR length(description) <= 1000),
            tracking_mode TEXT NOT NULL CHECK (
                tracking_mode IN ('QUANTITY', 'DURATION', 'COMPLETION_ONLY', 'EFFORT_AWARE')
            ),
            unit_code TEXT,
            target_period TEXT NOT NULL CHECK (target_period IN ('DAY', 'WEEK')),
            target_value INTEGER CHECK (
                (tracking_mode IN ('QUANTITY', 'DURATION') AND target_value IS NOT NULL AND target_value >= 1 AND target_value <= 2147483647)
                OR (tracking_mode IN ('COMPLETION_ONLY', 'EFFORT_AWARE') AND target_period = 'DAY' AND target_value IS NULL)
                OR (tracking_mode IN ('COMPLETION_ONLY', 'EFFORT_AWARE') AND target_period = 'WEEK' AND (target_value IS NULL OR (target_value >= 1 AND target_value <= 2147483647)))
            ),
            schedule_kind TEXT NOT NULL CHECK (
                schedule_kind IN ('DAILY', 'SELECTED_WEEKDAYS', 'FLEXIBLE_WEEKLY_TARGET')
            ),
            scheduled_weekdays_mask INTEGER,
            preferred_weekdays_mask INTEGER,
            lifecycle_state TEXT NOT NULL CHECK (
                lifecycle_state IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')
            ),
            reminder_mode TEXT NOT NULL CHECK (reminder_mode IN ('OFF', 'AT_TIME')),
            reminder_time_local TEXT CHECK (
                (reminder_mode = 'OFF' AND reminder_time_local IS NULL)
                OR (
                    reminder_mode = 'AT_TIME'
                    AND reminder_time_local IS NOT NULL
                    AND length(reminder_time_local) = 5
                    AND reminder_time_local GLOB '[0-2][0-9]:[0-5][0-9]'
                )
            ),
            tiny_description TEXT NOT NULL DEFAULT 'Deliberately small action' CHECK (length(tiny_description) <= 300),
            usual_description TEXT NOT NULL DEFAULT 'The ordinary intended action' CHECK (length(usual_description) <= 300),
            extra_description TEXT NOT NULL DEFAULT 'Optional stretch action' CHECK (length(extra_description) <= 300),
            color_hex TEXT NOT NULL DEFAULT '#286143' CHECK (length(color_hex) BETWEEN 4 AND 9),
            sort_order INTEGER NOT NULL DEFAULT 0,
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            CHECK (
                (schedule_kind = 'DAILY'
                    AND scheduled_weekdays_mask IS NULL
                    AND preferred_weekdays_mask IS NULL
                    AND target_period = 'DAY')
                OR (schedule_kind = 'SELECTED_WEEKDAYS'
                    AND scheduled_weekdays_mask BETWEEN 1 AND 127
                    AND preferred_weekdays_mask IS NULL
                    AND target_period = 'DAY')
                OR (schedule_kind = 'FLEXIBLE_WEEKLY_TARGET'
                    AND scheduled_weekdays_mask IS NULL
                    AND (preferred_weekdays_mask IS NULL OR preferred_weekdays_mask BETWEEN 1 AND 127)
                    AND target_period = 'WEEK')
            ),
            UNIQUE (habit_id, effective_from_local_date)
        );
        """.trimIndent()

    private fun buildHabitEntriesTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            entry_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(entry_id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            value_units INTEGER NOT NULL CHECK (value_units >= 0 AND value_units <= 2147483647),
            entry_note TEXT CHECK (entry_note IS NULL OR length(entry_note) <= 500),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms),
            idempotency_key TEXT NOT NULL UNIQUE CHECK (length(trim(idempotency_key)) > 0)
        );
        """.trimIndent()

    private fun buildHabitDayOutcomesTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            outcome_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(outcome_id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            outcome_kind TEXT NOT NULL CHECK (outcome_kind IN ('COMPLETED', 'EFFORT_LOGGED')),
            effort_level TEXT CHECK (
                (outcome_kind = 'COMPLETED' AND effort_level IS NULL)
                OR (outcome_kind = 'EFFORT_LOGGED' AND effort_level IN ('TINY', 'USUAL', 'EXTRA'))
            ),
            idempotency_key TEXT NOT NULL UNIQUE CHECK (length(trim(idempotency_key)) > 0),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms),
            UNIQUE (habit_id, local_date)
        );
        """.trimIndent()

    private fun buildHabitDateNotesTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            note_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(note_id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            body TEXT NOT NULL CHECK (length(trim(body)) BETWEEN 1 AND 2000),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms),
            UNIQUE (habit_id, local_date)
        );
        """.trimIndent()

    private fun buildHabitDateExceptionsTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            exception_kind TEXT NOT NULL CHECK (exception_kind IN ('REST_DAY')),
            note TEXT CHECK (note IS NULL OR length(note) <= 500),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms),
            UNIQUE (habit_id, local_date)
        );
        """.trimIndent()

    private fun buildHabitStreakRunsTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            run_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(run_id)) > 0),
            habit_id TEXT NOT NULL REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            start_local_date TEXT NOT NULL CHECK (
                length(start_local_date) = 10
                AND start_local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            end_local_date TEXT NOT NULL CHECK (
                length(end_local_date) = 10
                AND end_local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
                AND end_local_date >= start_local_date
            ),
            completed_streak_count INTEGER NOT NULL CHECK (completed_streak_count >= 1 AND completed_streak_count <= 2147483647),
            elapsed_active_span_count INTEGER NOT NULL CHECK (elapsed_active_span_count >= completed_streak_count AND elapsed_active_span_count <= 2147483647),
            streak_unit TEXT NOT NULL CHECK (streak_unit IN ('DAYS', 'WEEKS')),
            terminal_reason TEXT CHECK (
                (is_active = 1 AND terminal_reason IS NULL)
                OR (is_active = 0 AND terminal_reason IN ('MISSED', 'PAUSED', 'ARCHIVED', 'COMPLETED'))
            ),
            is_active INTEGER NOT NULL CHECK (is_active IN (0, 1)),
            qualified_for_grove INTEGER NOT NULL CHECK (qualified_for_grove IN (0, 1)),
            qualified_at_utc_ms INTEGER CHECK (
                (qualified_for_grove = 0 AND qualified_at_utc_ms IS NULL)
                OR (qualified_for_grove = 1 AND qualified_at_utc_ms IS NOT NULL AND qualified_at_utc_ms >= 0)
            ),
            broken_on_local_date TEXT CHECK (
                (terminal_reason = 'MISSED' AND broken_on_local_date IS NOT NULL
                    AND length(broken_on_local_date) = 10
                    AND broken_on_local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
                OR (terminal_reason IS NULL AND broken_on_local_date IS NULL)
                OR (terminal_reason IN ('PAUSED', 'ARCHIVED', 'COMPLETED') AND broken_on_local_date IS NULL)
            ),
            tree_state TEXT NOT NULL CHECK (
                (is_active = 1 AND qualified_for_grove = 0 AND tree_state = 'SAPLING_ACTIVE')
                OR (is_active = 1 AND qualified_for_grove = 1 AND tree_state = 'GROWING_TREE')
                OR (is_active = 0 AND qualified_for_grove = 1 AND tree_state = 'DORMANT_TREE')
                OR (is_active = 0 AND qualified_for_grove = 0 AND tree_state = 'UNQUALIFIED_HISTORY')
            ),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms)
        );
        """.trimIndent()

    private fun buildWeekStartHistoryTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            effective_from_local_date TEXT NOT NULL PRIMARY KEY CHECK (
                length(effective_from_local_date) = 10
                AND effective_from_local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            week_start_iso_day INTEGER NOT NULL CHECK (week_start_iso_day BETWEEN 1 AND 7),
            is_legacy_baseline INTEGER NOT NULL DEFAULT 0 CHECK (is_legacy_baseline IN (0, 1)),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0)
        );
        """.trimIndent()

    private fun buildJournalEntriesTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            journal_entry_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(journal_entry_id)) > 0),
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            body TEXT NOT NULL CHECK (length(trim(body)) BETWEEN 1 AND 20000),
            title TEXT CHECK (title IS NULL OR length(title) <= 200),
            primary_mood_code TEXT,
            secondary_mood_code TEXT CHECK (
                secondary_mood_code IS NULL
                OR (primary_mood_code IS NOT NULL AND secondary_mood_code != primary_mood_code)
            ),
            prompt_id TEXT,
            linked_habit_id TEXT REFERENCES $TABLE_HABITS(habit_id) ON DELETE SET NULL,
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms)
        );
        """.trimIndent()

    private fun buildJournalDraftsTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            draft_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(draft_id)) > 0),
            local_date TEXT NOT NULL CHECK (
                length(local_date) = 10
                AND local_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
            ),
            title TEXT CHECK (title IS NULL OR length(title) <= 200),
            body TEXT NOT NULL DEFAULT '' CHECK (length(body) <= 20000),
            primary_mood_code TEXT,
            secondary_mood_code TEXT CHECK (
                secondary_mood_code IS NULL
                OR (primary_mood_code IS NOT NULL AND secondary_mood_code != primary_mood_code)
            ),
            prompt_id TEXT,
            linked_habit_id TEXT REFERENCES $TABLE_HABITS(habit_id) ON DELETE SET NULL,
            intended_journal_entry_id TEXT,
            source_context TEXT NOT NULL CHECK (
                source_context IN ('JOURNAL_COMPOSER', 'HABIT_REFLECTION', 'COMEBACK_30D')
            ),
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            updated_at_utc_ms INTEGER NOT NULL CHECK (updated_at_utc_ms >= created_at_utc_ms)
        );
        """.trimIndent()

    private fun buildReminderDeliveriesTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            delivery_key TEXT NOT NULL PRIMARY KEY CHECK (length(trim(delivery_key)) > 0),
            reminder_type TEXT NOT NULL CHECK (
                reminder_type IN ('HABIT', 'HABIT_DAILY', 'COMEBACK_14D', 'COMEBACK_30D')
            ),
            habit_id TEXT REFERENCES $TABLE_HABITS(habit_id) ON DELETE CASCADE,
            target_period_key TEXT NOT NULL CHECK (length(trim(target_period_key)) > 0),
            delivery_status TEXT NOT NULL CHECK (
                delivery_status IN ('SCHEDULED', 'DELIVERED', 'SUPPRESSED', 'CANCELLED', 'DISMISSED', 'ACTED')
            ),
            scheduled_at_utc_ms INTEGER NOT NULL CHECK (scheduled_at_utc_ms >= 0),
            attempted_at_utc_ms INTEGER,
            delivered_at_utc_ms INTEGER,
            platform_notification_tag TEXT,
            platform_notification_id INTEGER,
            suppression_reason TEXT,
            CHECK (
                (reminder_type IN ('HABIT', 'HABIT_DAILY') AND habit_id IS NOT NULL)
                OR (reminder_type IN ('COMEBACK_14D', 'COMEBACK_30D') AND habit_id IS NULL)
            )
        );
        """.trimIndent()

    private fun buildNotificationCancelOutboxTableSql(tableName: String): String =
        """
        CREATE TABLE $tableName (
            cancel_id TEXT NOT NULL PRIMARY KEY CHECK (length(trim(cancel_id)) > 0),
            platform_notification_tag TEXT NOT NULL CHECK (length(trim(platform_notification_tag)) > 0),
            platform_notification_id INTEGER NOT NULL,
            created_at_utc_ms INTEGER NOT NULL CHECK (created_at_utc_ms >= 0),
            last_attempt_at_utc_ms INTEGER,
            attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
            status TEXT NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
            error_category TEXT
        );
        """.trimIndent()

    private fun createV3Indexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_versions_lookup ON $TABLE_HABIT_VERSIONS(habit_id, effective_from_local_date DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_entries_habit_date ON $TABLE_HABIT_ENTRIES(habit_id, local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_entries_date ON $TABLE_HABIT_ENTRIES(local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_outcomes_date ON $TABLE_HABIT_DAY_OUTCOMES(local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_notes_date ON $TABLE_HABIT_DATE_NOTES(local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_habit_exceptions_habit_date ON $TABLE_HABIT_DATE_EXCEPTIONS(habit_id, local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_streak_runs_habit_active ON $TABLE_HABIT_STREAK_RUNS(habit_id, is_active);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_streak_runs_grove ON $TABLE_HABIT_STREAK_RUNS(qualified_for_grove, tree_state);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_entries_date ON $TABLE_JOURNAL_ENTRIES(local_date DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_entries_linked_habit ON $TABLE_JOURNAL_ENTRIES(linked_habit_id);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_drafts_date ON $TABLE_JOURNAL_DRAFTS(local_date);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminder_deliveries_habit_period ON $TABLE_REMINDER_DELIVERIES(habit_id, target_period_key);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cancel_outbox_status ON $TABLE_NOTIFICATION_CANCEL_OUTBOX(status, created_at_utc_ms);")

        // Partial unique index guaranteeing at most 1 active run per habit
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_habit_streak_runs_one_active_per_habit
            ON $TABLE_HABIT_STREAK_RUNS(habit_id)
            WHERE is_active = 1;
            """.trimIndent()
        )
    }

    private fun createV3Triggers(db: SQLiteDatabase) {
        // 1. habit_entries mode, idempotency, pause, and rest_day enforcement
        for ((triggerName, eventClause) in listOf(
            "trg_habit_entries_validate_insert" to "BEFORE INSERT ON $TABLE_HABIT_ENTRIES",
            "trg_habit_entries_validate_update" to "BEFORE UPDATE ON $TABLE_HABIT_ENTRIES"
        )) {
            db.execSQL("DROP TRIGGER IF EXISTS $triggerName;")
            db.execSQL(
                """
                CREATE TRIGGER $triggerName
                $eventClause
                BEGIN
                    SELECT CASE
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_DAY_OUTCOMES
                            WHERE idempotency_key = NEW.idempotency_key
                        ) THEN RAISE(ABORT, 'Cross-table duplicate idempotency_key in habit_day_outcomes')
                        WHEN COALESCE(
                            (
                                SELECT tracking_mode FROM $TABLE_HABIT_VERSIONS
                                WHERE habit_id = NEW.habit_id
                                  AND effective_from_local_date <= NEW.local_date
                                ORDER BY effective_from_local_date DESC, created_at_utc_ms DESC
                                LIMIT 1
                            ),
                            (
                                SELECT tracking_mode FROM $TABLE_HABIT_VERSIONS
                                WHERE habit_id = NEW.habit_id
                                ORDER BY effective_from_local_date ASC, created_at_utc_ms ASC
                                LIMIT 1
                            )
                        ) NOT IN ('QUANTITY', 'DURATION')
                        THEN RAISE(ABORT, 'habit_entries requires QUANTITY or DURATION tracking_mode for effective habit_version')
                        WHEN (
                            SELECT lifecycle_state FROM $TABLE_HABIT_VERSIONS
                            WHERE habit_id = NEW.habit_id
                              AND effective_from_local_date <= NEW.local_date
                            ORDER BY effective_from_local_date DESC, created_at_utc_ms DESC
                            LIMIT 1
                        ) IN ('PAUSED', 'ARCHIVED', 'COMPLETED')
                        THEN RAISE(ABORT, 'Cannot record habit_entries on a date where habit lifecycle_state is closed/paused')
                        WHEN NEW.value_units > 0 AND EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_DATE_EXCEPTIONS
                            WHERE habit_id = NEW.habit_id
                              AND local_date = NEW.local_date
                              AND exception_kind = 'REST_DAY'
                        )
                        THEN RAISE(ABORT, 'Cannot coexist positive habit_entries with REST_DAY on the same date')
                    END;
                END;
                """.trimIndent()
            )
        }

        // 2. habit_day_outcomes mode, idempotency, pause, and rest_day enforcement
        for ((triggerName, eventClause) in listOf(
            "trg_habit_outcomes_validate_insert" to "BEFORE INSERT ON $TABLE_HABIT_DAY_OUTCOMES",
            "trg_habit_outcomes_validate_update" to "BEFORE UPDATE ON $TABLE_HABIT_DAY_OUTCOMES"
        )) {
            db.execSQL("DROP TRIGGER IF EXISTS $triggerName;")
            db.execSQL(
                """
                CREATE TRIGGER $triggerName
                $eventClause
                BEGIN
                    SELECT CASE
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_ENTRIES
                            WHERE idempotency_key = NEW.idempotency_key
                        ) THEN RAISE(ABORT, 'Cross-table duplicate idempotency_key in habit_entries')
                        WHEN COALESCE(
                            (
                                SELECT tracking_mode FROM $TABLE_HABIT_VERSIONS
                                WHERE habit_id = NEW.habit_id
                                  AND effective_from_local_date <= NEW.local_date
                                ORDER BY effective_from_local_date DESC, created_at_utc_ms DESC
                                LIMIT 1
                            ),
                            (
                                SELECT tracking_mode FROM $TABLE_HABIT_VERSIONS
                                WHERE habit_id = NEW.habit_id
                                ORDER BY effective_from_local_date ASC, created_at_utc_ms ASC
                                LIMIT 1
                            )
                        ) NOT IN ('COMPLETION_ONLY', 'EFFORT_AWARE')
                        THEN RAISE(ABORT, 'habit_day_outcomes requires COMPLETION_ONLY or EFFORT_AWARE tracking_mode for effective habit_version')
                        WHEN (
                            SELECT lifecycle_state FROM $TABLE_HABIT_VERSIONS
                            WHERE habit_id = NEW.habit_id
                              AND effective_from_local_date <= NEW.local_date
                            ORDER BY effective_from_local_date DESC, created_at_utc_ms DESC
                            LIMIT 1
                        ) IN ('PAUSED', 'ARCHIVED', 'COMPLETED')
                        THEN RAISE(ABORT, 'Cannot record habit_day_outcomes on a date where habit lifecycle_state is closed/paused')
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_DATE_EXCEPTIONS
                            WHERE habit_id = NEW.habit_id
                              AND local_date = NEW.local_date
                              AND exception_kind = 'REST_DAY'
                        )
                        THEN RAISE(ABORT, 'Cannot coexist habit_day_outcomes with REST_DAY on the same date')
                    END;
                END;
                """.trimIndent()
            )
        }

        // 3. habit_date_exceptions REST_DAY mutual exclusion with real activity & paused lifecycle
        for ((triggerName, eventClause) in listOf(
            "trg_habit_exceptions_validate_insert" to "BEFORE INSERT ON $TABLE_HABIT_DATE_EXCEPTIONS",
            "trg_habit_exceptions_validate_update" to "BEFORE UPDATE ON $TABLE_HABIT_DATE_EXCEPTIONS"
        )) {
            db.execSQL("DROP TRIGGER IF EXISTS $triggerName;")
            db.execSQL(
                """
                CREATE TRIGGER $triggerName
                $eventClause
                BEGIN
                    SELECT CASE
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_ENTRIES
                            WHERE habit_id = NEW.habit_id
                              AND local_date = NEW.local_date
                        )
                        THEN RAISE(ABORT, 'Cannot insert REST_DAY when habit_entries already exist on that date')
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_DAY_OUTCOMES
                            WHERE habit_id = NEW.habit_id
                              AND local_date = NEW.local_date
                        )
                        THEN RAISE(ABORT, 'Cannot insert REST_DAY when habit_day_outcomes already exist on that date')
                        WHEN (
                            SELECT lifecycle_state FROM $TABLE_HABIT_VERSIONS
                            WHERE habit_id = NEW.habit_id
                              AND effective_from_local_date <= NEW.local_date
                            ORDER BY effective_from_local_date DESC, created_at_utc_ms DESC
                            LIMIT 1
                        ) IN ('PAUSED', 'ARCHIVED', 'COMPLETED')
                        THEN RAISE(ABORT, 'Cannot insert REST_DAY on a paused, archived, or completed habit date')
                    END;
                END;
                """.trimIndent()
            )
        }
    }

    // =========================================================================
    // DEFAULT SETTINGS & WEEK-START BASELINE SEEDING
    // =========================================================================

    private fun seedDefaultSettingsIfEmpty(db: SQLiteDatabase, nowMs: Long) {
        val defaults = listOf(
            Triple("onboarding_completed", "BOOL", "false"),
            Triple("week_start", "STRING", "MONDAY"),
            Triple("theme_mode", "STRING", "SYSTEM"),
            Triple("quiet_hours_start", "STRING", "22:00"),
            Triple("quiet_hours_end", "STRING", "07:00"),
            Triple("daily_reminder_cap", "INT", "3"),
            Triple("retrospective_journal_prompts_enabled", "BOOL", "true")
        )
        for ((key, type, value) in defaults) {
            val cv = ContentValues().apply {
                put("setting_key", key)
                put("value_type", type)
                put("value_text", value)
                put("schema_version", 1)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertWithOnConflict(TABLE_APP_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun seedDefaultWeekStartBaselineIfEmpty(db: SQLiteDatabase, nowMs: Long) {
        var count = 0
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_WEEK_START_HISTORY", null).use { c ->
            if (c.moveToFirst()) count = c.getInt(0)
        }
        if (count > 0) return

        var weekStartIsoDay = 1
        db.rawQuery(
            "SELECT value_text FROM $TABLE_APP_SETTINGS WHERE setting_key = 'week_start' LIMIT 1",
            null
        ).use { c ->
            if (c.moveToFirst()) {
                val txt = c.getString(0)?.trim()?.uppercase()
                weekStartIsoDay = when (txt) {
                    "SUNDAY", "7" -> 7
                    "SATURDAY", "6" -> 6
                    else -> 1
                }
            }
        }

        val earliestDate = findEarliestLocalDateInDb(db) ?: "1970-01-01"
        val cv = ContentValues().apply {
            put("effective_from_local_date", earliestDate)
            put("week_start_iso_day", weekStartIsoDay)
            put("is_legacy_baseline", 1)
            put("created_at_utc_ms", nowMs)
        }
        db.insertWithOnConflict(TABLE_WEEK_START_HISTORY, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun findEarliestLocalDateInDb(db: SQLiteDatabase): String? {
        val candidates = mutableListOf<String>()
        val queries = listOf(
            "SELECT MIN(effective_from_local_date) FROM $TABLE_HABIT_VERSIONS",
            "SELECT MIN(local_date) FROM $TABLE_HABIT_ENTRIES",
            "SELECT MIN(local_date) FROM $TABLE_HABIT_DAY_OUTCOMES",
            "SELECT MIN(local_date) FROM $TABLE_JOURNAL_ENTRIES"
        )
        for (sql in queries) {
            try {
                db.rawQuery(sql, null).use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) {
                        val d = c.getString(0)
                        if (LocalDateValidator.isValidStrict(d)) {
                            candidates.add(d)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return candidates.minOrNull()
    }

    // =========================================================================
    // V1 -> V2 FAIL-CLOSED MIGRATION (Preserved for v1 upgrade paths & AC-020)
    // =========================================================================

    fun migrateV1ToV2FailClosed(db: SQLiteDatabase) {
        // Pre-scan v1 daily_records for any unsupported SKIPPED outcome
        val hasDailyRecords = tableExists(db, "daily_records")
        if (hasDailyRecords) {
            db.rawQuery("SELECT outcome FROM daily_records", null).use { c ->
                while (c.moveToNext()) {
                    val outcome = c.getString(0)?.uppercase() ?: ""
                    if (outcome == "SKIPPED" || outcome !in setOf("COMPLETED", "EFFORT_LOGGED")) {
                        throw DayseedMigrationBlockedException(
                            "Migration blocked: unsupported v1 outcome '$outcome' found in daily_records."
                        )
                    }
                }
            }
        }

        // Read v1 tables into memory before replacing them
        data class V1Habit(
            val id: String,
            val name: String,
            val description: String?,
            val trackingMode: String,
            val tinyDesc: String?,
            val usualDesc: String?,
            val extraDesc: String?,
            val scheduleDays: String?,
            val isArchived: Int,
            val colorHex: String?,
            val createdAt: Long
        )
        val v1Habits = mutableListOf<V1Habit>()
        if (tableExists(db, "habits")) {
            db.rawQuery("SELECT id, name, description, tracking_mode, tiny_desc, usual_desc, extra_desc, schedule_days, is_archived, color_hex, created_at FROM habits", null).use { c ->
                while (c.moveToNext()) {
                    v1Habits.add(
                        V1Habit(
                            id = c.getString(0),
                            name = c.getString(1),
                            description = if (c.isNull(2)) null else c.getString(2),
                            trackingMode = c.getString(3),
                            tinyDesc = if (c.isNull(4)) null else c.getString(4),
                            usualDesc = if (c.isNull(5)) null else c.getString(5),
                            extraDesc = if (c.isNull(6)) null else c.getString(6),
                            scheduleDays = if (c.isNull(7)) null else c.getString(7),
                            isArchived = c.getInt(8),
                            colorHex = if (c.isNull(9)) null else c.getString(9),
                            createdAt = c.getLong(10)
                        )
                    )
                }
            }
        }

        data class V1Record(
            val id: String,
            val habitId: String,
            val recordDate: String,
            val outcome: String,
            val effortLevel: String?,
            val note: String?,
            val updatedAt: Long
        )
        val v1Records = mutableListOf<V1Record>()
        if (hasDailyRecords) {
            db.rawQuery("SELECT id, habit_id, record_date, outcome, effort_level, note, updated_at FROM daily_records", null).use { c ->
                while (c.moveToNext()) {
                    v1Records.add(
                        V1Record(
                            id = c.getString(0),
                            habitId = c.getString(1),
                            recordDate = c.getString(2),
                            outcome = c.getString(3),
                            effortLevel = if (c.isNull(4)) null else c.getString(4),
                            note = if (c.isNull(5)) null else c.getString(5),
                            updatedAt = c.getLong(6)
                        )
                    )
                }
            }
        }

        data class V1Journal(
            val id: String,
            val entryDate: String,
            val prompt: String?,
            val content: String,
            val createdAt: Long,
            val updatedAt: Long
        )
        val v1Journals = mutableListOf<V1Journal>()
        if (tableExists(db, "journal_entries")) {
            db.rawQuery("SELECT id, entry_date, prompt, content, created_at, updated_at FROM journal_entries", null).use { c ->
                while (c.moveToNext()) {
                    v1Journals.add(
                        V1Journal(
                            id = c.getString(0),
                            entryDate = c.getString(1),
                            prompt = if (c.isNull(2)) null else c.getString(2),
                            content = c.getString(3),
                            createdAt = c.getLong(4),
                            updatedAt = c.getLong(5)
                        )
                    )
                }
            }
        }

        val v1Prefs = mutableMapOf<String, String>()
        if (tableExists(db, "app_preferences")) {
            db.rawQuery("SELECT pref_key, pref_value FROM app_preferences", null).use { c ->
                while (c.moveToNext()) {
                    v1Prefs[c.getString(0)] = c.getString(1)
                }
            }
        }

        // Drop v1 tables and create v3 schema directly (or v2-compatible layout ready for v3)
        db.execSQL("DROP TABLE IF EXISTS daily_records;")
        db.execSQL("DROP TABLE IF EXISTS journal_entries;")
        db.execSQL("DROP TABLE IF EXISTS app_preferences;")
        db.execSQL("DROP TABLE IF EXISTS habits;")

        createV3Schema(db)

        val earliestRecordByHabit = v1Records.groupBy { it.habitId }
            .mapValues { (_, list) -> list.minOf { it.recordDate } }
        val latestRecordByHabit = v1Records.groupBy { it.habitId }
            .mapValues { (_, list) -> list.maxOf { it.recordDate } }

        for (h in v1Habits) {
            val hCv = ContentValues().apply {
                put("habit_id", h.id)
                put("created_at_utc_ms", h.createdAt)
                put("updated_at_utc_ms", h.createdAt)
            }
            db.insertOrThrow(TABLE_HABITS, null, hCv)

            val createdDate = Instant.ofEpochMilli(h.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val effDate = minOf(createdDate, earliestRecordByHabit[h.id] ?: createdDate)
            val latestRecordDate = latestRecordByHabit[h.id]
            val mappedMode = when (h.trackingMode.uppercase()) {
                "BINARY", "COMPLETION_ONLY" -> "COMPLETION_ONLY"
                "QUANTITY" -> "QUANTITY"
                "DURATION" -> "DURATION"
                else -> "EFFORT_AWARE"
            }

            val dayTokens = h.scheduleDays?.split(",")?.mapNotNull { raw ->
                val token = raw.trim().uppercase()
                token.toIntOrNull()?.takeIf { it in 1..7 }
                    ?: try {
                        java.time.DayOfWeek.valueOf(token).value
                    } catch (_: Exception) {
                        null
                    }
            }.orEmpty()
            var mask = 0
            for (d in dayTokens) {
                mask = mask or (1 shl (d - 1))
            }
            if (mask == 0) mask = WeekdayMask.ALL_DAYS_MASK
            val isDaily = mask == WeekdayMask.ALL_DAYS_MASK

            val initialLifecycle = if (h.isArchived == 1 && latestRecordDate == null) "ARCHIVED" else "ACTIVE"
            val vCv = ContentValues().apply {
                put("version_id", "v1_mig_${h.id}")
                put("habit_id", h.id)
                put("effective_from_local_date", effDate)
                put("name", h.name.trim().ifEmpty { "Habit" })
                if (!h.description.isNullOrBlank()) put("description", h.description.trim()) else putNull("description")
                put("tracking_mode", mappedMode)
                if (mappedMode == "QUANTITY") put("unit_code", "count")
                else if (mappedMode == "DURATION") put("unit_code", "min")
                else putNull("unit_code")
                put("target_period", "DAY")
                if (mappedMode in listOf("QUANTITY", "DURATION")) put("target_value", 1) else putNull("target_value")
                put("schedule_kind", if (isDaily) "DAILY" else "SELECTED_WEEKDAYS")
                if (isDaily) putNull("scheduled_weekdays_mask") else put("scheduled_weekdays_mask", mask)
                putNull("preferred_weekdays_mask")
                put("lifecycle_state", initialLifecycle)
                put("reminder_mode", "OFF")
                putNull("reminder_time_local")
                put("tiny_description", h.tinyDesc?.takeIf { it.isNotBlank() } ?: "Deliberately small action")
                put("usual_description", h.usualDesc?.takeIf { it.isNotBlank() } ?: "The ordinary intended action")
                put("extra_description", h.extraDesc?.takeIf { it.isNotBlank() } ?: "Optional stretch action")
                put("color_hex", h.colorHex?.takeIf { it.isNotBlank() } ?: "#286143")
                put("sortOrder".replace("sortOrder", "sort_order"), 0)
                put("created_at_utc_ms", h.createdAt)
            }
            db.insertOrThrow(TABLE_HABIT_VERSIONS, null, vCv)

            if (h.isArchived == 1 && latestRecordDate != null) {
                val archiveEffectiveDate = LocalDate.parse(latestRecordDate).plusDays(1).toString()
                val archCv = ContentValues(vCv).apply {
                    put("version_id", "v1_mig_arch_${h.id}")
                    put("effective_from_local_date", archiveEffectiveDate)
                    put("lifecycle_state", "ARCHIVED")
                }
                db.insertOrThrow(TABLE_HABIT_VERSIONS, null, archCv)
            }
        }

        val modeByHabit = v1Habits.associate {
            it.id to when (it.trackingMode.uppercase()) {
                "BINARY", "COMPLETION_ONLY" -> "COMPLETION_ONLY"
                "QUANTITY" -> "QUANTITY"
                "DURATION" -> "DURATION"
                else -> "EFFORT_AWARE"
            }
        }

        for (r in v1Records) {
            val mode = modeByHabit[r.habitId] ?: "EFFORT_AWARE"
            if (mode in listOf("QUANTITY", "DURATION")) {
                val eCv = ContentValues().apply {
                    put("entry_id", r.id)
                    put("habit_id", r.habitId)
                    put("local_date", r.recordDate)
                    put("value_units", 1)
                    if (!r.note.isNullOrBlank()) put("entry_note", r.note.trim()) else putNull("entry_note")
                    put("created_at_utc_ms", r.updatedAt)
                    put("updated_at_utc_ms", r.updatedAt)
                    put("idempotency_key", "v1_ent_${r.id}")
                }
                db.insertOrThrow(TABLE_HABIT_ENTRIES, null, eCv)
            } else {
                val outcomeKind = if (mode == "EFFORT_AWARE") "EFFORT_LOGGED" else "COMPLETED"
                val effort = if (outcomeKind == "EFFORT_LOGGED") {
                    val e = r.effortLevel?.uppercase()
                    if (e in setOf("TINY", "USUAL", "EXTRA")) e else "USUAL"
                } else {
                    null
                }

                val oCv = ContentValues().apply {
                    put("outcome_id", r.id)
                    put("habit_id", r.habitId)
                    put("local_date", r.recordDate)
                    put("outcome_kind", outcomeKind)
                    if (effort != null) put("effort_level", effort) else putNull("effort_level")
                    put("idempotency_key", "v1_out_${r.id}")
                    put("created_at_utc_ms", r.updatedAt)
                    put("updated_at_utc_ms", r.updatedAt)
                }
                db.insertOrThrow(TABLE_HABIT_DAY_OUTCOMES, null, oCv)
            }

            if (!r.note.isNullOrBlank()) {
                val nCv = ContentValues().apply {
                    put("note_id", "v1_note_${r.id}")
                    put("habit_id", r.habitId)
                    put("local_date", r.recordDate)
                    put("body", r.note.trim())
                    put("created_at_utc_ms", r.updatedAt)
                    put("updated_at_utc_ms", r.updatedAt)
                }
                db.insertOrThrow(TABLE_HABIT_DATE_NOTES, null, nCv)
            }
        }

        for (j in v1Journals) {
            if (j.content.isBlank()) continue
            val jCv = ContentValues().apply {
                put("journal_entry_id", j.id)
                put("local_date", j.entryDate)
                put("body", j.content.trim())
                putNull("title")
                putNull("primary_mood_code")
                putNull("secondary_mood_code")
                if (!j.prompt.isNullOrBlank()) put("prompt_id", j.prompt) else putNull("prompt_id")
                putNull("linked_habit_id")
                put("created_at_utc_ms", j.createdAt)
                put("updated_at_utc_ms", maxOf(j.createdAt, j.updatedAt))
            }
            db.insertOrThrow(TABLE_JOURNAL_ENTRIES, null, jCv)
        }

        val nowMs = System.currentTimeMillis()
        for ((k, v) in v1Prefs) {
            val type = when {
                v.equals("true", true) || v.equals("false", true) -> "BOOL"
                v.toLongOrNull() != null -> "INT"
                else -> "STRING"
            }
            val sCv = ContentValues().apply {
                put("setting_key", k)
                put("value_type", type)
                put("value_text", v)
                put("schema_version", 1)
                put("updated_at_utc_ms", nowMs)
            }
            db.insertWithOnConflict(TABLE_APP_SETTINGS, null, sCv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        seedDefaultSettingsIfEmpty(db, nowMs)
        seedDefaultWeekStartBaselineIfEmpty(db, nowMs)
    }

    // =========================================================================
    // V2 -> V3 FAIL-CLOSED MIGRATION (14-Step FK-Cascade-Safe Procedure, §7)
    // =========================================================================

    fun migrateV2ToV3FailClosed(
        db: SQLiteDatabase,
        evaluationDate: LocalDate = migrationEvaluationDateProvider()
    ) {
        db.execSQL("PRAGMA foreign_keys = ON;")

        // Step 1: Pre-scan v2 tables for strict calendar dates, non-negative units, and valid modes/outcomes
        validateV2DataBeforeMigration(db)

        // Step 2: Drop any old v2 triggers before rebuilding child tables
        dropAllUserTriggers(db)

        // Step 3: Add `category` and `lifetime_goal_days` to `habits` in-place via ALTER TABLE (NEVER drop `habits`!)
        val habitCols = getColumnNames(db, TABLE_HABITS)
        if ("category" !in habitCols) {
            db.execSQL("ALTER TABLE $TABLE_HABITS ADD COLUMN category TEXT;")
        }
        if ("lifetime_goal_days" !in habitCols) {
            db.execSQL(
                "ALTER TABLE $TABLE_HABITS ADD COLUMN lifetime_goal_days INTEGER CHECK (lifetime_goal_days IS NULL OR (lifetime_goal_days >= 1 AND lifetime_goal_days <= 36500));"
            )
        }

        // Step 4: Rebuild `habit_versions` with v3 columns (extracting DAYSEED_META JSON) & v3 CHECK constraints
        val versionCols = getColumnNames(db, TABLE_HABIT_VERSIONS)
        db.execSQL("DROP TABLE IF EXISTS habit_versions_v3_new;")
        db.execSQL(buildHabitVersionsTableSql("habit_versions_v3_new"))

        val hasTinyCol = "tiny_description" in versionCols
        val selectVersionsSql = if (hasTinyCol) {
            """
            SELECT version_id, habit_id, effective_from_local_date, name, description,
                   tracking_mode, unit_code, target_period, target_value, schedule_kind,
                   scheduled_weekdays_mask, preferred_weekdays_mask, lifecycle_state,
                   reminder_mode, reminder_time_local, created_at_utc_ms,
                   tiny_description, usual_description, extra_description, color_hex, sort_order
            FROM $TABLE_HABIT_VERSIONS
            ORDER BY created_at_utc_ms ASC
            """.trimIndent()
        } else {
            """
            SELECT version_id, habit_id, effective_from_local_date, name, description,
                   tracking_mode, unit_code, target_period, target_value, schedule_kind,
                   scheduled_weekdays_mask, preferred_weekdays_mask, lifecycle_state,
                   reminder_mode, reminder_time_local, created_at_utc_ms
            FROM $TABLE_HABIT_VERSIONS
            ORDER BY created_at_utc_ms ASC
            """.trimIndent()
        }

        db.rawQuery(selectVersionsSql, null).use { c ->
            while (c.moveToNext()) {
                val rawDesc = if (c.isNull(4)) null else c.getString(4)
                val extracted = extractLegacyHabitMeta(rawDesc)
                val rawTargetPeriod = c.getString(7)
                val rawScheduleKind = c.getString(9)
                val rawScheduledMask = if (c.isNull(10)) null else c.getValidatedInt(10, "scheduled_weekdays_mask")
                val rawPreferredMask = if (c.isNull(11)) null else c.getValidatedInt(11, "preferred_weekdays_mask")

                // Preserve legacy v2 weekly schedules (U-11):
                // - (DAILY, WEEK) -> (FLEXIBLE_WEEKLY_TARGET, WEEK, preferred_weekdays_mask = null)
                // - (SELECTED_WEEKDAYS, mask, WEEK) -> (FLEXIBLE_WEEKLY_TARGET, WEEK, scheduled_weekdays_mask = null, preferred_weekdays_mask = mask)
                val (normalizedScheduleKind, normalizedTargetPeriod, finalScheduledMask, finalPreferredMask) = when {
                    rawTargetPeriod == "WEEK" && rawScheduleKind == "DAILY" ->
                        Quadruple("FLEXIBLE_WEEKLY_TARGET", "WEEK", null, null)
                    rawTargetPeriod == "WEEK" && rawScheduleKind == "SELECTED_WEEKDAYS" ->
                        Quadruple("FLEXIBLE_WEEKLY_TARGET", "WEEK", null, rawScheduledMask)
                    rawScheduleKind == "FLEXIBLE_WEEKLY_TARGET" ->
                        Quadruple("FLEXIBLE_WEEKLY_TARGET", "WEEK", null, rawPreferredMask)
                    else ->
                        Quadruple(rawScheduleKind, "DAY", rawScheduledMask, null)
                }

                val cv = ContentValues().apply {
                    put("version_id", c.getString(0))
                    put("habit_id", c.getString(1))
                    put("effective_from_local_date", c.getString(2))
                    put("name", c.getString(3))
                    if (extracted.cleanDescription != null) put("description", extracted.cleanDescription) else putNull("description")
                    put("tracking_mode", TrackingMode.fromDb(c.getString(5)).canonical().name)
                    if (c.isNull(6)) putNull("unit_code") else put("unit_code", c.getString(6))
                    put("target_period", normalizedTargetPeriod)
                    if (c.isNull(8)) putNull("target_value") else put("target_value", c.getValidatedInt(8, "target_value"))
                    put("schedule_kind", normalizedScheduleKind)
                    if (finalScheduledMask == null) putNull("scheduled_weekdays_mask") else put("scheduled_weekdays_mask", finalScheduledMask)
                    if (finalPreferredMask == null) putNull("preferred_weekdays_mask") else put("preferred_weekdays_mask", finalPreferredMask)
                    put("lifecycle_state", c.getString(12))
                    put("reminder_mode", c.getString(13))
                    if (c.isNull(14)) putNull("reminder_time_local") else put("reminder_time_local", c.getString(14))
                    put("created_at_utc_ms", c.getLong(15))
                    if (hasTinyCol) {
                        put("tiny_description", c.getString(16) ?: extracted.tiny)
                        put("usual_description", c.getString(17) ?: extracted.usual)
                        put("extra_description", c.getString(18) ?: extracted.extra)
                        put("color_hex", c.getString(19) ?: extracted.colorHex)
                        put("sort_order", c.getValidatedInt(20, "sort_order"))
                    } else {
                        put("tiny_description", extracted.tiny)
                        put("usual_description", extracted.usual)
                        put("extra_description", extracted.extra)
                        put("color_hex", extracted.colorHex)
                        put("sort_order", 0)
                    }
                }
                db.insertOrThrow("habit_versions_v3_new", null, cv)
            }
        }
        db.execSQL("DROP TABLE $TABLE_HABIT_VERSIONS;")
        db.execSQL("ALTER TABLE habit_versions_v3_new RENAME TO $TABLE_HABIT_VERSIONS;")

        // Step 5: Rebuild child tables (habit_entries, habit_day_outcomes, habit_date_notes, journal_entries, journal_drafts, reminder_deliveries, notification_cancel_outbox)
        rebuildTableCopy(
            db = db,
            tableName = TABLE_HABIT_ENTRIES,
            createSql = buildHabitEntriesTableSql("habit_entries_v3_new"),
            tempName = "habit_entries_v3_new",
            columns = "entry_id, habit_id, local_date, value_units, entry_note, created_at_utc_ms, updated_at_utc_ms, idempotency_key"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_HABIT_DAY_OUTCOMES,
            createSql = buildHabitDayOutcomesTableSql("habit_day_outcomes_v3_new"),
            tempName = "habit_day_outcomes_v3_new",
            columns = "outcome_id, habit_id, local_date, outcome_kind, effort_level, idempotency_key, created_at_utc_ms, updated_at_utc_ms"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_HABIT_DATE_NOTES,
            createSql = buildHabitDateNotesTableSql("habit_date_notes_v3_new"),
            tempName = "habit_date_notes_v3_new",
            columns = "note_id, habit_id, local_date, body, created_at_utc_ms, updated_at_utc_ms"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_JOURNAL_ENTRIES,
            createSql = buildJournalEntriesTableSql("journal_entries_v3_new"),
            tempName = "journal_entries_v3_new",
            columns = "journal_entry_id, local_date, body, title, primary_mood_code, secondary_mood_code, prompt_id, linked_habit_id, created_at_utc_ms, updated_at_utc_ms"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_JOURNAL_DRAFTS,
            createSql = buildJournalDraftsTableSql("journal_drafts_v3_new"),
            tempName = "journal_drafts_v3_new",
            columns = "draft_id, local_date, title, body, primary_mood_code, secondary_mood_code, prompt_id, linked_habit_id, intended_journal_entry_id, source_context, created_at_utc_ms, updated_at_utc_ms"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_REMINDER_DELIVERIES,
            createSql = buildReminderDeliveriesTableSql("reminder_deliveries_v3_new"),
            tempName = "reminder_deliveries_v3_new",
            columns = "delivery_key, reminder_type, habit_id, target_period_key, delivery_status, scheduled_at_utc_ms, attempted_at_utc_ms, delivered_at_utc_ms, platform_notification_tag, platform_notification_id, suppression_reason"
        )
        rebuildTableCopy(
            db = db,
            tableName = TABLE_NOTIFICATION_CANCEL_OUTBOX,
            createSql = buildNotificationCancelOutboxTableSql("notification_cancel_outbox_v3_new"),
            tempName = "notification_cancel_outbox_v3_new",
            columns = "cancel_id, platform_notification_tag, platform_notification_id, created_at_utc_ms, last_attempt_at_utc_ms, attempt_count, status, error_category"
        )

        // Rebuild restore_conflicts to expand allowed record_type CHECK constraint for v3 without touching restore_sessions
        rebuildTableCopy(
            db = db,
            tableName = TABLE_RESTORE_CONFLICTS,
            createSql = buildRestoreConflictsTableSql("restore_conflicts_v3_new"),
            tempName = "restore_conflicts_v3_new",
            columns = "conflict_id, restore_session_id, record_type, record_key, current_payload_json, backup_payload_json, created_at_utc_ms"
        )

        // Step 6: Create the 3 new v3 tables if they do not yet exist
        if (!tableExists(db, TABLE_HABIT_DATE_EXCEPTIONS)) {
            db.execSQL(buildHabitDateExceptionsTableSql(TABLE_HABIT_DATE_EXCEPTIONS))
        }
        if (!tableExists(db, TABLE_HABIT_STREAK_RUNS)) {
            db.execSQL(buildHabitStreakRunsTableSql(TABLE_HABIT_STREAK_RUNS))
        }
        if (!tableExists(db, TABLE_WEEK_START_HISTORY)) {
            db.execSQL(buildWeekStartHistoryTableSql(TABLE_WEEK_START_HISTORY))
        }

        val nowMs = System.currentTimeMillis()
        seedDefaultSettingsIfEmpty(db, nowMs)
        seedDefaultWeekStartBaselineIfEmpty(db, nowMs)

        // Step 7: Recreate all 13 named indexes + partial unique index + triggers
        createV3Indexes(db)
        createV3Triggers(db)

        // Step 8: Backfill habit_streak_runs for all habits using StreakAndGroveEngine
        backfillStreakRunsDuringMigration(db, evaluationDate, nowMs)

        // Step 9: Verify PRAGMA foreign_key_check and PRAGMA integrity_check
        db.rawQuery("PRAGMA foreign_key_check;", null).use { c ->
            if (c.moveToFirst()) {
                val tbl = c.getString(0)
                throw DayseedMigrationBlockedException("PRAGMA foreign_key_check failed on table '$tbl' during v2->v3 migration.")
            }
        }
        db.rawQuery("PRAGMA integrity_check;", null).use { c ->
            if (c.moveToFirst()) {
                val res = c.getString(0)
                if (!res.equals("ok", ignoreCase = true)) {
                    throw DayseedMigrationBlockedException("PRAGMA integrity_check failed during v2->v3 migration: $res")
                }
            }
        }
    }

    private fun validateV2DataBeforeMigration(db: SQLiteDatabase) {
        // 1. Validate local_date columns across all date-bearing tables
        val dateChecks = listOf(
            TABLE_HABIT_VERSIONS to "effective_from_local_date",
            TABLE_HABIT_ENTRIES to "local_date",
            TABLE_HABIT_DAY_OUTCOMES to "local_date",
            TABLE_HABIT_DATE_NOTES to "local_date",
            TABLE_JOURNAL_ENTRIES to "local_date",
            TABLE_JOURNAL_DRAFTS to "local_date"
        )
        for ((table, col) in dateChecks) {
            if (!tableExists(db, table)) continue
            db.rawQuery("SELECT $col FROM $table", null).use { c ->
                while (c.moveToNext()) {
                    val raw = if (c.isNull(0)) null else c.getString(0)
                    if (!LocalDateValidator.isValidStrict(raw)) {
                        throw DayseedMigrationBlockedException(
                            "Migration blocked: invalid calendar date '$raw' in $table.$col"
                        )
                    }
                }
            }
        }

        // 2. Validate habit_entries value_units range
        if (tableExists(db, TABLE_HABIT_ENTRIES)) {
            db.rawQuery("SELECT value_units FROM $TABLE_HABIT_ENTRIES", null).use { c ->
                while (c.moveToNext()) {
                    val v = try {
                        c.getValidatedInt(0, "value_units")
                    } catch (e: DayseedNumericRangeException) {
                        throw DayseedMigrationBlockedException("Migration blocked: ${e.message}")
                    }
                    if (v < 0) {
                        throw DayseedMigrationBlockedException("Migration blocked: negative value_units ($v) in habit_entries.")
                    }
                }
            }
        }

        // 3. Validate habit_day_outcomes outcome_kind
        if (tableExists(db, TABLE_HABIT_DAY_OUTCOMES)) {
            db.rawQuery("SELECT outcome_kind, effort_level FROM $TABLE_HABIT_DAY_OUTCOMES", null).use { c ->
                while (c.moveToNext()) {
                    val kind = c.getString(0)
                    val eff = if (c.isNull(1)) null else c.getString(1)
                    if (kind !in setOf("COMPLETED", "EFFORT_LOGGED")) {
                        throw DayseedMigrationBlockedException("Migration blocked: unsupported outcome_kind '$kind' in habit_day_outcomes.")
                    }
                    if (kind == "EFFORT_LOGGED" && eff !in setOf("TINY", "USUAL", "EXTRA")) {
                        throw DayseedMigrationBlockedException("Migration blocked: invalid effort_level '$eff' for EFFORT_LOGGED.")
                    }
                }
            }
        }

        // 4. Validate habit_versions schedule_kind, target_period, masks, and target_value
        if (tableExists(db, TABLE_HABIT_VERSIONS)) {
            db.rawQuery(
                """
                SELECT version_id, tracking_mode, target_period, target_value,
                       schedule_kind, scheduled_weekdays_mask, preferred_weekdays_mask
                FROM $TABLE_HABIT_VERSIONS
                """.trimIndent(),
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val vId = if (c.isNull(0)) "" else c.getString(0)
                    val rawMode = if (c.isNull(1)) "" else c.getString(1)
                    val canonicalMode = try {
                        TrackingMode.fromDb(rawMode).canonical().name
                    } catch (_: Exception) {
                        throw DayseedMigrationBlockedException("Migration blocked: invalid tracking_mode '$rawMode' on version '$vId'.")
                    }
                    val period = if (c.isNull(2)) "" else c.getString(2)
                    if (period !in setOf("DAY", "WEEK")) {
                        throw DayseedMigrationBlockedException("Migration blocked: invalid target_period '$period' on version '$vId'.")
                    }
                    val targetVal = try {
                        c.getNullableValidatedInt(3, "target_value")
                    } catch (e: DayseedNumericRangeException) {
                        throw DayseedMigrationBlockedException("Migration blocked: ${e.message}")
                    }
                    val schedKind = if (c.isNull(4)) "" else c.getString(4)
                    if (schedKind !in setOf("DAILY", "SELECTED_WEEKDAYS", "FLEXIBLE_WEEKLY_TARGET")) {
                        throw DayseedMigrationBlockedException("Migration blocked: invalid schedule_kind '$schedKind' on version '$vId'.")
                    }
                    val schedMask = try {
                        c.getNullableValidatedInt(5, "scheduled_weekdays_mask")
                    } catch (e: DayseedNumericRangeException) {
                        throw DayseedMigrationBlockedException("Migration blocked: ${e.message}")
                    }
                    val prefMask = try {
                        c.getNullableValidatedInt(6, "preferred_weekdays_mask")
                    } catch (e: DayseedNumericRangeException) {
                        throw DayseedMigrationBlockedException("Migration blocked: ${e.message}")
                    }

                    // Validate schedule/mask/period invariants (allowing legacy v2 DAILY+WEEK and SELECTED_WEEKDAYS+WEEK)
                    when (schedKind) {
                        "DAILY" -> {
                            if (schedMask != null || prefMask != null) {
                                throw DayseedMigrationBlockedException("Migration blocked: DAILY schedule_kind cannot have weekday masks on version '$vId'.")
                            }
                        }
                        "SELECTED_WEEKDAYS" -> {
                            if (schedMask == null || schedMask !in 1..127 || prefMask != null) {
                                throw DayseedMigrationBlockedException("Migration blocked: invalid SELECTED_WEEKDAYS mask ($schedMask) on version '$vId'.")
                            }
                        }
                        "FLEXIBLE_WEEKLY_TARGET" -> {
                            if (period != "WEEK" || schedMask != null || (prefMask != null && prefMask !in 1..127)) {
                                throw DayseedMigrationBlockedException("Migration blocked: invalid FLEXIBLE_WEEKLY_TARGET configuration (period=$period, schedMask=$schedMask, prefMask=$prefMask) on version '$vId'.")
                            }
                        }
                    }

                    // Validate target_value bounds
                    if (canonicalMode in setOf("QUANTITY", "DURATION")) {
                        if (targetVal == null || targetVal < 1) {
                            throw DayseedMigrationBlockedException("Migration blocked: $canonicalMode requires target_value >= 1 (was $targetVal) on version '$vId'.")
                        }
                    } else {
                        if (period == "DAY" && targetVal != null) {
                            throw DayseedMigrationBlockedException("Migration blocked: daily $canonicalMode requires target_value IS NULL (was $targetVal) on version '$vId'.")
                        }
                        if (period == "WEEK" && targetVal != null && targetVal < 1) {
                            throw DayseedMigrationBlockedException("Migration blocked: weekly $canonicalMode requires target_value IS NULL or >= 1 (was $targetVal) on version '$vId'.")
                        }
                    }
                }
            }
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * Preserves D-219 (`DATABASE_VERSION = 3`) while idempotently upgrading any existing v3 database
     * whose `habit_versions` or `restore_conflicts` table still has the older v3 CHECK constraints.
     */
    fun reconcileExistingV3SchemaIfNeeded(db: SQLiteDatabase) {
        val habitVersionsSql = getTableCreateSql(db, TABLE_HABIT_VERSIONS)
        val restoreConflictsSql = getTableCreateSql(db, TABLE_RESTORE_CONFLICTS)

        val needsHabitVersionsUpgrade = habitVersionsSql != null &&
            !habitVersionsSql.contains("target_period = 'WEEK' AND (target_value IS NULL")
        val needsRestoreConflictsUpgrade = restoreConflictsSql != null &&
            !restoreConflictsSql.contains("'habit_streak_run'")

        if (!needsHabitVersionsUpgrade && !needsRestoreConflictsUpgrade) {
            return
        }

        db.beginTransaction()
        try {
            if (needsHabitVersionsUpgrade) {
                validateV2DataBeforeMigration(db)
            }
            if (needsRestoreConflictsUpgrade && tableExists(db, TABLE_RESTORE_CONFLICTS)) {
                val allowedTypes = setOf(
                    "habit",
                    "habit_version",
                    "habit_entry",
                    "habit_day_outcome",
                    "habit_date_note",
                    "habit_date_exception",
                    "habit_streak_run",
                    "journal_entry",
                    "journal_draft",
                    "app_setting",
                    "week_start_history"
                )
                db.rawQuery("SELECT conflict_id, record_type, record_key, created_at_utc_ms FROM $TABLE_RESTORE_CONFLICTS", null).use { c ->
                    while (c.moveToNext()) {
                        val cId = if (c.isNull(0)) "" else c.getString(0).trim()
                        val rType = if (c.isNull(1)) "" else c.getString(1)
                        val rKey = if (c.isNull(2)) "" else c.getString(2).trim()
                        val createdMs = if (c.isNull(3)) -1L else c.getLong(3)
                        if (cId.isEmpty() || rType !in allowedTypes || rKey.isEmpty() || createdMs < 0L) {
                            throw DayseedMigrationBlockedException(
                                "v3 schema reconciliation blocked: invalid restore_conflicts row (conflict_id='$cId', record_type='$rType')."
                            )
                        }
                    }
                }
            }

            dropAllUserTriggers(db)

            if (needsHabitVersionsUpgrade) {
                rebuildTableCopy(
                    db = db,
                    tableName = TABLE_HABIT_VERSIONS,
                    createSql = buildHabitVersionsTableSql("habit_versions_v3_reconcile"),
                    tempName = "habit_versions_v3_reconcile",
                    columns = "version_id, habit_id, effective_from_local_date, name, description, tracking_mode, unit_code, target_period, target_value, schedule_kind, scheduled_weekdays_mask, preferred_weekdays_mask, lifecycle_state, reminder_mode, reminder_time_local, tiny_description, usual_description, extra_description, color_hex, sort_order, created_at_utc_ms"
                )
            }

            if (needsRestoreConflictsUpgrade) {
                rebuildTableCopy(
                    db = db,
                    tableName = TABLE_RESTORE_CONFLICTS,
                    createSql = buildRestoreConflictsTableSql("restore_conflicts_v3_reconcile"),
                    tempName = "restore_conflicts_v3_reconcile",
                    columns = "conflict_id, restore_session_id, record_type, record_key, current_payload_json, backup_payload_json, created_at_utc_ms"
                )
            }

            createV3Indexes(db)
            createV3Triggers(db)

            db.rawQuery("PRAGMA foreign_key_check;", null).use { c ->
                if (c.moveToFirst()) {
                    val tbl = c.getString(0)
                    throw DayseedMigrationBlockedException("PRAGMA foreign_key_check failed on table '$tbl' during v3 schema reconciliation.")
                }
            }
            db.rawQuery("PRAGMA integrity_check;", null).use { c ->
                if (c.moveToFirst()) {
                    val res = c.getString(0)
                    if (!res.equals("ok", ignoreCase = true)) {
                        throw DayseedMigrationBlockedException("PRAGMA integrity_check failed during v3 schema reconciliation: $res")
                    }
                }
            }

            db.setTransactionSuccessful()
        } catch (e: DayseedMigrationBlockedException) {
            throw e
        } catch (e: Exception) {
            throw DayseedMigrationBlockedException("v3 schema reconciliation failed and rolled back: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    private fun getTableCreateSql(db: SQLiteDatabase, tableName: String): String? {
        db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { c ->
            return if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }

    private data class ExtractedHabitMeta(
        val cleanDescription: String?,
        val tiny: String,
        val usual: String,
        val extra: String,
        val colorHex: String
    )

    private fun extractLegacyHabitMeta(raw: String?): ExtractedHabitMeta {
        if (raw.isNullOrBlank()) {
            return ExtractedHabitMeta(
                cleanDescription = null,
                tiny = "Deliberately small action",
                usual = "The ordinary intended action",
                extra = "Optional stretch action",
                colorHex = "#286143"
            )
        }
        val htmlCommentRegex = Regex("""\s*<!--DAYSEED_META:(\{.*?\})-->\s*$""", RegexOption.DOT_MATCHES_ALL)
        val htmlMatch = htmlCommentRegex.find(raw)
        if (htmlMatch != null) {
            return try {
                val obj = JSONObject(htmlMatch.groupValues[1])
                val strippedDesc = raw.substring(0, htmlMatch.range.first).trim()
                val desc = strippedDesc.ifEmpty { obj.optString("desc", "").trim() }.ifEmpty { null }
                ExtractedHabitMeta(
                    cleanDescription = desc,
                    tiny = obj.optString("tiny", "Deliberately small action").ifBlank { "Deliberately small action" },
                    usual = obj.optString("usual", "The ordinary intended action").ifBlank { "The ordinary intended action" },
                    extra = obj.optString("extra", "Optional stretch action").ifBlank { "Optional stretch action" },
                    colorHex = obj.optString("color", "#286143").ifBlank { "#286143" }
                )
            } catch (_: Exception) {
                ExtractedHabitMeta(
                    cleanDescription = raw,
                    tiny = "Deliberately small action",
                    usual = "The ordinary intended action",
                    extra = "Optional stretch action",
                    colorHex = "#286143"
                )
            }
        }
        if (raw.startsWith("DAYSEED_META:")) {
            return try {
                val obj = JSONObject(raw.removePrefix("DAYSEED_META:"))
                val desc = obj.optString("desc", "").trim().ifEmpty { null }
                ExtractedHabitMeta(
                    cleanDescription = desc,
                    tiny = obj.optString("tiny", "Deliberately small action").ifBlank { "Deliberately small action" },
                    usual = obj.optString("usual", "The ordinary intended action").ifBlank { "The ordinary intended action" },
                    extra = obj.optString("extra", "Optional stretch action").ifBlank { "Optional stretch action" },
                    colorHex = obj.optString("color", "#286143").ifBlank { "#286143" }
                )
            } catch (_: Exception) {
                ExtractedHabitMeta(
                    cleanDescription = raw,
                    tiny = "Deliberately small action",
                    usual = "The ordinary intended action",
                    extra = "Optional stretch action",
                    colorHex = "#286143"
                )
            }
        }
        return ExtractedHabitMeta(
            cleanDescription = raw,
            tiny = "Deliberately small action",
            usual = "The ordinary intended action",
            extra = "Optional stretch action",
            colorHex = "#286143"
        )
    }

    private fun rebuildTableCopy(
        db: SQLiteDatabase,
        tableName: String,
        createSql: String,
        tempName: String,
        columns: String
    ) {
        if (!tableExists(db, tableName)) {
            db.execSQL(createSql.replace(tempName, tableName))
            return
        }
        db.execSQL("DROP TABLE IF EXISTS $tempName;")
        db.execSQL(createSql)
        db.execSQL("INSERT INTO $tempName ($columns) SELECT $columns FROM $tableName;")
        db.execSQL("DROP TABLE $tableName;")
        db.execSQL("ALTER TABLE $tempName RENAME TO $tableName;")
    }

    private fun backfillStreakRunsDuringMigration(
        db: SQLiteDatabase,
        evaluationDate: LocalDate,
        nowMs: Long
    ) {
        val habitIds = mutableListOf<String>()
        db.rawQuery("SELECT habit_id FROM $TABLE_HABITS", null).use { c ->
            while (c.moveToNext()) habitIds.add(c.getString(0))
        }
        if (habitIds.isEmpty()) return

        val weekStartHistory = mutableListOf<WeekStartPolicyEntry>()
        db.rawQuery(
            "SELECT effective_from_local_date, week_start_iso_day, is_legacy_baseline, created_at_utc_ms FROM $TABLE_WEEK_START_HISTORY ORDER BY effective_from_local_date ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                weekStartHistory.add(
                    WeekStartPolicyEntry(
                        effectiveFromLocalDate = c.getString(0),
                        weekStartIsoDay = c.getInt(1),
                        isLegacyBaseline = c.getInt(2) == 1,
                        createdAtUtcMs = c.getLong(3)
                    )
                )
            }
        }

        for (habitId in habitIds) {
            val versions = mutableListOf<HabitVersion>()
            db.rawQuery(
                """
                SELECT version_id, habit_id, effective_from_local_date, name, description,
                       tracking_mode, unit_code, target_period, target_value, schedule_kind,
                       scheduled_weekdays_mask, preferred_weekdays_mask, lifecycle_state,
                       reminder_mode, reminder_time_local, tiny_description, usual_description,
                       extra_description, color_hex, sort_order, created_at_utc_ms
                FROM $TABLE_HABIT_VERSIONS
                WHERE habit_id = ?
                ORDER BY effective_from_local_date ASC, created_at_utc_ms ASC
                """.trimIndent(),
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    versions.add(
                        HabitVersion(
                            versionId = c.getString(0),
                            habitId = c.getString(1),
                            effectiveFromLocalDate = c.getString(2),
                            name = c.getString(3),
                            description = if (c.isNull(4)) null else c.getString(4),
                            trackingMode = TrackingMode.fromDb(c.getString(5)),
                            unitCode = if (c.isNull(6)) null else c.getString(6),
                            targetPeriod = TargetPeriod.valueOf(c.getString(7)),
                            targetValue = c.getNullableValidatedInt(8, "target_value"),
                            scheduleKind = ScheduleKind.valueOf(c.getString(9)),
                            scheduledWeekdaysMask = c.getNullableValidatedInt(10, "scheduled_weekdays_mask"),
                            preferredWeekdaysMask = c.getNullableValidatedInt(11, "preferred_weekdays_mask"),
                            lifecycleState = HabitLifecycleState.valueOf(c.getString(12)),
                            reminderMode = c.getString(13),
                            reminderTimeLocal = if (c.isNull(14)) null else c.getString(14),
                            tinyDescription = c.getString(15),
                            usualDescription = c.getString(16),
                            extraDescription = c.getString(17),
                            colorHex = c.getString(18),
                            sortOrder = c.getValidatedInt(19, "sort_order"),
                            createdAtUtcMs = c.getLong(20)
                        )
                    )
                }
            }

            val entries = mutableListOf<HabitEntry>()
            db.rawQuery(
                "SELECT entry_id, habit_id, local_date, value_units, entry_note, created_at_utc_ms, updated_at_utc_ms, idempotency_key FROM $TABLE_HABIT_ENTRIES WHERE habit_id = ?",
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    entries.add(
                        HabitEntry(
                            entryId = c.getString(0),
                            habitId = c.getString(1),
                            localDate = c.getString(2),
                            valueUnits = c.getValidatedInt(3, "value_units"),
                            entryNote = if (c.isNull(4)) null else c.getString(4),
                            createdAtUtcMs = c.getLong(5),
                            updatedAtUtcMs = c.getLong(6),
                            idempotencyKey = c.getString(7)
                        )
                    )
                }
            }

            val outcomes = mutableListOf<HabitDayOutcome>()
            db.rawQuery(
                "SELECT outcome_id, habit_id, local_date, outcome_kind, effort_level, idempotency_key, created_at_utc_ms, updated_at_utc_ms FROM $TABLE_HABIT_DAY_OUTCOMES WHERE habit_id = ?",
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    outcomes.add(
                        HabitDayOutcome(
                            outcomeId = c.getString(0),
                            habitId = c.getString(1),
                            localDate = c.getString(2),
                            outcomeKind = c.getString(3),
                            effortLevel = if (c.isNull(4)) null else c.getString(4),
                            idempotencyKey = c.getString(5),
                            createdAtUtcMs = c.getLong(6),
                            updatedAtUtcMs = c.getLong(7)
                        )
                    )
                }
            }

            val exceptions = mutableListOf<HabitDateException>()
            db.rawQuery(
                "SELECT id, habit_id, local_date, exception_kind, note, created_at_utc_ms, updated_at_utc_ms FROM $TABLE_HABIT_DATE_EXCEPTIONS WHERE habit_id = ?",
                arrayOf(habitId)
            ).use { c ->
                while (c.moveToNext()) {
                    exceptions.add(
                        HabitDateException(
                            id = c.getString(0),
                            habitId = c.getString(1),
                            localDate = c.getString(2),
                            exceptionKind = HabitDateExceptionKind.valueOf(c.getString(3)),
                            note = if (c.isNull(4)) null else c.getString(4),
                            createdAtUtcMs = c.getLong(5),
                            updatedAtUtcMs = c.getLong(6)
                        )
                    )
                }
            }

            // Pick an evaluation date at least as late as the latest recorded activity for this habit
            val maxDataDate = (
                versions.map { it.effectiveFromLocalDate } +
                    entries.map { it.localDate } +
                    outcomes.map { it.localDate } +
                    exceptions.map { it.localDate }
                ).maxOrNull()?.let {
                try { LocalDateValidator.parseStrict(it) } catch (_: Exception) { null }
            }
            val effectiveEvalDate = if (maxDataDate != null && maxDataDate.isAfter(evaluationDate)) {
                maxDataDate
            } else {
                evaluationDate
            }

            StreakAndGroveEngine.syncHabitStreakRunsInDb(
                db = db,
                habitId = habitId,
                versions = versions,
                entries = entries,
                outcomes = outcomes,
                exceptions = exceptions,
                weekStartHistory = weekStartHistory,
                evaluationDate = effectiveEvalDate,
                nowUtcMs = nowMs
            )
        }
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { c ->
            return c.moveToFirst()
        }
    }

    private fun getColumnNames(db: SQLiteDatabase, tableName: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($tableName);", null).use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) {
                if (nameIdx >= 0) cols.add(c.getString(nameIdx))
            }
        }
        return cols
    }

    private fun dropAllUserTriggers(db: SQLiteDatabase) {
        val triggers = mutableListOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'trigger'", null).use { c ->
            while (c.moveToNext()) {
                triggers.add(c.getString(0))
            }
        }
        for (t in triggers) {
            db.execSQL("DROP TRIGGER IF EXISTS $t;")
        }
    }
}
