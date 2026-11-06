# docs/40 — Restore Recovery After Successful Restore Investigation Report

**Date:** 2026-11-05  
**Author:** Dayseed remediation agent (per mission §5)  
**Scope:** Investigate whether the existing safety backup can support user-initiated recovery after a successful restore. Do NOT implement a one-tap revert.

---

## 1. Current Safety Backup Mechanism

### 1.1 Creation (restorePortableBackupSync lines 3426–3431)

```kotlin
val nowMs = clock.nowUtcMs()
val safetyBackupJson = exportPortableBackupSync(password = null)
val safetyFile = File(appContext.filesDir, "pre_restore_safety_$nowMs.dayseed.json")
safetyFile.writeText(safetyBackupJson)
val verifiedSafety = safetyFile.exists() && JSONObject(safetyFile.readText()).optString("backup_kind") == "dayseed_portable_backup"
check(verifiedSafety) { "Failed to verify pre-restore safety backup" }
```

- **Location:** Internal `filesDir` (private app storage, not user-accessible via SAF)
- **Naming:** `pre_restore_safety_<nowMs>.dayseed.json` (timestamped, unique per restore)
- **Content:** Full portable backup export of **current state before restore** (unencrypted, `password = null`)
- **Verification:** Immediately read back and validated as a valid Dayseed portable backup

### 1.2 Persistence in Restore Session (lines 3451–3452, 3504–3505)

```kotlin
put("safety_backup_ref", safetyFile.absolutePath)
put("safety_backup_verified", 1)
```

The `restore_sessions` table records:
- `safety_backup_ref` — absolute path to the safety file
- `safety_backup_verified` — 1 if verification passed

### 1.3 Cleanup Policy (line 4752)

```kotlin
if (file.name.endsWith(".dayseed.json") || file.name.startsWith("pre_restore_safety_")) {
```

The `cleanupExportFiles` function retains files matching these patterns. **Safety backups are NOT auto-deleted** by the current cleanup logic.

---

## 2. Can the Safety Backup Be Restored?

### 2.1 Technical Feasibility

| Aspect | Status |
|--------|--------|
| **Format** | Valid portable backup (v3, schema 2/3) — same `restorePortableBackupSync` can consume it |
| **Encryption** | Exported **unencrypted** (`password = null` at line 3427), so no password needed to restore |
| **Schema compatibility** | Same format_version and source_schema_version checks apply; safety backup matches current app version |
| **Path accessibility** | File is in `filesDir` — app can read it; user cannot pick it via SAF (internal storage) |
| **Verification** | Already verified at creation (`safety_backup_verified = 1`) |

### 2.2 Restoration Path

```kotlin
// To restore from safety backup:
val safetyJson = File(safetyBackupRef).readText()
restorePortableBackupSync(safetyJson, restoreMode = "REPLACE", password = null)
```

This would:
1. Create a **new** safety backup of the *current* (post-restore) state
2. Replace all data with the pre-restore state
3. Record a new `restore_session` with the new safety file

### 2.3 What Is Restored

Everything in the portable backup envelope:
- Habits, versions, entries, outcomes, exceptions, notes, streak runs
- Journal entries and drafts
- Week start history
- App settings (including `reminders_enabled`, quiet hours, daily cap, etc.)

---

## 3. Retention and Availability

| Factor | Detail |
|--------|--------|
| **Storage location** | `context.filesDir/pre_restore_safety_<timestamp>.dayseed.json` |
| **Auto-deletion** | **None** — `cleanupExportFiles` explicitly keeps `pre_restore_safety_*` files |
| **User visibility** | None — internal storage, not exposed in SAF picker |
| **Persistence across app updates** | Yes, `filesDir` survives updates (cleared only on uninstall or "Clear Data") |
| **Multiple restores** | Each restore creates a new timestamped safety file; all retained |

---

## 4. Risks of a One-Tap Revert

| Risk | Assessment |
|------|------------|
| **Overwrites valid newer data** | Yes — REPLACE mode deletes all current data. User changes *after* the restore (new entries, habit edits, journal writes) would be lost. |
| **Encryption mismatch** | Safety backup is unencrypted. If the user's current data was password-protected, the safety backup contains plaintext. Restoring it leaves data unencrypted unless the user re-applies a password. |
| **Settings regression** | `reminders_enabled`, quiet hours, daily cap, week start, theme, etc. all revert to pre-restore values. |
| **Notification schedule** | Reminder alarms are rescheduled from restored habits (via `rescheduleReminderAlarms` hook). Pending alarms for deleted habits would be cancelled by next reconciliation. |
| **Conflict with MERGE restore** | If original restore was MERGE with unresolved conflicts, reverting to pre-restore state loses the conflict resolution work. |
| **No partial revert** | All-or-nothing; cannot selectively undo habit data but keep journal entries added after restore. |

---

## 5. Required Additions for Safe User-Initiated Recovery

If a one-tap revert were to be added, these would be needed:

1. **UI entry point** — "Undo restore" action in restore confirmation dialog or Settings, visible only when a verified safety backup exists
2. **Confirmation dialog** — Explicit warning: "This will discard all changes made since the restore, including new entries, habit edits, and settings changes. Cannot be undone."
3. **Post-revert safety backup** — The revert itself must create a new safety backup (of the post-restore state) so the user can "re-redo" if they change their mind
4. **Encryption handling** — Decision: restore safety backup as unencrypted (current behaviour) or require password to re-encrypt?
5. **Selective revert option** — Advanced: allow user to choose which tables to revert (habits only, journal only, etc.)

---

## 6. Conclusion

**The existing safety backup *technically* supports restoration**, but:

- It is **not user-accessible** (internal `filesDir`, no SAF exposure)
- It captures a **complete pre-restore snapshot** (unencrypted, verified)
- It is **retained indefinitely** unless the user clears app data or uninstalls
- A **one-tap revert would overwrite all post-restore user data** — a significant data-loss risk that requires explicit, unmistakable confirmation and ideally a "re-redo" safety net

**No code change is required or recommended in this remediation.** The investigation confirms the safety backup exists and is viable for recovery, but a one-tap revert feature requires a separate scoped design addressing the risks above.

---

## 7. Traceability Links

- `docs/28-REQUIREMENT-TRACEABILITY.md` — no requirement currently maps to post-restore undo
- `docs/12-PROGRESS-AND-HANDOVER.md` — record investigation complete, feature deferred
- `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` — register if owner later batches a revert feature