package com.example.dayseed.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalExportFormat
import com.example.dayseed.ui.components.DaypartArtwork
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedArtwork
import kotlinx.coroutines.launch

/**
 * Settings panel contents (restructure, 2026-10-31). Each panel is the "corresponding
 * settings appear" surface behind one hub row and is intentionally small and focused:
 * short groups are bottom sheets, backup/restore is a full sub-screen because it carries
 * dialogs, safety sequencing and the conflict matrix. All pre-existing test tags are kept.
 */
enum class SettingsSheet {
    APPEARANCE, WEEK_START, QUIET_HOURS, JOURNAL_EXPORT, PRIVACY, REMOVED_HABITS, ABOUT
}

@Composable
private fun PanelTitle(title: String, supporting: String? = null) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePanel(
    viewModel: DayseedViewModel,
    currentThemeMode: ThemeModeOption
) {
    val isSystemDark = isSystemInDarkTheme()
    val nowLocal = remember { viewModel.repository.clock.nowLocalTime() }
    val previewArtwork = remember(currentThemeMode, isSystemDark, nowLocal) {
        resolveDayseedArtwork(currentThemeMode, isSystemDark, nowLocal)
    }
    val daypart = remember(currentThemeMode, nowLocal) {
        when (currentThemeMode) {
            ThemeModeOption.SOFT_BOTANICAL_AUTO -> SoftBotanicalThemeConfig.resolveDaypart(nowLocal)
            ThemeModeOption.LIGHT -> null
            ThemeModeOption.DARK -> BotanicalDaypart.NIGHT
            ThemeModeOption.SYSTEM -> if (isSystemDark) BotanicalDaypart.NIGHT else null
        }
    }

    PanelTitle(
        "Appearance",
        "How Dayseed looks. The time-of-day palette follows your device clock automatically."
    )
    Spacer(modifier = Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (mode in ThemeModeOption.entries) {
            FilterChip(
                selected = currentThemeMode == mode,
                onClick = { viewModel.setThemeMode(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("theme_chip_${mode.settingValue.lowercase()}")
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        DaypartArtwork(previewArtwork, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Right now you would see:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = daypart?.displayName ?: currentThemeMode.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun WeekStartPanel(
    viewModel: DayseedViewModel,
    currentWeekStart: java.time.DayOfWeek
) {
    PanelTitle("Week starts", "Which day the Insights week begins on.")
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = currentWeekStart == java.time.DayOfWeek.MONDAY,
            onClick = { viewModel.setWeekStart(java.time.DayOfWeek.MONDAY) },
            label = { Text("Monday") },
            modifier = Modifier.testTag("week_start_monday_chip")
        )
        FilterChip(
            selected = currentWeekStart == java.time.DayOfWeek.SUNDAY,
            onClick = { viewModel.setWeekStart(java.time.DayOfWeek.SUNDAY) },
            label = { Text("Sunday") },
            modifier = Modifier.testTag("week_start_sunday_chip")
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuietHoursPanel(
    viewModel: DayseedViewModel,
    initialStart: String,
    initialEnd: String,
    onSaved: () -> Unit
) {
    var startText by remember { mutableStateOf(initialStart) }
    var endText by remember { mutableStateOf(initialEnd) }
    val startError = SettingsSupport.quietHoursError(startText)
    val endError = SettingsSupport.quietHoursError(endText)
    val valid = startError == null && endError == null

    PanelTitle(
        "Quiet hours",
        "Reminders pause during this window. At most 3 reminders are sent per day."
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = startText,
            onValueChange = { startText = it },
            label = { Text("Start") },
            supportingText = { startError?.let { Text(it) } },
            isError = startError != null,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("quiet_hours_start_input")
        )
        OutlinedTextField(
            value = endText,
            onValueChange = { endText = it },
            label = { Text("End") },
            supportingText = { endError?.let { Text(it) } },
            isError = endError != null,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("quiet_hours_end_input")
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            val start = SettingsSupport.parseQuietHours(startText)
            val end = SettingsSupport.parseQuietHours(endText)
            if (start != null && end != null) {
                viewModel.setQuietHours(
                    SettingsSupport.formatQuietHours(start),
                    SettingsSupport.formatQuietHours(end)
                )
                onSaved()
            }
        },
        enabled = valid,
        modifier = Modifier.testTag("quiet_hours_save_btn")
    ) {
        Text("Save")
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalExportPanel(
    viewModel: DayseedViewModel,
    onPreview: (title: String, text: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    PanelTitle(
        "Read a copy of your journal",
        "Saved journal entries only — drafts and habit data are not included."
    )
    Spacer(modifier = Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview("Journal copy (Markdown)", viewModel.repository.exportReadableJournals(JournalExportFormat.MARKDOWN))
                }
            },
            modifier = Modifier
                .testTag("export_journal_md_btn")
        ) { Text("Markdown") }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview("Journal copy (CSV)", viewModel.repository.exportReadableJournals(JournalExportFormat.CSV))
                }
            },
            modifier = Modifier
                .testTag("export_journal_csv_btn")
        ) { Text("CSV") }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview("Journal copy (JSON)", viewModel.repository.exportReadableJournals(JournalExportFormat.JSON))
                }
            },
            modifier = Modifier
                .testTag("export_journal_json_btn")
        ) { Text("JSON") }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun PrivacyPanel() {
    PanelTitle("How Dayseed protects your privacy")
    Spacer(modifier = Modifier.height(10.dp))
    PrivacyParagraph(
        "On this device only. Your habits, tracking history and journal text are stored in a " +
            "local database and are never sent to a server. There is no account and no tracking."
    )
    PrivacyParagraph(
        "Android's own backup won't carry your records. Dayseed asks Android to exclude its " +
            "data from platform backups and device transfer. To move to a new phone, create a " +
            "portable backup file first and restore it there."
    )
    PrivacyParagraph(
        "Copies you make are yours to manage. Exported journal copies and backup files live " +
            "wherever you save or send them, outside the app — delete them there when you no " +
            "longer need them."
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun PrivacyParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun RemovedHabitsPanel(
    viewModel: DayseedViewModel,
    inactiveHabits: List<Habit>
) {
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }

    PanelTitle(
        "Removed habits",
        "Paused and archived habits keep their history without cluttering today's list."
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (inactiveHabits.isEmpty()) {
        Text(
            text = "No paused or archived habits right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (h in inactiveHabits) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = h.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = h.lifecycleState.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.updateHabitLifecycle(h.habitId, HabitLifecycleState.ACTIVE) },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("restore_habit_${h.habitId}")
                    ) { Text("Restore", style = MaterialTheme.typography.labelMedium) }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { pendingDelete = h },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("perm_delete_habit_${h.habitId}")
                    ) { Text("Delete", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }

    pendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this habit permanently?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes \"${habit.name}\" with its setup, tracking history and " +
                        "notes. Journal entries you wrote are kept. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(habit.habitId)
                        pendingDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_perm_delete_btn")
                ) { Text("Delete habit") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun AboutPanel() {
    PanelTitle("About Dayseed")
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Dayseed 1.0.0",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
    Text(
        text = "Your progress is more than your streak.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
}

/**
 * Backup & restore sub-screen. Reached from the hub's "Back up or restore" row; it keeps the
 * documented sequencing: optional password, verified export, restore with pre-restore safety
 * backup, and inline resolution of merge conflicts when they exist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupRestorePanel(
    viewModel: DayseedViewModel,
    backupPassword: String,
    onBackupPasswordChange: (String) -> Unit,
    onPreview: (title: String, text: String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val restoreConflicts by viewModel.restoreConflicts.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreMode by remember { mutableStateOf("MERGE") }
    var restoreError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("backup_panel_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Settings"
                )
            }
            Text(
                text = "Back up or restore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Backup ---
        PanelTitle(
            "Back up",
            "One file holding everything: habits, entries, reflections and settings, with an " +
                "optional password. A forgotten password means the file cannot be opened."
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = backupPassword,
            onValueChange = onBackupPasswordChange,
            label = { Text("Optional password") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_password_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val json = viewModel.repository.exportDataAsJson(
                            password = backupPassword.takeIf { it.isNotBlank() }
                        )
                        onPreview("Portable backup", json)
                        val clipboard = context.getSystemService(
                            android.content.Context.CLIPBOARD_SERVICE
                        ) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("Dayseed Backup", json)
                        )
                    }
                },
                modifier = Modifier.testTag("export_data_btn")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val json = viewModel.repository.exportDataAsJson(
                            password = backupPassword.takeIf { it.isNotBlank() }
                        )
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, json)
                            type = "application/json"
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(sendIntent, "Share backup")
                        )
                    }
                },
                modifier = Modifier.testTag("share_backup_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Restore ---
        PanelTitle(
            "Restore",
            "Before importing, Dayseed creates and verifies an automatic safety backup."
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                restoreError = null
                showRestoreDialog = true
            },
            modifier = Modifier.testTag("open_restore_dialog_btn")
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restore from a file")
        }

        // --- Merge conflicts (D-202) ---
        if (restoreConflicts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Choices needed while merging (${restoreConflicts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            for (conflict in restoreConflicts) {
                val allowed = conflict.allowedResolutions()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("conflict_card_${conflict.conflictId}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${conflict.recordType} • ${conflict.recordKey}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.resolveConflict(conflict.conflictId, ConflictResolution.KEEP_CURRENT)
                                },
                                modifier = Modifier.testTag("resolve_keep_current_${conflict.conflictId}")
                            ) { Text("Keep current", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = {
                                    viewModel.resolveConflict(conflict.conflictId, ConflictResolution.USE_BACKUP)
                                },
                                modifier = Modifier.testTag("resolve_use_backup_${conflict.conflictId}")
                            ) { Text("Use backup", style = MaterialTheme.typography.labelSmall) }
                            if (ConflictResolution.KEEP_BOTH in allowed) {
                                Button(
                                    onClick = {
                                        viewModel.resolveConflict(conflict.conflictId, ConflictResolution.KEEP_BOTH)
                                    },
                                    modifier = Modifier.testTag("resolve_keep_both_${conflict.conflictId}")
                                ) { Text("Keep both", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore a backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "An automatic safety backup is created and verified before anything is imported.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = restoreMode == "MERGE",
                            onClick = { restoreMode = "MERGE" },
                            label = { Text("Merge") }
                        )
                        FilterChip(
                            selected = restoreMode == "REPLACE",
                            onClick = { restoreMode = "REPLACE" },
                            label = { Text("Replace all") }
                        )
                    }
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Paste backup file contents") },
                        maxLines = 5,
                        supportingText = { restoreError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        isError = restoreError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            try {
                                viewModel.repository.restorePortableBackupSync(
                                    rawBackupPayload = restoreJsonInput.trim(),
                                    restoreMode = restoreMode,
                                    password = backupPassword.takeIf { it.isNotBlank() }
                                )
                                showRestoreDialog = false
                                restoreJsonInput = ""
                            } catch (_: Exception) {
                                restoreError = "That didn't restore. Check the file contents" +
                                    (if (backupPassword.isNotBlank()) " and the password" else "") + "."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }
}
