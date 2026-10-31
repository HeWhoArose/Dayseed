package com.example.dayseed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.ui.components.SettingsGroupCard
import com.example.dayseed.ui.components.SettingsRow
import com.example.dayseed.ui.components.SettingsRowDivider
import com.example.dayseed.ui.components.SettingsSectionLabel
import com.example.dayseed.ui.theme.ThemeModeOption
import java.time.DayOfWeek

/**
 * Settings hub (restructure, owner instruction "A", 2026-10-31).
 *
 * A short list of buttons in the Material settings pattern: every row is the button, its
 * secondary text shows the current state, and pressing it makes the corresponding settings
 * appear — as a bottom sheet for short groups, or the backup/restore sub-screen for the
 * heavy group. Internal vocabulary (decision IDs, schema/table counts) is kept out of the
 * UI; destructive deletion stays isolated at the bottom of the hub.
 *
 * All pre-existing test tags are preserved: settings_screen, theme_chip_*, week_start_*,
 * export_journal_*_btn, backup_password_input, export_data_btn, open_restore_dialog_btn,
 * restore_json_input, confirm_restore_btn, delete_all_data_btn, perm_delete_habit_*,
 * conflict_card_*, resolve_*.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val inactiveHabits = remember(habits) {
        habits.filter { it.lifecycleState != HabitLifecycleState.ACTIVE }
    }
    val currentThemeMode = remember(appSettings) {
        ThemeModeOption.fromSetting(appSettings["theme_mode"]?.valueText)
    }
    val currentWeekStart = remember(appSettings) {
        if (appSettings["week_start"]?.valueText?.equals("SUNDAY", ignoreCase = true) == true) {
            DayOfWeek.SUNDAY
        } else {
            DayOfWeek.MONDAY
        }
    }

    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var backupOpen by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportedTextPreview by remember { mutableStateOf<String?>(null) }
    var exportedTitle by remember { mutableStateOf("") }

    // The backup sub-screen is a state inside Settings, so system back returns to the hub.
    BackHandler(enabled = backupOpen) { backupOpen = false }

    if (backupOpen) {
        BackupRestorePanel(
            viewModel = viewModel,
            backupPassword = backupPassword,
            onBackupPasswordChange = { backupPassword = it },
            onPreview = { title, text ->
                exportedTitle = title
                exportedTextPreview = text
            },
            onBack = { backupOpen = false }
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You own your data. It stays on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SettingsSectionLabel("Everyday") }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "Appearance",
                        summary = currentThemeMode.label,
                        icon = Icons.Default.Palette,
                        onClick = { activeSheet = SettingsSheet.APPEARANCE },
                        testTag = "settings_row_appearance"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Week starts",
                        summary = SettingsSupport.weekStartSummary(currentWeekStart),
                        icon = Icons.Default.Today,
                        onClick = { activeSheet = SettingsSheet.WEEK_START },
                        testTag = "settings_row_week_start"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Quiet hours",
                        summary = SettingsSupport.quietHoursSummary(
                            appSettings["quiet_hours_start"]?.valueText,
                            appSettings["quiet_hours_end"]?.valueText
                        ),
                        icon = Icons.Default.Bedtime,
                        onClick = { activeSheet = SettingsSheet.QUIET_HOURS },
                        testTag = "settings_row_quiet_hours"
                    )
                }
            }

            item { SettingsSectionLabel("Your data") }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "Read a copy of your journal",
                        summary = "Markdown, CSV or JSON · saved entries only",
                        icon = Icons.Default.Description,
                        onClick = { activeSheet = SettingsSheet.JOURNAL_EXPORT },
                        testTag = "settings_row_journal_export"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Back up or restore",
                        summary = "Optional password · safety backup before restoring",
                        icon = Icons.Default.Backup,
                        onClick = { backupOpen = true },
                        testTag = "settings_row_backup"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "How Dayseed protects your privacy",
                        summary = "Local-first · platform backup off",
                        icon = Icons.Default.Lock,
                        onClick = { activeSheet = SettingsSheet.PRIVACY },
                        testTag = "settings_row_privacy"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Removed habits",
                        summary = SettingsSupport.removedHabitsSummary(inactiveHabits.size),
                        icon = Icons.Default.Archive,
                        onClick = { activeSheet = SettingsSheet.REMOVED_HABITS },
                        testTag = "settings_row_removed"
                    )
                }
            }

            item { SettingsSectionLabel("About") }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "About Dayseed",
                        summary = "Version 1.0.0",
                        icon = Icons.Default.Info,
                        onClick = { activeSheet = SettingsSheet.ABOUT },
                        testTag = "settings_row_about"
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_all_data_btn")
                    ) {
                        Text("Delete all personal data")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Removes everything from this device. Copies you made earlier " +
                            "may still exist outside the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = { activeSheet = null }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                when (sheet) {
                    SettingsSheet.APPEARANCE ->
                        AppearancePanel(viewModel, currentThemeMode)
                    SettingsSheet.WEEK_START ->
                        WeekStartPanel(viewModel, currentWeekStart)
                    SettingsSheet.QUIET_HOURS ->
                        QuietHoursPanel(
                            viewModel = viewModel,
                            initialStart = appSettings["quiet_hours_start"]?.valueText ?: "21:00",
                            initialEnd = appSettings["quiet_hours_end"]?.valueText ?: "08:00",
                            onSaved = { activeSheet = null }
                        )
                    SettingsSheet.JOURNAL_EXPORT ->
                        JournalExportPanel(
                            viewModel = viewModel,
                            onPreview = { title, text ->
                                exportedTitle = title
                                exportedTextPreview = text
                                activeSheet = null
                            }
                        )
                    SettingsSheet.PRIVACY -> PrivacyPanel()
                    SettingsSheet.REMOVED_HABITS ->
                        RemovedHabitsPanel(viewModel, inactiveHabits)
                    SettingsSheet.ABOUT -> AboutPanel()
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete all data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes everything Dayseed keeps on this device: habits and their " +
                        "history, tracking records and notes, journal entries and drafts, " +
                        "preferences, and unfinished restore work.\n\n" +
                        "Files you exported, copies saved elsewhere, or Android-held backups " +
                        "may remain — those live outside the app and are not deleted. This " +
                        "cannot be undone."
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_all_btn")
                ) {
                    Text("Delete everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (exportedTextPreview != null) {
        AlertDialog(
            onDismissRequest = { exportedTextPreview = null },
            title = { Text(exportedTitle, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = exportedTextPreview!!.take(600) + if (exportedTextPreview!!.length > 600) "\n..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { exportedTextPreview = null }) {
                    Text("Close")
                }
            }
        )
    }
}
