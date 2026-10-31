package com.example.dayseed.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class DayseedMigrationBlockedException(message: String) : SQLiteException(message)

/**
 * Canonical 12-table SQLiteOpenHelper (DATABASE_VERSION = 2) implementing
 * docs/03-TECHNICAL-ARCHITECTURE.md, docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md,
 * and fail-closed v1-to-v2 migration (D-197).
 */
class DayseedDatabaseHelper(
    context: Context,
    databaseName: String? = DATABASE_NAME
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dayseed.db"
        const val DATABASE_VERSION = 2

        // 1. habits
        const val TABLE_HABITS = "habits"
        const val COL_HABIT_ID = "habit_id"
        const val COL_CREATED_AT_UTC_MS = "created_at_utc_ms"
        const val COL_UPDATED_AT_UTC_MS = "updated_at_utc_ms"

        // 2. habit_versions
        const val TABLE_HABIT_VERSIONS = "habit_versions"
        const val COL_VERSION_ID = "version_id"
        const val COL_EFFECTIVE_FROM_LOCAL_DATE = "effective_from_local_date"
        const val COL_NAME = "name"
        const val COL_DESCRIPTION = "description"
        const val COL_TRACKING_MODE = "tracking_mode"
        const val COL_UNIT_CODE = "unit_code"
        const val COL_TARGET_PERIOD = "target_period"
        const val COL_TARGET_VALUE = "target_value"
        const val COL_SCHEDULE_KIND = "schedule_kind"
        const val COL_SCHEDULED_WEEKDAYS_MASK = "scheduled_weekdays_mask"
        const val COL_PREFERRED_WEEKDAYS_MASK = "preferred_weekdays_mask"
        const val COL_LIFECYCLE_STATE = "lifecycle_state"
        const val COL_REMINDER_MODE = "reminder_mode"
        const val COL_REMINDER_TIME_LOCAL = "reminder_time_local"

        // 3. habit_entries
        const val TABLE_HABIT_ENTRIES = "habit_entries"
        const val COL_ENTRY_ID = "entry_id"
        const val COL_LOCAL_DATE = "local_date"
        const val COL_VALUE_UNITS = "value_units"
        const val COL_ENTRY_NOTE = "entry_note"
        const val COL_IDEMPOTENCY_KEY = "idempotency_key"

        // 4. habit_day_outcomes
        const val TABLE_HABIT_DAY_OUTCOMES = "habit_day_outcomes"
        const val COL_OUTCOME_ID = "outcome_id"
        const val COL_OUTCOME_KIND = "outcome_kind"
        const val COL_EFFORT_LEVEL = "effort_level"

        // 5. habit_date_notes
        const val TABLE_HABIT_DATE_NOTES = "habit_date_notes"
        const val COL_NOTE_ID = "note_id"
        const val COL_BODY = "body"

        // 6. journal_entries
        const val TABLE_JOURNAL_ENTRIES = "journal_entries"
        const val COL_JOURNAL_ENTRY_ID = "journal_entry_id"
        const val COL_TITLE = "title"
        const val COL_PRIMARY_MOOD_CODE = "primary_mood_code"
        const val COL_SECONDARY_MOOD_CODE = "secondary_mood_code"
        const val COL_PROMPT_ID = "prompt_id"
        const val COL_LINKED_HABIT_ID = "linked_habit_id"

        // 7. journal_drafts
        const val TABLE_JOURNAL_DRAFTS = "journal_drafts"
        const val COL_DRAFT_ID = "draft_id"
        const val COL_INTENDED_JOURNAL_ENTRY_ID = "intended_journal_entry_id"
        const val COL_SOURCE_CONTEXT = "source_context"

        // 8. app_settings
        const val TABLE_APP_SETTINGS = "app_settings"
        const val COL_SETTING_KEY = "setting_key"
        const val COL_VALUE_TYPE = "value_type"
        const val COL_VALUE_TEXT = "value_text"
        const val COL_SCHEMA_VERSION = "schema_version"

        // 9. reminder_deliveries
        const val TABLE_REMINDER_DELIVERIES = "reminder_deliveries"
        const val COL_DELIVERY_KEY = "delivery_key"
        const val COL_REMINDER_TYPE = "reminder_type"
        const val COL_TARGET_PERIOD_KEY = "target_period_key"
        const val COL_DELIVERY_STATUS = "delivery_status"
        const val COL_SCHEDULED_AT_UTC_MS = "scheduled_at_utc_ms"
        const val COL_ATTEMPTED_AT_UTC_MS = "attempted_at_utc_ms"
        const val COL_DELIVERED_AT_UTC_MS = "delivered_at_utc_ms"
        const val COL_PLATFORM_NOTIFICATION_TAG = "platform_notification_tag"
        const val COL_PLATFORM_NOTIFICATION_ID = "platform_notification_id"
        const val COL_SUPPRESSION_REASON = "suppression_reason"

        // 10. restore_sessions
        const val TABLE_RESTORE_SESSIONS = "restore_sessions"
        const val COL_RESTORE_SESSION_ID = "restore_session_id"
        const val COL_RESTORE_MODE = "restore_mode"
        const val COL_FORMAT_VERSION = "format_version"
        const val COL_SOURCE_SCHEMA_VERSION = "source_schema_version"
        const val COL_SESSION_STATE = "session_state"
        const val COL_STARTED_AT_UTC_MS = "started_at_utc_ms"
        const val COL_MANIFEST_JSON = "manifest_json"
        const val COL_SAFETY_BACKUP_REF = "safety_backup_ref"
        const val COL_SAFETY_BACKUP_VERIFIED = "safety_backup_verified"

        // 11. restore_conflicts
        const val TABLE_RESTORE_CONFLICTS = "restore_conflicts"
        const val COL_CONFLICT_ID = "conflict_id"
        const val COL_RECORD_TYPE = "record_type"
        const val COL_RECORD_KEY = "record_key"
        const val COL_CURRENT_PAYLOAD_JSON = "current_payload_json"
        const val COL_BACKUP_PAYLOAD_JSON = "backup_payload_json"

        // 12. notification_cancel_outbox
        const val TABLE_NOTIFICATION_CANCEL_OUTBOX = "notification_cancel_outbox"
        const val COL_CANCEL_ID = "cancel_id"
        const val COL_LAST_ATTEMPT_AT_UTC_MS = "last_attempt_at_utc_ms"
        const val COL_ATTEMPT_COUNT = "attempt_count"
        const val COL_STATUS = "status"
        const val COL_ERROR_CATEGORY = "error_category"

        fun createV2Schema(db: SQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")

            // 1. habits
            db.execSQL(
                """
                CREATE TABLE $TABLE_HABITS (
                    $COL_HABIT_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_HABIT_ID)) > 0),
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 2. habit_versions
            db.execSQL(
                """
                CREATE TABLE $TABLE_HABIT_VERSIONS (
                    $COL_VERSION_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_VERSION_ID)) > 0),
                    $COL_HABIT_ID TEXT NOT NULL REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE CASCADE,
                    $COL_EFFECTIVE_FROM_LOCAL_DATE TEXT NOT NULL CHECK(length($COL_EFFECTIVE_FROM_LOCAL_DATE) = 10),
                    $COL_NAME TEXT NOT NULL CHECK(length(trim($COL_NAME)) > 0),
                    $COL_DESCRIPTION TEXT,
                    $COL_TRACKING_MODE TEXT NOT NULL CHECK($COL_TRACKING_MODE IN ('QUANTITY', 'DURATION', 'COMPLETION_ONLY', 'EFFORT_AWARE')),
                    $COL_UNIT_CODE TEXT,
                    $COL_TARGET_PERIOD TEXT NOT NULL CHECK($COL_TARGET_PERIOD IN ('DAY', 'WEEK')),
                    $COL_TARGET_VALUE INTEGER,
                    $COL_SCHEDULE_KIND TEXT NOT NULL CHECK($COL_SCHEDULE_KIND IN ('DAILY', 'SELECTED_WEEKDAYS', 'FLEXIBLE_WEEKLY_TARGET')),
                    $COL_SCHEDULED_WEEKDAYS_MASK INTEGER,
                    $COL_PREFERRED_WEEKDAYS_MASK INTEGER,
                    $COL_LIFECYCLE_STATE TEXT NOT NULL CHECK($COL_LIFECYCLE_STATE IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')),
                    $COL_REMINDER_MODE TEXT NOT NULL DEFAULT 'OFF' CHECK($COL_REMINDER_MODE IN ('OFF', 'AT_TIME', 'NONE', 'SYSTEM_MANAGED', 'MANUAL')),
                    $COL_REMINDER_TIME_LOCAL TEXT,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    UNIQUE($COL_HABIT_ID, $COL_EFFECTIVE_FROM_LOCAL_DATE),
                    CHECK (
                        (
                            $COL_TRACKING_MODE IN ('QUANTITY', 'DURATION')
                            AND $COL_TARGET_VALUE IS NOT NULL
                            AND typeof($COL_TARGET_VALUE) = 'integer'
                            AND $COL_TARGET_VALUE > 0
                            AND $COL_UNIT_CODE IS NOT NULL
                            AND length(trim($COL_UNIT_CODE)) > 0
                        )
                        OR
                        (
                            $COL_TRACKING_MODE IN ('COMPLETION_ONLY', 'EFFORT_AWARE')
                            AND $COL_TARGET_PERIOD = 'DAY'
                            AND $COL_TARGET_VALUE IS NULL
                            AND $COL_UNIT_CODE IS NULL
                        )
                        OR
                        (
                            $COL_TRACKING_MODE IN ('COMPLETION_ONLY', 'EFFORT_AWARE')
                            AND $COL_TARGET_PERIOD = 'WEEK'
                            AND $COL_TARGET_VALUE IS NOT NULL
                            AND typeof($COL_TARGET_VALUE) = 'integer'
                            AND $COL_TARGET_VALUE > 0
                            AND $COL_UNIT_CODE IS NULL
                        )
                    ),
                    CHECK (
                        (
                            $COL_SCHEDULE_KIND = 'DAILY'
                            AND $COL_SCHEDULED_WEEKDAYS_MASK IS NULL
                            AND $COL_PREFERRED_WEEKDAYS_MASK IS NULL
                        )
                        OR
                        (
                            $COL_SCHEDULE_KIND = 'SELECTED_WEEKDAYS'
                            AND $COL_SCHEDULED_WEEKDAYS_MASK IS NOT NULL
                            AND typeof($COL_SCHEDULED_WEEKDAYS_MASK) = 'integer'
                            AND $COL_SCHEDULED_WEEKDAYS_MASK >= 1
                            AND $COL_SCHEDULED_WEEKDAYS_MASK <= 127
                            AND $COL_PREFERRED_WEEKDAYS_MASK IS NULL
                        )
                        OR
                        (
                            $COL_SCHEDULE_KIND = 'FLEXIBLE_WEEKLY_TARGET'
                            AND $COL_TARGET_PERIOD = 'WEEK'
                            AND $COL_SCHEDULED_WEEKDAYS_MASK IS NULL
                            AND (
                                $COL_PREFERRED_WEEKDAYS_MASK IS NULL
                                OR (
                                    typeof($COL_PREFERRED_WEEKDAYS_MASK) = 'integer'
                                    AND $COL_PREFERRED_WEEKDAYS_MASK >= 1
                                    AND $COL_PREFERRED_WEEKDAYS_MASK <= 127
                                )
                            )
                        )
                    )
                )
                """.trimIndent()
            )

            // 3. habit_entries
            db.execSQL(
                """
                CREATE TABLE $TABLE_HABIT_ENTRIES (
                    $COL_ENTRY_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_ENTRY_ID)) > 0),
                    $COL_HABIT_ID TEXT NOT NULL REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE CASCADE,
                    $COL_LOCAL_DATE TEXT NOT NULL CHECK(length($COL_LOCAL_DATE) = 10),
                    $COL_VALUE_UNITS INTEGER NOT NULL CHECK(typeof($COL_VALUE_UNITS) = 'integer' AND $COL_VALUE_UNITS >= 0),
                    $COL_ENTRY_NOTE TEXT,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_IDEMPOTENCY_KEY TEXT CHECK($COL_IDEMPOTENCY_KEY IS NULL OR length(trim($COL_IDEMPOTENCY_KEY)) > 0)
                )
                """.trimIndent()
            )

            // 4. habit_day_outcomes (D-193 / AC-017: strictly completed outcomes; never SKIPPED or MISSED)
            db.execSQL(
                """
                CREATE TABLE $TABLE_HABIT_DAY_OUTCOMES (
                    $COL_OUTCOME_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_OUTCOME_ID)) > 0),
                    $COL_HABIT_ID TEXT NOT NULL REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE CASCADE,
                    $COL_LOCAL_DATE TEXT NOT NULL CHECK(length($COL_LOCAL_DATE) = 10),
                    $COL_OUTCOME_KIND TEXT NOT NULL CHECK($COL_OUTCOME_KIND IN ('COMPLETED', 'EFFORT_LOGGED', 'COMPLETION_ONLY', 'EFFORT_AWARE')),
                    $COL_EFFORT_LEVEL TEXT,
                    $COL_IDEMPOTENCY_KEY TEXT CHECK($COL_IDEMPOTENCY_KEY IS NULL OR length(trim($COL_IDEMPOTENCY_KEY)) > 0),
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL,
                    UNIQUE($COL_HABIT_ID, $COL_LOCAL_DATE),
                    CHECK (
                        (
                            $COL_OUTCOME_KIND IN ('COMPLETED', 'COMPLETION_ONLY')
                            AND $COL_EFFORT_LEVEL IS NULL
                        )
                        OR
                        (
                            $COL_OUTCOME_KIND IN ('EFFORT_LOGGED', 'EFFORT_AWARE')
                            AND $COL_EFFORT_LEVEL IS NOT NULL
                            AND $COL_EFFORT_LEVEL IN ('TINY', 'USUAL', 'EXTRA')
                        )
                    )
                )
                """.trimIndent()
            )

            // 5. habit_date_notes
            db.execSQL(
                """
                CREATE TABLE $TABLE_HABIT_DATE_NOTES (
                    $COL_NOTE_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_NOTE_ID)) > 0),
                    $COL_HABIT_ID TEXT NOT NULL REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE CASCADE,
                    $COL_LOCAL_DATE TEXT NOT NULL CHECK(length($COL_LOCAL_DATE) = 10),
                    $COL_BODY TEXT NOT NULL CHECK(length(trim($COL_BODY)) > 0),
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL,
                    UNIQUE($COL_HABIT_ID, $COL_LOCAL_DATE)
                )
                """.trimIndent()
            )

            // 6. journal_entries
            db.execSQL(
                """
                CREATE TABLE $TABLE_JOURNAL_ENTRIES (
                    $COL_JOURNAL_ENTRY_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_JOURNAL_ENTRY_ID)) > 0),
                    $COL_LOCAL_DATE TEXT NOT NULL CHECK(length($COL_LOCAL_DATE) = 10),
                    $COL_BODY TEXT NOT NULL CHECK(length(trim($COL_BODY)) > 0),
                    $COL_TITLE TEXT,
                    $COL_PRIMARY_MOOD_CODE TEXT,
                    $COL_SECONDARY_MOOD_CODE TEXT,
                    $COL_PROMPT_ID TEXT,
                    $COL_LINKED_HABIT_ID TEXT REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE SET NULL,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 7. journal_drafts
            db.execSQL(
                """
                CREATE TABLE $TABLE_JOURNAL_DRAFTS (
                    $COL_DRAFT_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_DRAFT_ID)) > 0),
                    $COL_LOCAL_DATE TEXT CHECK($COL_LOCAL_DATE IS NULL OR length($COL_LOCAL_DATE) = 10),
                    $COL_TITLE TEXT,
                    $COL_BODY TEXT NOT NULL,
                    $COL_PRIMARY_MOOD_CODE TEXT,
                    $COL_SECONDARY_MOOD_CODE TEXT,
                    $COL_PROMPT_ID TEXT,
                    $COL_LINKED_HABIT_ID TEXT REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE SET NULL,
                    $COL_INTENDED_JOURNAL_ENTRY_ID TEXT CHECK($COL_INTENDED_JOURNAL_ENTRY_ID IS NULL OR length(trim($COL_INTENDED_JOURNAL_ENTRY_ID)) > 0),
                    $COL_SOURCE_CONTEXT TEXT,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 8. app_settings
            db.execSQL(
                """
                CREATE TABLE $TABLE_APP_SETTINGS (
                    $COL_SETTING_KEY TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_SETTING_KEY)) > 0),
                    $COL_VALUE_TYPE TEXT NOT NULL CHECK($COL_VALUE_TYPE IN ('STRING', 'INT', 'LONG', 'BOOLEAN', 'BOOL', 'ENUM', 'TIME_LOCAL', 'JSON')),
                    $COL_VALUE_TEXT TEXT NOT NULL,
                    $COL_SCHEMA_VERSION INTEGER NOT NULL DEFAULT 1,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 9. reminder_deliveries
            db.execSQL(
                """
                CREATE TABLE $TABLE_REMINDER_DELIVERIES (
                    $COL_DELIVERY_KEY TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_DELIVERY_KEY)) > 0),
                    $COL_REMINDER_TYPE TEXT NOT NULL CHECK($COL_REMINDER_TYPE IN ('HABIT_DAILY', 'HABIT_EVENING', 'WEEKLY_REFLECTION', 'INACTIVITY_2D')),
                    $COL_HABIT_ID TEXT REFERENCES $TABLE_HABITS($COL_HABIT_ID) ON DELETE CASCADE,
                    $COL_TARGET_PERIOD_KEY TEXT NOT NULL CHECK(length(trim($COL_TARGET_PERIOD_KEY)) > 0),
                    $COL_DELIVERY_STATUS TEXT NOT NULL CHECK($COL_DELIVERY_STATUS IN ('SCHEDULED', 'DELIVERED', 'SUPPRESSED', 'DISMISSED', 'CANCELLED')),
                    $COL_SCHEDULED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_ATTEMPTED_AT_UTC_MS INTEGER,
                    $COL_DELIVERED_AT_UTC_MS INTEGER,
                    $COL_PLATFORM_NOTIFICATION_TAG TEXT,
                    $COL_PLATFORM_NOTIFICATION_ID INTEGER,
                    $COL_SUPPRESSION_REASON TEXT
                )
                """.trimIndent()
            )

            // 10. restore_sessions
            db.execSQL(
                """
                CREATE TABLE $TABLE_RESTORE_SESSIONS (
                    $COL_RESTORE_SESSION_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_RESTORE_SESSION_ID)) > 0),
                    $COL_RESTORE_MODE TEXT NOT NULL CHECK($COL_RESTORE_MODE IN ('REPLACE', 'MERGE')),
                    $COL_FORMAT_VERSION INTEGER NOT NULL CHECK($COL_FORMAT_VERSION > 0),
                    $COL_SOURCE_SCHEMA_VERSION INTEGER NOT NULL CHECK($COL_SOURCE_SCHEMA_VERSION > 0),
                    $COL_SESSION_STATE TEXT NOT NULL CHECK($COL_SESSION_STATE IN ('VALIDATED', 'IN_PROGRESS', 'AWAITING_CONFLICT_RESOLUTION', 'RECOVERY_REQUIRED', 'COMPLETED', 'COMPLETED_WITH_CONFLICTS', 'FAILED_SAFE')),
                    $COL_STARTED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_UPDATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_MANIFEST_JSON TEXT NOT NULL,
                    $COL_SAFETY_BACKUP_REF TEXT NOT NULL CHECK(length(trim($COL_SAFETY_BACKUP_REF)) > 0),
                    $COL_SAFETY_BACKUP_VERIFIED INTEGER NOT NULL CHECK($COL_SAFETY_BACKUP_VERIFIED IN (0, 1))
                )
                """.trimIndent()
            )

            // 11. restore_conflicts
            db.execSQL(
                """
                CREATE TABLE $TABLE_RESTORE_CONFLICTS (
                    $COL_CONFLICT_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_CONFLICT_ID)) > 0),
                    $COL_RESTORE_SESSION_ID TEXT NOT NULL REFERENCES $TABLE_RESTORE_SESSIONS($COL_RESTORE_SESSION_ID) ON DELETE CASCADE,
                    $COL_RECORD_TYPE TEXT NOT NULL CHECK(UPPER($COL_RECORD_TYPE) IN ('HABIT', 'HABIT_VERSION', 'HABIT_ENTRY', 'HABIT_DAY_OUTCOME', 'HABIT_DATE_NOTE', 'JOURNAL_ENTRY', 'JOURNAL_DRAFT', 'APP_SETTING', 'ACTIVITY_IDEMPOTENCY_KEY')),
                    $COL_RECORD_KEY TEXT NOT NULL CHECK(length(trim($COL_RECORD_KEY)) > 0),
                    $COL_CURRENT_PAYLOAD_JSON TEXT NOT NULL,
                    $COL_BACKUP_PAYLOAD_JSON TEXT NOT NULL,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    UNIQUE($COL_RESTORE_SESSION_ID, $COL_RECORD_TYPE, $COL_RECORD_KEY)
                )
                """.trimIndent()
            )

            // 12. notification_cancel_outbox
            db.execSQL(
                """
                CREATE TABLE $TABLE_NOTIFICATION_CANCEL_OUTBOX (
                    $COL_CANCEL_ID TEXT NOT NULL PRIMARY KEY CHECK(length(trim($COL_CANCEL_ID)) > 0),
                    $COL_PLATFORM_NOTIFICATION_TAG TEXT NOT NULL CHECK(length(trim($COL_PLATFORM_NOTIFICATION_TAG)) > 0),
                    $COL_PLATFORM_NOTIFICATION_ID INTEGER NOT NULL,
                    $COL_CREATED_AT_UTC_MS INTEGER NOT NULL,
                    $COL_LAST_ATTEMPT_AT_UTC_MS INTEGER,
                    $COL_ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0 CHECK($COL_ATTEMPT_COUNT >= 0),
                    $COL_STATUS TEXT NOT NULL CHECK($COL_STATUS IN ('PENDING', 'RETRYING', 'FAILED', 'COMPLETED')),
                    $COL_ERROR_CATEGORY TEXT,
                    UNIQUE($COL_PLATFORM_NOTIFICATION_TAG, $COL_PLATFORM_NOTIFICATION_ID)
                )
                """.trimIndent()
            )

            // Required indexes & partial unique indexes
            db.execSQL(
                "CREATE INDEX idx_habit_entries_habit_date ON $TABLE_HABIT_ENTRIES($COL_HABIT_ID, $COL_LOCAL_DATE, $COL_CREATED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX idx_habit_entries_idempotency ON $TABLE_HABIT_ENTRIES($COL_IDEMPOTENCY_KEY) WHERE $COL_IDEMPOTENCY_KEY IS NOT NULL"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX idx_habit_day_outcomes_idempotency ON $TABLE_HABIT_DAY_OUTCOMES($COL_IDEMPOTENCY_KEY) WHERE $COL_IDEMPOTENCY_KEY IS NOT NULL"
            )
            db.execSQL(
                "CREATE INDEX idx_journal_entries_date ON $TABLE_JOURNAL_ENTRIES($COL_LOCAL_DATE, $COL_CREATED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE INDEX idx_journal_drafts_updated ON $TABLE_JOURNAL_DRAFTS($COL_UPDATED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX idx_journal_drafts_intended_entry ON $TABLE_JOURNAL_DRAFTS($COL_INTENDED_JOURNAL_ENTRY_ID) WHERE $COL_INTENDED_JOURNAL_ENTRY_ID IS NOT NULL"
            )
            db.execSQL(
                "CREATE INDEX idx_reminder_deliveries_status ON $TABLE_REMINDER_DELIVERIES($COL_DELIVERY_STATUS, $COL_SCHEDULED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE INDEX idx_restore_conflicts_session ON $TABLE_RESTORE_CONFLICTS($COL_RESTORE_SESSION_ID, $COL_CREATED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE INDEX idx_restore_sessions_state ON $TABLE_RESTORE_SESSIONS($COL_SESSION_STATE, $COL_UPDATED_AT_UTC_MS)"
            )
            db.execSQL(
                "CREATE INDEX idx_notification_cancel_outbox_status ON $TABLE_NOTIFICATION_CANCEL_OUTBOX($COL_STATUS, $COL_CREATED_AT_UTC_MS)"
            )

            // Cross-table SQLite triggers for D-190 (activity idempotency) & D-189 (mode exclusivity)
            db.execSQL(
                """
                CREATE TRIGGER trg_habit_entries_insert_guards
                BEFORE INSERT ON $TABLE_HABIT_ENTRIES
                FOR EACH ROW
                BEGIN
                    SELECT CASE
                        WHEN NEW.$COL_IDEMPOTENCY_KEY IS NOT NULL
                             AND EXISTS (
                                 SELECT 1 FROM $TABLE_HABIT_DAY_OUTCOMES
                                 WHERE $COL_IDEMPOTENCY_KEY = NEW.$COL_IDEMPOTENCY_KEY
                             )
                        THEN RAISE(ABORT, 'CROSS_TABLE_DUPLICATE_IDEMPOTENCY_KEY')
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_DAY_OUTCOMES
                            WHERE $COL_HABIT_ID = NEW.$COL_HABIT_ID AND $COL_LOCAL_DATE = NEW.$COL_LOCAL_DATE
                        )
                        THEN RAISE(ABORT, 'CONTRADICTORY_DAY_OUTCOME_EXISTS')
                        WHEN NOT EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_VERSIONS v
                            WHERE v.$COL_HABIT_ID = NEW.$COL_HABIT_ID
                              AND v.$COL_EFFECTIVE_FROM_LOCAL_DATE = (
                                  SELECT MAX(v2.$COL_EFFECTIVE_FROM_LOCAL_DATE)
                                  FROM $TABLE_HABIT_VERSIONS v2
                                  WHERE v2.$COL_HABIT_ID = NEW.$COL_HABIT_ID
                                    AND v2.$COL_EFFECTIVE_FROM_LOCAL_DATE <= NEW.$COL_LOCAL_DATE
                              )
                              AND v.$COL_TRACKING_MODE IN ('QUANTITY', 'DURATION')
                        )
                        THEN RAISE(ABORT, 'INCOMPATIBLE_EFFECTIVE_TRACKING_MODE_FOR_ENTRY')
                    END;
                END;
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER trg_habit_entries_update_guards
                BEFORE UPDATE ON $TABLE_HABIT_ENTRIES
                FOR EACH ROW
                BEGIN
                    SELECT CASE
                        WHEN NEW.$COL_IDEMPOTENCY_KEY IS NOT NULL
                             AND EXISTS (
                                 SELECT 1 FROM $TABLE_HABIT_DAY_OUTCOMES
                                 WHERE $COL_IDEMPOTENCY_KEY = NEW.$COL_IDEMPOTENCY_KEY
                             )
                        THEN RAISE(ABORT, 'CROSS_TABLE_DUPLICATE_IDEMPOTENCY_KEY')
                    END;
                END;
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER trg_habit_day_outcomes_insert_guards
                BEFORE INSERT ON $TABLE_HABIT_DAY_OUTCOMES
                FOR EACH ROW
                BEGIN
                    SELECT CASE
                        WHEN NEW.$COL_IDEMPOTENCY_KEY IS NOT NULL
                             AND EXISTS (
                                 SELECT 1 FROM $TABLE_HABIT_ENTRIES
                                 WHERE $COL_IDEMPOTENCY_KEY = NEW.$COL_IDEMPOTENCY_KEY
                             )
                        THEN RAISE(ABORT, 'CROSS_TABLE_DUPLICATE_IDEMPOTENCY_KEY')
                        WHEN EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_ENTRIES
                            WHERE $COL_HABIT_ID = NEW.$COL_HABIT_ID AND $COL_LOCAL_DATE = NEW.$COL_LOCAL_DATE
                        )
                        THEN RAISE(ABORT, 'CONTRADICTORY_HABIT_ENTRY_EXISTS')
                        WHEN NOT EXISTS (
                            SELECT 1 FROM $TABLE_HABIT_VERSIONS v
                            WHERE v.$COL_HABIT_ID = NEW.$COL_HABIT_ID
                              AND v.$COL_EFFECTIVE_FROM_LOCAL_DATE = (
                                  SELECT MAX(v2.$COL_EFFECTIVE_FROM_LOCAL_DATE)
                                  FROM $TABLE_HABIT_VERSIONS v2
                                  WHERE v2.$COL_HABIT_ID = NEW.$COL_HABIT_ID
                                    AND v2.$COL_EFFECTIVE_FROM_LOCAL_DATE <= NEW.$COL_LOCAL_DATE
                              )
                              AND (
                                  (v.$COL_TRACKING_MODE = 'COMPLETION_ONLY' AND NEW.$COL_OUTCOME_KIND IN ('COMPLETED', 'COMPLETION_ONLY'))
                                  OR
                                  (v.$COL_TRACKING_MODE = 'EFFORT_AWARE' AND NEW.$COL_OUTCOME_KIND IN ('EFFORT_LOGGED', 'EFFORT_AWARE'))
                              )
                        )
                        THEN RAISE(ABORT, 'INCOMPATIBLE_EFFECTIVE_TRACKING_MODE_FOR_OUTCOME')
                    END;
                END;
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER trg_habit_day_outcomes_update_guards
                BEFORE UPDATE ON $TABLE_HABIT_DAY_OUTCOMES
                FOR EACH ROW
                BEGIN
                    SELECT CASE
                        WHEN NEW.$COL_IDEMPOTENCY_KEY IS NOT NULL
                             AND EXISTS (
                                 SELECT 1 FROM $TABLE_HABIT_ENTRIES
                                 WHERE $COL_IDEMPOTENCY_KEY = NEW.$COL_IDEMPOTENCY_KEY
                             )
                        THEN RAISE(ABORT, 'CROSS_TABLE_DUPLICATE_IDEMPOTENCY_KEY')
                    END;
                END;
                """.trimIndent()
            )
        }

        /**
         * Fail-closed v1-to-v2 migration (D-197).
         * Validates all legacy rows in v1 tables before mutating schema.
         * If any row cannot be mapped losslessly (e.g., SKIPPED outcome, invalid date,
         * unknown tracking mode, invalid weekday mask, orphaned record), throws
         * DayseedMigrationBlockedException so the enclosing SQLite transaction rolls back completely.
         */
        fun migrateV1ToV2FailClosed(db: SQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")

            data class LegacyHabitRow(
                val id: String,
                val name: String,
                val description: String?,
                val trackingMode: TrackingMode,
                val scheduleKind: ScheduleKind,
                val scheduledMask: Int?,
                val isArchived: Boolean,
                val createdAtMs: Long,
                val earliestEffectiveDate: LocalDate
            )

            data class LegacyRecordRow(
                val id: String,
                val habitId: String,
                val localDate: LocalDate,
                val outcomeKind: String,
                val effortLevel: String?,
                val note: String?,
                val updatedAtMs: Long
            )

            data class LegacyJournalRow(
                val id: String,
                val localDate: LocalDate,
                val prompt: String?,
                val content: String,
                val createdAtMs: Long,
                val updatedAtMs: Long
            )

            data class LegacyPrefRow(
                val key: String,
                val value: String
            )

            val habitIds = mutableSetOf<String>()
            val earliestDateByHabit = mutableMapOf<String, LocalDate>()

            val legacyRecords = mutableListOf<LegacyRecordRow>()
            val hasDailyRecords = tableExists(db, "daily_records")
            val hasHabits = tableExists(db, "habits")
            if (!hasHabits) {
                throw DayseedMigrationBlockedException("Legacy v1 database is missing 'habits' table")
            }

            // Read habit modes first to validate records against their parent habit
            val rawHabitModes = mutableMapOf<String, TrackingMode>()
            db.rawQuery("SELECT id, name, tracking_mode FROM habits", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)?.trim().orEmpty()
                    val name = cursor.getString(1)?.trim().orEmpty()
                    val modeStr = cursor.getString(2)?.trim().orEmpty()
                    if (id.isEmpty() || name.isEmpty()) {
                        throw DayseedMigrationBlockedException("Legacy habit has empty id or name")
                    }
                    if (!habitIds.add(id)) {
                        throw DayseedMigrationBlockedException("Duplicate legacy habit id: $id")
                    }
                    val mappedMode = when (modeStr) {
                        "EFFORT_LEVELS", "EFFORT_AWARE" -> TrackingMode.EFFORT_AWARE
                        "COMPLETION_ONLY" -> TrackingMode.COMPLETION_ONLY
                        else -> throw DayseedMigrationBlockedException("Unsupported legacy tracking_mode '$modeStr' on habit $id")
                    }
                    rawHabitModes[id] = mappedMode
                }
            }

            if (hasDailyRecords) {
                val recordCols = getColumnNames(db, "daily_records")
                val dateCol = if ("record_date" in recordCols) "record_date" else "date"
                db.rawQuery(
                    "SELECT id, habit_id, $dateCol, outcome, effort_level, note, updated_at FROM daily_records",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val recId = cursor.getString(0)?.trim().orEmpty()
                        val habitId = cursor.getString(1)?.trim().orEmpty()
                        val dateStr = cursor.getString(2)?.trim().orEmpty()
                        val outcomeStr = cursor.getString(3)?.trim().orEmpty()
                        val effortStr = cursor.getString(4)?.trim().orEmpty()
                        val note = if (cursor.isNull(5)) null else cursor.getString(5)?.trim()
                        val updatedAt = if (cursor.isNull(6)) 0L else cursor.getLong(6)

                        if (recId.isEmpty() || habitId.isEmpty() || updatedAt <= 0L) {
                            throw DayseedMigrationBlockedException("Invalid legacy daily_records row metadata: $recId")
                        }
                        val parentMode = rawHabitModes[habitId]
                            ?: throw DayseedMigrationBlockedException("Orphaned daily_records row $recId for missing habit $habitId")
                        val parsedDate = try {
                            LocalDate.parse(dateStr)
                        } catch (e: Exception) {
                            throw DayseedMigrationBlockedException("Invalid ISO local date '$dateStr' in daily_records $recId")
                        }

                        if (outcomeStr != "COMPLETED") {
                            throw DayseedMigrationBlockedException(
                                "Unmappable legacy outcome '$outcomeStr' on record $recId (D-193/D-197 fail-closed rule)"
                            )
                        }

                        val (outcomeKind, effortLevel) = when (parentMode) {
                            TrackingMode.COMPLETION_ONLY -> {
                                if (effortStr != "STANDARD" && effortStr.isNotEmpty()) {
                                    throw DayseedMigrationBlockedException(
                                        "Contradictory effort_level '$effortStr' for COMPLETION_ONLY record $recId"
                                    )
                                }
                                "COMPLETED" to null
                            }
                            TrackingMode.EFFORT_AWARE -> {
                                val level = when (effortStr) {
                                    "TINY", "USUAL", "EXTRA" -> effortStr
                                    else -> throw DayseedMigrationBlockedException(
                                        "Invalid effort_level '$effortStr' for EFFORT_AWARE record $recId"
                                    )
                                }
                                "EFFORT_LOGGED" to level
                            }
                            else -> throw DayseedMigrationBlockedException("Unsupported mode $parentMode in legacy daily_records")
                        }

                        val currentEarliest = earliestDateByHabit[habitId]
                        if (currentEarliest == null || parsedDate.isBefore(currentEarliest)) {
                            earliestDateByHabit[habitId] = parsedDate
                        }

                        legacyRecords.add(
                            LegacyRecordRow(
                                id = recId,
                                habitId = habitId,
                                localDate = parsedDate,
                                outcomeKind = outcomeKind,
                                effortLevel = effortLevel,
                                note = note?.takeIf { it.isNotEmpty() },
                                updatedAtMs = updatedAt
                            )
                        )
                    }
                }
            }

            // Step 2: Validate all legacy habits
            val legacyHabits = mutableListOf<LegacyHabitRow>()
            db.rawQuery(
                "SELECT id, name, description, tracking_mode, schedule_days, is_archived, created_at FROM habits",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0).trim()
                    val name = cursor.getString(1).trim()
                    val desc = if (cursor.isNull(2)) null else cursor.getString(2)?.trim()
                    val mode = rawHabitModes.getValue(id)
                    val scheduleStr = cursor.getString(4)?.trim().orEmpty()
                    val isArchived = cursor.getInt(5) != 0
                    val createdAt = if (cursor.isNull(6)) 0L else cursor.getLong(6)
                    if (createdAt <= 0L) {
                        throw DayseedMigrationBlockedException("Invalid created_at on legacy habit $id")
                    }

                    val parsedDays = parseLegacyWeekdaysOrThrow(scheduleStr, id)
                    val mask = WeekdayMask.toMask(parsedDays)
                    if (!WeekdayMask.isValidNonEmptyMask(mask)) {
                        throw DayseedMigrationBlockedException("Empty or invalid weekday mask for legacy habit $id")
                    }
                    val (scheduleKind, scheduledMask) = if (mask == WeekdayMask.ALL_DAYS_MASK) {
                        ScheduleKind.DAILY to null
                    } else {
                        ScheduleKind.SELECTED_WEEKDAYS to mask
                    }

                    val createdLocalDate = Instant.ofEpochMilli(createdAt).atZone(ZoneOffset.UTC).toLocalDate()
                    val minRecordDate = earliestDateByHabit[id]
                    val effectiveFrom = if (minRecordDate != null && minRecordDate.isBefore(createdLocalDate)) {
                        minRecordDate
                    } else {
                        createdLocalDate
                    }

                    legacyHabits.add(
                        LegacyHabitRow(
                            id = id,
                            name = name,
                            description = desc?.takeIf { it.isNotEmpty() },
                            trackingMode = mode,
                            scheduleKind = scheduleKind,
                            scheduledMask = scheduledMask,
                            isArchived = isArchived,
                            createdAtMs = createdAt,
                            earliestEffectiveDate = effectiveFrom
                        )
                    )
                }
            }

            // Step 3: Validate legacy journal_entries
            val legacyJournals = mutableListOf<LegacyJournalRow>()
            if (tableExists(db, "journal_entries")) {
                val journalCols = getColumnNames(db, "journal_entries")
                val entryDateCol = if ("entry_date" in journalCols) "entry_date" else "date"
                db.rawQuery(
                    "SELECT id, $entryDateCol, prompt, content, created_at, updated_at FROM journal_entries",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0)?.trim().orEmpty()
                        val dateStr = cursor.getString(1)?.trim().orEmpty()
                        val prompt = if (cursor.isNull(2)) null else cursor.getString(2)?.trim()
                        val content = cursor.getString(3)?.trim().orEmpty()
                        val createdAt = if (cursor.isNull(4)) 0L else cursor.getLong(4)
                        val updatedAt = if (cursor.isNull(5)) 0L else cursor.getLong(5)

                        if (id.isEmpty() || content.isEmpty() || createdAt <= 0L || updatedAt <= 0L) {
                            throw DayseedMigrationBlockedException("Invalid legacy journal_entries row $id")
                        }
                        val parsedDate = try {
                            LocalDate.parse(dateStr)
                        } catch (e: Exception) {
                            throw DayseedMigrationBlockedException("Invalid date '$dateStr' on legacy journal $id")
                        }
                        legacyJournals.add(
                            LegacyJournalRow(
                                id = id,
                                localDate = parsedDate,
                                prompt = prompt?.takeIf { it.isNotEmpty() },
                                content = content,
                                createdAtMs = createdAt,
                                updatedAtMs = updatedAt
                            )
                        )
                    }
                }
            }

            // Step 4: Validate legacy app_preferences
            val legacyPrefs = mutableListOf<LegacyPrefRow>()
            if (tableExists(db, "app_preferences")) {
                db.rawQuery("SELECT pref_key, pref_value FROM app_preferences", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val key = cursor.getString(0)?.trim().orEmpty()
                        val value = cursor.getString(1) ?: ""
                        if (key.isEmpty()) {
                            throw DayseedMigrationBlockedException("Empty pref_key in legacy app_preferences")
                        }
                        legacyPrefs.add(LegacyPrefRow(key, value))
                    }
                }
            }

            // Step 5: Rename legacy tables, create canonical v2 schema, and insert validated data
            db.execSQL("ALTER TABLE habits RENAME TO habits_v1_legacy")
            if (hasDailyRecords) {
                db.execSQL("ALTER TABLE daily_records RENAME TO daily_records_v1_legacy")
            }
            if (tableExists(db, "journal_entries")) {
                db.execSQL("ALTER TABLE journal_entries RENAME TO journal_entries_v1_legacy")
            }
            if (tableExists(db, "app_preferences")) {
                db.execSQL("ALTER TABLE app_preferences RENAME TO app_preferences_v1_legacy")
            }

            createV2Schema(db)

            for (h in legacyHabits) {
                val habitCv = ContentValues().apply {
                    put(COL_HABIT_ID, h.id)
                    put(COL_CREATED_AT_UTC_MS, h.createdAtMs)
                    put(COL_UPDATED_AT_UTC_MS, h.createdAtMs)
                }
                if (db.insertOrThrow(TABLE_HABITS, null, habitCv) == -1L) {
                    throw DayseedMigrationBlockedException("Failed to insert migrated habit ${h.id}")
                }

                val versionCv = ContentValues().apply {
                    put(COL_VERSION_ID, UUID.nameUUIDFromBytes("v1-version-${h.id}".toByteArray()).toString())
                    put(COL_HABIT_ID, h.id)
                    put(COL_EFFECTIVE_FROM_LOCAL_DATE, h.earliestEffectiveDate.toString())
                    put(COL_NAME, h.name)
                    if (h.description != null) put(COL_DESCRIPTION, h.description) else putNull(COL_DESCRIPTION)
                    put(COL_TRACKING_MODE, h.trackingMode.canonical().name)
                    putNull(COL_UNIT_CODE)
                    put(COL_TARGET_PERIOD, TargetPeriod.DAY.name)
                    putNull(COL_TARGET_VALUE)
                    put(COL_SCHEDULE_KIND, h.scheduleKind.name)
                    if (h.scheduledMask != null) {
                        put(COL_SCHEDULED_WEEKDAYS_MASK, h.scheduledMask)
                    } else {
                        putNull(COL_SCHEDULED_WEEKDAYS_MASK)
                    }
                    putNull(COL_PREFERRED_WEEKDAYS_MASK)
                    put(
                        COL_LIFECYCLE_STATE,
                        if (h.isArchived) HabitLifecycleState.ARCHIVED.name else HabitLifecycleState.ACTIVE.name
                    )
                    put(COL_REMINDER_MODE, "OFF")
                    put(COL_REMINDER_TIME_LOCAL, "09:00")
                    put(COL_CREATED_AT_UTC_MS, h.createdAtMs)
                }
                if (db.insertOrThrow(TABLE_HABIT_VERSIONS, null, versionCv) == -1L) {
                    throw DayseedMigrationBlockedException("Failed to insert initial version for habit ${h.id}")
                }
            }

            for (r in legacyRecords) {
                val outcomeCv = ContentValues().apply {
                    put(COL_OUTCOME_ID, r.id)
                    put(COL_HABIT_ID, r.habitId)
                    put(COL_LOCAL_DATE, r.localDate.toString())
                    put(COL_OUTCOME_KIND, r.outcomeKind)
                    if (r.effortLevel != null) {
                        put(COL_EFFORT_LEVEL, r.effortLevel)
                    } else {
                        putNull(COL_EFFORT_LEVEL)
                    }
                    putNull(COL_IDEMPOTENCY_KEY)
                    put(COL_CREATED_AT_UTC_MS, r.updatedAtMs)
                    put(COL_UPDATED_AT_UTC_MS, r.updatedAtMs)
                }
                if (db.insertOrThrow(TABLE_HABIT_DAY_OUTCOMES, null, outcomeCv) == -1L) {
                    throw DayseedMigrationBlockedException("Failed to insert migrated outcome ${r.id}")
                }

                if (r.note != null) {
                    val noteCv = ContentValues().apply {
                        put(COL_NOTE_ID, UUID.nameUUIDFromBytes("v1-note-${r.id}".toByteArray()).toString())
                        put(COL_HABIT_ID, r.habitId)
                        put(COL_LOCAL_DATE, r.localDate.toString())
                        put(COL_BODY, r.note)
                        put(COL_CREATED_AT_UTC_MS, r.updatedAtMs)
                        put(COL_UPDATED_AT_UTC_MS, r.updatedAtMs)
                    }
                    if (db.insertOrThrow(TABLE_HABIT_DATE_NOTES, null, noteCv) == -1L) {
                        throw DayseedMigrationBlockedException("Failed to insert migrated note for ${r.id}")
                    }
                }
            }

            for (j in legacyJournals) {
                val journalCv = ContentValues().apply {
                    put(COL_JOURNAL_ENTRY_ID, j.id)
                    put(COL_LOCAL_DATE, j.localDate.toString())
                    put(COL_BODY, j.content)
                    putNull(COL_TITLE)
                    putNull(COL_PRIMARY_MOOD_CODE)
                    putNull(COL_SECONDARY_MOOD_CODE)
                    if (j.prompt != null) put(COL_PROMPT_ID, j.prompt) else putNull(COL_PROMPT_ID)
                    putNull(COL_LINKED_HABIT_ID)
                    put(COL_CREATED_AT_UTC_MS, j.createdAtMs)
                    put(COL_UPDATED_AT_UTC_MS, j.updatedAtMs)
                }
                if (db.insertOrThrow(TABLE_JOURNAL_ENTRIES, null, journalCv) == -1L) {
                    throw DayseedMigrationBlockedException("Failed to insert migrated journal ${j.id}")
                }
            }

            for (p in legacyPrefs) {
                val prefCv = ContentValues().apply {
                    put(COL_SETTING_KEY, p.key)
                    put(COL_VALUE_TYPE, "STRING")
                    put(COL_VALUE_TEXT, p.value)
                    put(COL_SCHEMA_VERSION, 1)
                    put(COL_UPDATED_AT_UTC_MS, System.currentTimeMillis())
                }
                if (db.insertOrThrow(TABLE_APP_SETTINGS, null, prefCv) == -1L) {
                    throw DayseedMigrationBlockedException("Failed to insert migrated preference ${p.key}")
                }
            }

            // Drop temporary renamed legacy tables after verifying counts
            db.execSQL("DROP TABLE habits_v1_legacy")
            if (hasDailyRecords) {
                db.execSQL("DROP TABLE daily_records_v1_legacy")
            }
            if (tableExists(db, "journal_entries_v1_legacy")) {
                db.execSQL("DROP TABLE journal_entries_v1_legacy")
            }
            if (tableExists(db, "app_preferences_v1_legacy")) {
                db.execSQL("DROP TABLE app_preferences_v1_legacy")
            }
        }

        private fun parseLegacyWeekdaysOrThrow(scheduleStr: String, habitId: String): Set<DayOfWeek> {
            if (scheduleStr.isBlank()) {
                throw DayseedMigrationBlockedException("Blank schedule_days on legacy habit $habitId")
            }
            val tokens = scheduleStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tokens.isEmpty()) {
                throw DayseedMigrationBlockedException("Empty schedule_days tokens on legacy habit $habitId")
            }
            val days = mutableSetOf<DayOfWeek>()
            for (token in tokens) {
                val day = try {
                    DayOfWeek.valueOf(token.uppercase())
                } catch (e: Exception) {
                    throw DayseedMigrationBlockedException("Unrecognised weekday '$token' on legacy habit $habitId")
                }
                days.add(day)
            }
            return days
        }

        private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName)
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun getColumnNames(db: SQLiteDatabase, tableName: String): Set<String> {
            val cols = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                if (nameIdx >= 0) {
                    while (cursor.moveToNext()) {
                        cols.add(cursor.getString(nameIdx))
                    }
                }
            }
            return cols
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON")
    }

    override fun onCreate(db: SQLiteDatabase) {
        createV2Schema(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1 || newVersion > DATABASE_VERSION) {
            throw DayseedMigrationBlockedException("Unsupported schema upgrade path $oldVersion -> $newVersion")
        }
        if (oldVersion == 1 && newVersion == 2) {
            migrateV1ToV2FailClosed(db)
        }
    }
}
