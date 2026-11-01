package com.example.dayseed.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalExportFormat
import com.example.dayseed.ui.components.DaypartArtwork
import com.example.dayseed.ui.components.SettingsGroupCard
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.DarkBrand
import com.example.dayseed.ui.theme.DarkCanvas
import com.example.dayseed.ui.theme.LightBrand
import com.example.dayseed.ui.theme.LightCanvas
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedArtwork
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/**
 * Settings panels and inline controls for the 4-section Soft Botanical Settings Hub (D-212 / D-213).
 * Preserves all pre-existing Compose testTags while eliminating unnecessary modal sheet hops for
 * quick preferences and unifying Journal Export + Portable Backup into a cohesive Data sub-screen.
 */
enum class SettingsSheet {
    APPEARANCE, WEEK_START, QUIET_HOURS, JOURNAL_EXPORT, PRIVACY, REMOVED_HABITS, ABOUT
}

enum class DataSubScreenSection {
    ALL, JOURNAL_FOCUS, BACKUP_FOCUS
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

/**
 * Inline visual theme swatch cards rendered directly inside the Appearance & Rhythm hub card
 * (and reused inside [AppearancePanel] if opened as a sheet).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineAppearanceSelector(
    viewModel: DayseedViewModel,
    currentThemeMode: ThemeModeOption,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()
    val nowLocal = remember { viewModel.repository.clock.nowLocalTime() }
    val previewArtwork = remember(currentThemeMode, isSystemDark, nowLocal) {
        resolveDayseedArtwork(currentThemeMode, isSystemDark, nowLocal)
    }
    val daypart = remember(currentThemeMode, nowLocal, isSystemDark) {
        when (currentThemeMode) {
            ThemeModeOption.SOFT_BOTANICAL_AUTO -> SoftBotanicalThemeConfig.resolveDaypart(nowLocal)
            ThemeModeOption.LIGHT -> null
            ThemeModeOption.DARK -> BotanicalDaypart.NIGHT
            ThemeModeOption.SYSTEM -> if (isSystemDark) BotanicalDaypart.NIGHT else null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (mode in ThemeModeOption.entries) {
                val selected = currentThemeMode == mode
                val (swatchCanvas, swatchAccent) = remember(mode) {
                    when (mode) {
                        ThemeModeOption.SOFT_BOTANICAL_AUTO ->
                            SoftBotanicalThemeConfig.MorningCanvas to SoftBotanicalThemeConfig.EveningBrand
                        ThemeModeOption.LIGHT ->
                            LightCanvas to LightBrand
                        ThemeModeOption.DARK ->
                            DarkCanvas to DarkBrand
                        ThemeModeOption.SYSTEM ->
                            SoftBotanicalThemeConfig.EveningCanvas to LightBrand
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        }
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setThemeMode(mode) }
                        .testTag("theme_chip_${mode.settingValue.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(swatchCanvas)
                                .border(1.dp, swatchAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(swatchAccent)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DaypartArtwork(previewArtwork, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Active botanical atmosphere",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = daypart?.displayName ?: currentThemeMode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun InlineWeekStartSelector(
    viewModel: DayseedViewModel,
    currentWeekStart: DayOfWeek,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = currentWeekStart == DayOfWeek.MONDAY,
            onClick = { viewModel.setWeekStart(DayOfWeek.MONDAY) },
            label = { Text("Monday") },
            modifier = Modifier.testTag("week_start_monday_chip")
        )
        FilterChip(
            selected = currentWeekStart == DayOfWeek.SUNDAY,
            onClick = { viewModel.setWeekStart(DayOfWeek.SUNDAY) },
            label = { Text("Sunday") },
            modifier = Modifier.testTag("week_start_sunday_chip")
        )
    }
}

@Composable
fun AppearancePanel(
    viewModel: DayseedViewModel,
    currentThemeMode: ThemeModeOption
) {
    PanelTitle(
        "Appearance",
        "How Dayseed looks. Soft Botanical (Time of Day) shifts gradually across Morning, Day, Evening, and Night."
    )
    Spacer(modifier = Modifier.height(12.dp))
    InlineAppearanceSelector(viewModel = viewModel, currentThemeMode = currentThemeMode)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun WeekStartPanel(
    viewModel: DayseedViewModel,
    currentWeekStart: DayOfWeek
) {
    PanelTitle("Week starts", "Which day the Insights week and weekly targets begin on.")
    Spacer(modifier = Modifier.height(12.dp))
    InlineWeekStartSelector(viewModel = viewModel, currentWeekStart = currentWeekStart)
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
        "Quiet hours & gentle prompts",
        "Reminders pause during this window. To keep Dayseed calm, at most 3 reminders are sent per day."
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Quick presets",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (preset in SettingsSupport.quietHoursPresets) {
            val isSelected = startText.trim() == preset.start && endText.trim() == preset.end
            FilterChip(
                selected = isSelected,
                onClick = {
                    startText = preset.start
                    endText = preset.end
                },
                label = { Text(preset.label) },
                modifier = Modifier.testTag("quiet_preset_${preset.id}")
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                label = { Text("Start (24h)") },
                supportingText = { startError?.let { Text(it) } },
                isError = startError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiet_hours_start_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { startText = SettingsSupport.stepQuietHours(startText, -30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiet_start_minus_btn")
                ) { Text("-30m", style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { startText = SettingsSupport.stepQuietHours(startText, 30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiet_start_plus_btn")
                ) { Text("+30m", style = MaterialTheme.typography.labelSmall) }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it },
                label = { Text("End (24h)") },
                supportingText = { endError?.let { Text(it) } },
                isError = endError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiet_hours_end_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { endText = SettingsSupport.stepQuietHours(endText, -30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiet_end_minus_btn")
                ) { Text("-30m", style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { endText = SettingsSupport.stepQuietHours(endText, 30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiet_end_plus_btn")
                ) { Text("+30m", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Per-habit reminders default to 09:00 when turned on inside a habit. " +
                    "Android battery saver or Do Not Disturb can delay or silence reminders outside Dayseed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiet_hours_save_btn")
    ) {
        Text("Save quiet hours")
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalExportPanel(
    viewModel: DayseedViewModel,
    onPreview: (title: String, text: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    PanelTitle(
        "Read a copy of your journal",
        "Saved journal entries only — unsaved drafts and habit records are never included."
    )
    Spacer(modifier = Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview(
                        "Journal copy (Markdown)",
                        viewModel.repository.exportReadableJournals(JournalExportFormat.MARKDOWN)
                    )
                }
            },
            modifier = Modifier.testTag("export_journal_md_btn")
        ) { Text("Markdown") }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview(
                        "Journal copy (CSV)",
                        viewModel.repository.exportReadableJournals(JournalExportFormat.CSV)
                    )
                }
            },
            modifier = Modifier.testTag("export_journal_csv_btn")
        ) { Text("CSV") }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    onPreview(
                        "Journal copy (JSON)",
                        viewModel.repository.exportReadableJournals(JournalExportFormat.JSON)
                    )
                }
            },
            modifier = Modifier.testTag("export_journal_json_btn")
        ) { Text("JSON") }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    val md = viewModel.repository.exportReadableJournals(JournalExportFormat.MARKDOWN)
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, md)
                        type = "text/markdown"
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(sendIntent, "Share journal copy")
                    )
                }
            },
            modifier = Modifier.testTag("share_journal_md_btn")
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Markdown")
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
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
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Designed as a calm, offline-first field journal for sustainable habits and honest reflection.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
}

/**
 * Unified Data, Backup & Journal Copy sub-screen (D-213).
 * Combines:
 * 1. On-device data summary card (habits, check-ins, saved reflections)
 * 2. Portable Backup with masked password, optional confirmation check, and Share
 * 3. Restore with pre-restore payload inspection and merge conflict resolution
 * 4. Readable Journal Copy exports (Markdown, CSV, JSON, Share)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupRestorePanel(
    viewModel: DayseedViewModel,
    backupPassword: String,
    onBackupPasswordChange: (String) -> Unit,
    onPreview: (title: String, text: String) -> Unit,
    onBack: () -> Unit,
    initialFocus: DataSubScreenSection = DataSubScreenSection.ALL
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val habits by viewModel.habits.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val restoreConflicts by viewModel.restoreConflicts.collectAsState()

    val activeCount = remember(habits) { habits.count { it.lifecycleState == HabitLifecycleState.ACTIVE } }
    val pausedCount = remember(habits) { habits.count { it.lifecycleState != HabitLifecycleState.ACTIVE } }
    val totalCheckIns = remember(habitEntries, habitDayOutcomes) { habitEntries.size + habitDayOutcomes.size }
    val summaryLine = remember(activeCount, pausedCount, totalCheckIns, journalEntries.size) {
        SettingsSupport.dataSummaryLine(activeCount, pausedCount, totalCheckIns, journalEntries.size)
    }

    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordMismatchError = SettingsSupport.backupPasswordMismatchError(backupPassword, confirmPassword)
    val passwordReady = SettingsSupport.isBackupPasswordReady(backupPassword, confirmPassword)

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreMode by remember { mutableStateOf("MERGE") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    val payloadInspection = remember(restoreJsonInput) {
        SettingsSupport.inspectBackupPayloadSummary(restoreJsonInput)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
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
            Column {
                Text(
                    text = "Data, backup & journal copy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Everything stored on this device, ready to export or restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 1. On-Device Data Summary Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("data_summary_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Currently on this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summaryLine,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // If opened from "Read a copy of your journal", show Journal Copy card first
        if (initialFocus == DataSubScreenSection.JOURNAL_FOCUS) {
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    JournalExportPanel(viewModel = viewModel, onPreview = onPreview)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Portable Backup Card
        SettingsGroupCard {
            Column(modifier = Modifier.padding(16.dp)) {
                PanelTitle(
                    "Portable backup (.dayseed.json)",
                    "One file holding everything: habits, entries, reflections and settings, with an " +
                        "optional password. A forgotten password means the file cannot be opened."
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = onBackupPasswordChange,
                    label = { Text("Optional backup password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("toggle_backup_password_visibility")
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_password_input")
                )
                if (backupPassword.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password (optional check)") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        isError = passwordMismatchError != null,
                        supportingText = {
                            passwordMismatchError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_password_confirm_input")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                        enabled = passwordReady,
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
                        enabled = passwordReady,
                        modifier = Modifier.testTag("share_backup_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Restore Card
        SettingsGroupCard {
            Column(modifier = Modifier.padding(16.dp)) {
                PanelTitle(
                    "Restore from a backup",
                    "Before importing, Dayseed creates and verifies an automatic safety backup of your current records."
                )
                Spacer(modifier = Modifier.height(12.dp))
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

                // Merge conflicts (D-202)
                if (restoreConflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
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
            }
        }

        if (initialFocus != DataSubScreenSection.JOURNAL_FOCUS) {
            Spacer(modifier = Modifier.height(16.dp))
            // 4. Readable Journal Copy Card
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    JournalExportPanel(viewModel = viewModel, onPreview = onPreview)
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
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE
                            ) as android.content.ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (!clipText.isNullOrBlank()) {
                                restoreJsonInput = clipText
                            }
                        },
                        modifier = Modifier.testTag("paste_backup_clipboard_btn")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste from clipboard", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Paste backup file contents") },
                        maxLines = 5,
                        supportingText = {
                            when {
                                restoreError != null -> Text(restoreError!!, color = MaterialTheme.colorScheme.error)
                                payloadInspection != null -> Text(payloadInspection)
                            }
                        },
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
