package com.example.dayseed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.NavTab
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.ui.components.SettingsGroupCard
import com.example.dayseed.ui.components.SettingsRow
import com.example.dayseed.ui.components.SettingsRowDivider
import com.example.dayseed.ui.components.SettingsSectionLabel
import com.example.dayseed.ui.theme.ThemeModeOption
import java.time.DayOfWeek

/**
 * Four-section Soft Botanical Settings Hub (D-212 / D-213).
 *
 * Organized into four intuitive groups:
 * 1. Appearance & rhythm — inline visual theme cards + inline Monday/Sunday week-start toggle.
 * 2. Habits & gentle prompts — Quiet hours & reminder presets/steppers + Removed (paused/archived) habits.
 * 3. Data, backup & journal copy — unified Data sub-screen with on-device summary, masked password
 *    backup, restore payload inspection, and readable journal export.
 * 4. Trust & about — local-first privacy guarantees, app info, and a clearly framed Reset card.
 *
 * All pre-existing test tags are preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DayseedViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val activeHabitsCount = remember(habits) {
        habits.count { it.lifecycleState == HabitLifecycleState.ACTIVE }
    }
    val inactiveHabits = remember(habits) {
        habits.filter { it.lifecycleState != HabitLifecycleState.ACTIVE }
    }
    val totalCheckIns = remember(habitEntries, habitDayOutcomes) {
        habitEntries.size + habitDayOutcomes.size
    }
    val dataOverviewText = remember(activeHabitsCount, inactiveHabits.size, totalCheckIns, journalEntries.size) {
        SettingsSupport.dataSummaryLine(
            activeHabits = activeHabitsCount,
            pausedHabits = inactiveHabits.size,
            checkInCount = totalCheckIns,
            journalEntryCount = journalEntries.size
        )
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
    var dataSubScreenFocus by remember { mutableStateOf<DataSubScreenSection?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportedTextPreview by remember { mutableStateOf<String?>(null) }
    var exportedTitle by remember { mutableStateOf("") }

    // System back returns from the unified Data & Backup sub-screen to the main Settings hub.
    BackHandler(enabled = dataSubScreenFocus != null) {
        dataSubScreenFocus = null
    }

    if (dataSubScreenFocus != null) {
        BackupRestorePanel(
            viewModel = viewModel,
            backupPassword = backupPassword,
            onBackupPasswordChange = { backupPassword = it },
            onPreview = { title, text ->
                exportedTitle = title
                exportedTextPreview = text
            },
            onBack = { dataSubScreenFocus = null },
            initialFocus = dataSubScreenFocus ?: DataSubScreenSection.ALL
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Calm preferences and local data controls. Everything stays on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECTION 1: Appearance & rhythm (Inline theme cards + inline week toggle)
            item {
                SettingsSectionLabel(
                    text = "Appearance & rhythm",
                    supporting = "Botanical theme atmosphere and weekly Insights calendar start."
                )
            }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "Appearance",
                        summary = currentThemeMode.label,
                        icon = Icons.Default.Palette,
                        showChevron = false,
                        onClick = { activeSheet = SettingsSheet.APPEARANCE },
                        testTag = "settings_row_appearance"
                    )
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 14.dp
                        )
                    ) {
                        InlineAppearanceSelector(
                            viewModel = viewModel,
                            currentThemeMode = currentThemeMode
                        )
                    }

                    SettingsRowDivider()

                    SettingsRow(
                        label = "Week starts",
                        summary = "Insights & weekly goals begin on ${SettingsSupport.weekStartSummary(currentWeekStart)}",
                        icon = Icons.Default.Today,
                        showChevron = false,
                        onClick = { activeSheet = SettingsSheet.WEEK_START },
                        testTag = "settings_row_week_start"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 66.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InlineWeekStartSelector(
                            viewModel = viewModel,
                            currentWeekStart = currentWeekStart
                        )
                    }
                }
            }

            // SECTION 2: Habits & gentle prompts (Quiet hours + Removed habits)
            item {
                SettingsSectionLabel(
                    text = "Habits & gentle prompts",
                    supporting = "Notification quiet hours and paused or archived habits."
                )
            }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "Notifications & reminders",
                        summary = "Delivery not active yet (pending device verification) · " +
                            SettingsSupport.quietHoursSummary(
                                appSettings["quiet_hours_start"]?.valueText,
                                appSettings["quiet_hours_end"]?.valueText
                            ),
                        icon = Icons.Default.NotificationsNone,
                        onClick = { activeSheet = SettingsSheet.REMINDERS },
                        testTag = "settings_row_reminders"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Quiet hours & reminders",
                        summary = SettingsSupport.quietHoursSummary(
                            appSettings["quiet_hours_start"]?.valueText,
                            appSettings["quiet_hours_end"]?.valueText
                        ),
                        icon = Icons.Default.Bedtime,
                        badgeText = "Max 3/day",
                        onClick = { activeSheet = SettingsSheet.QUIET_HOURS },
                        testTag = "settings_row_quiet_hours"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Removed habits",
                        summary = SettingsSupport.removedHabitsSummary(inactiveHabits.size),
                        icon = Icons.Default.Archive,
                        badgeText = if (inactiveHabits.isNotEmpty()) "${inactiveHabits.size}" else null,
                        onClick = { activeSheet = SettingsSheet.REMOVED_HABITS },
                        testTag = "settings_row_removed"
                    )
                }
            }

            // SECTION 3: Data, backup & journal copy (Unified Data sub-screen)
            item {
                SettingsSectionLabel(
                    text = "Data, backup & journal copy",
                    supporting = dataOverviewText
                )
            }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "Back up or restore",
                        summary = "Portable backup with optional password · safety backup before restore",
                        icon = Icons.Default.Backup,
                        onClick = { dataSubScreenFocus = DataSubScreenSection.BACKUP_FOCUS },
                        testTag = "settings_row_backup"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "Read a copy of your journal",
                        summary = "Export or share saved entries as Markdown, CSV or JSON",
                        icon = Icons.Default.Description,
                        onClick = { dataSubScreenFocus = DataSubScreenSection.JOURNAL_FOCUS },
                        testTag = "settings_row_journal_export"
                    )
                }
            }

            // SECTION 4: Trust & about
            item {
                SettingsSectionLabel(
                    text = "Trust & about",
                    supporting = "How Dayseed keeps your records private and offline."
                )
            }
            item {
                SettingsGroupCard {
                    SettingsRow(
                        label = "How Dayseed protects your privacy",
                        summary = "On-device SQLite only · no accounts · platform cloud backup off",
                        icon = Icons.Default.Lock,
                        onClick = { activeSheet = SettingsSheet.PRIVACY },
                        testTag = "settings_row_privacy"
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        label = "About Dayseed",
                        summary = "Version 1.0.0 · Your progress is more than your streak",
                        icon = Icons.Default.Info,
                        onClick = { activeSheet = SettingsSheet.ABOUT },
                        testTag = "settings_row_about"
                    )
                }
            }

            // Framed Reset Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsGroupCard(
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reset on-device data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Removes all habits, check-ins, and journal reflections from this device. " +
                                "Export a portable backup above first if you might want them later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    SettingsSheet.REMINDERS ->
                        RemindersPanel(
                            viewModel = viewModel,
                            onOpenQuietHours = { activeSheet = SettingsSheet.QUIET_HOURS },
                            onNavigateToHabitEditors = {
                                activeSheet = null
                                viewModel.selectTab(NavTab.HABITS)
                            }
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
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { exportedTextPreview = null },
            title = { Text(exportedTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = exportedTextPreview!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, exportedTextPreview)
                                type = "text/plain"
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(sendIntent, exportedTitle)
                            )
                        }
                    ) {
                        Text("Share")
                    }
                    TextButton(onClick = { exportedTextPreview = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
