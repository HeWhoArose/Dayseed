package com.example.dayseed.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dayseed.BuildConfig
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.BackupLimits
import com.example.dayseed.data.ConflictResolution
import com.example.dayseed.data.Habit
import com.example.dayseed.data.readBoundedBytes
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.JournalExportFormat
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.ui.components.DaypartArtwork
import com.example.dayseed.ui.components.SettingsGroupCard
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.DarkBrand
import com.example.dayseed.ui.theme.DarkCanvas
import com.example.dayseed.ui.theme.LightBrand
import com.example.dayseed.ui.theme.LightCanvas
import com.example.dayseed.ui.theme.LocalDayseedLiveTime
import com.example.dayseed.ui.theme.NightGoldAccent
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.components.dayseedHeading
import com.example.dayseed.ui.theme.artworkForDaypart
import com.example.dayseed.ui.theme.greetingFor
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import com.example.dayseed.ui.theme.resolveDayseedArtwork
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/**
 * Settings panels and inline controls for the 4-section Soft Botanical Settings Hub (D-212 / D-213).
 * Preserves all pre-existing Compose testTags while eliminating unnecessary modal sheet hops for
 * quick preferences and unifying Journal Export + Portable Backup into a cohesive Data sub-screen.
 */
enum class SettingsSheet {
    APPEARANCE, WEEK_START, REMINDERS, PRIVACY, REMOVED_HABITS, ABOUT
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
private fun themeSwatches(mode: ThemeModeOption): List<Color> = when (mode) {
    ThemeModeOption.SOFT_BOTANICAL_AUTO -> listOf(
        SoftBotanicalThemeConfig.MorningCanvas, LightCanvas, SoftBotanicalThemeConfig.EveningCanvas, DarkCanvas
    )
    ThemeModeOption.BOTANICAL_LIGHT -> listOf(
        SoftBotanicalThemeConfig.MorningCanvas, LightCanvas, SoftBotanicalThemeConfig.EveningCanvas,
        SoftBotanicalThemeConfig.NightLightCanvas
    )
    ThemeModeOption.DARK_DAYPART_ART -> listOf(
        SoftBotanicalThemeConfig.MorningDarkCanvas, SoftBotanicalThemeConfig.DayDarkCanvas,
        SoftBotanicalThemeConfig.EveningDarkCanvas, DarkCanvas
    )
    ThemeModeOption.BOTANICAL_SYSTEM -> listOf(
        SoftBotanicalThemeConfig.MorningCanvas, SoftBotanicalThemeConfig.DayDarkCanvas,
        SoftBotanicalThemeConfig.EveningCanvas, DarkCanvas
    )
    ThemeModeOption.LIGHT -> listOf(LightCanvas)
    ThemeModeOption.DARK -> listOf(DarkCanvas)
    ThemeModeOption.SYSTEM -> listOf(LightCanvas, DarkCanvas)
}

private fun themeDescription(mode: ThemeModeOption): String = when (mode) {
    ThemeModeOption.SOFT_BOTANICAL_AUTO -> "Light by day, dark at night. Leaves, hills and moon change with the hour."
    ThemeModeOption.BOTANICAL_LIGHT -> "Always light. The colours and artwork still change from morning to night."
    ThemeModeOption.DARK_DAYPART_ART -> "Always dark. The colours and artwork still change from morning to night."
    ThemeModeOption.BOTANICAL_SYSTEM -> "Light or dark to match your phone, with the time-of-day colours and artwork."
    ThemeModeOption.LIGHT -> "Calm light. No artwork."
    ThemeModeOption.DARK -> "Calm dark. No artwork."
    ThemeModeOption.SYSTEM -> "Matches your phone. No artwork."
}

@Composable
fun InlineAppearanceSelector(
    viewModel: DayseedViewModel,
    currentThemeMode: ThemeModeOption,
    modifier: Modifier = Modifier
) {
    val nowLocal = LocalDayseedLiveTime.current
    val daypart = remember(nowLocal) { SoftBotanicalThemeConfig.resolveDaypart(nowLocal) }
    val previewArtwork = remember(daypart) { artworkForDaypart(daypart) }

    @Composable
    fun Group(title: String, subtitle: String, modes: List<ThemeModeOption>) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp).dayseedHeading()
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                val selected = currentThemeMode == mode
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .selectable(selected = selected, role = Role.RadioButton) { viewModel.setThemeMode(mode) }
                        .testTag("theme_chip_${mode.settingValue.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            themeSwatches(mode).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = themeDescription(mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Group(
            "Botanical",
            "Leaf artwork and colours that follow the time of day.",
            ThemeModeOption.entries.filter { it.isBotanical }
        )
        Spacer(Modifier.height(10.dp))
        Group(
            "Plain",
            "No artwork. Only your Living Grove keeps its plants.",
            ThemeModeOption.entries.filter { !it.isBotanical }
        )

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
                if (currentThemeMode.isBotanical) {
                    DaypartArtwork(previewArtwork, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Right now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (currentThemeMode.isBotanical) "${daypart.displayName} · ${greetingFor(daypart)}" else greetingFor(daypart),
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
    val appSettings by viewModel.appSettings.collectAsState()
    val appReducedMotionSetting = remember(appSettings) {
        appSettings["reduced_motion_enabled"]?.valueText.equals("true", ignoreCase = true)
    }
    val systemReducedMotion = remember(appSettings) {
        viewModel.isSystemReducedMotionActive()
    }
    val effectiveReducedMotion = appReducedMotionSetting || systemReducedMotion

    PanelTitle(
        "Appearance",
        "How Dayseed looks. Botanical themes follow the time of day: Morning 5am, Day 11am, Evening 4pm, " +
            "Night 7pm. Every theme greets you by the time of day."
    )
    Spacer(modifier = Modifier.height(12.dp))
    InlineAppearanceSelector(viewModel = viewModel, currentThemeMode = currentThemeMode)
    Spacer(modifier = Modifier.height(14.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Reduce motion",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (systemReducedMotion) {
                    "Active via Android system accessibility settings"
                } else {
                    "Simplify theme transitions, botanical motion, and launch animations"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = effectiveReducedMotion,
            onCheckedChange = { enabled ->
                if (!systemReducedMotion) {
                    viewModel.setReducedMotionEnabled(enabled)
                }
            },
            enabled = !systemReducedMotion,
            modifier = Modifier.testTag("appearance_sheet_reduced_motion_switch")
        )
    }
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

/**
 * Notifications panel (D-248): deliberately small. Whether Dayseed may show notifications at all is Android's decision,
 * so this panel only links to it; the reminder time lives on each habit. The two weekly check-ins are the only
 * Dayseed-side switches.
 */
@Composable
fun NotificationsPanel(viewModel: DayseedViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appSettings by viewModel.appSettings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> allowed = notificationsAllowed(context) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = notificationsAllowed(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PanelTitle("Notifications", "Habit reminders arrive at the time you set on each habit.")
    Spacer(modifier = Modifier.height(10.dp))

    if (!allowed) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().testTag("notifications_off_notice")
        ) {
            Text(
                text = "Notifications are off for Dayseed, so reminders can't reach you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSystemNotificationSettings(context)
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminders_request_permission_btn")
        ) { Text("Allow notifications") }
        Spacer(modifier = Modifier.height(8.dp))
    }
    OutlinedButton(
        onClick = { openSystemNotificationSettings(context) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminders_open_system_settings_btn")
    ) { Text("Notification settings") }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            val posted = allowed && com.example.dayseed.data.ReminderNotifier.postWeeklyReminder(
                context, "dayseed_preview", 9_001, "Preview", "This is how a Dayseed reminder looks and sounds."
            )
            android.widget.Toast.makeText(
                context,
                if (posted) "Preview sent" else "Allow notifications first",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminders_preview_notification_btn")
    ) { Text("Send a preview") }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Weekly check-ins",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.dayseedHeading()
    )
    Spacer(modifier = Modifier.height(8.dp))
    val weeklyReflectionsPreference =
        appSettings["weekly_reflections_enabled"]?.valueText.equals("true", ignoreCase = true)
    val encouragementTipsPreference =
        appSettings["encouragement_tips_enabled"]?.valueText.equals("true", ignoreCase = true)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Fresh start each week", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "A gentle note at the start of your week.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = weeklyReflectionsPreference,
            onCheckedChange = { viewModel.setWeeklyReflectionsEnabled(it) },
            modifier = Modifier
                .testTag("weekly_reflection_switch")
                .semantics {
                    contentDescription = "Fresh start each week"
                    stateDescription = if (weeklyReflectionsPreference) "On" else "Off"
                }
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Encouragement tips", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Occasional tips after a week of partial progress.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = encouragementTipsPreference,
            onCheckedChange = { viewModel.setEncouragementTipsEnabled(it) },
            modifier = Modifier
                .testTag("encouragement_tips_switch")
                .semantics {
                    contentDescription = "Encouragement tips"
                    stateDescription = if (encouragementTipsPreference) "On" else "Off"
                }
        )
    }

    // ---- Debug only: real delivery verification (never shown in release builds) ----
    if (com.example.dayseed.BuildConfig.DEBUG) {
        Spacer(modifier = Modifier.height(14.dp))
        ReminderDebugVerificationSection()
    }
    Spacer(modifier = Modifier.height(20.dp))
}

private fun notificationsAllowed(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
    val permitted = android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    return permitted && manager?.areNotificationsEnabled() == true
}

/**
 * Debug-only end-to-end verification (Phase 4 remediation): schedules a REAL inexact alarm
 * through the production receiver, posts a REAL notification on the `dayseed_reminders`
 * channel, and auto-cancels it — while reporting the four independent gates separately
 * (preference / runtime permission / app-level enablement / channel).
 *
 * It writes no reminder preference, inserts no ledger rows, and touches no habit data.
 * Release builds never compose this section (`BuildConfig.DEBUG` guard), so the debug
 * controls and receiver action do not exist in shipping behaviour.
 */
@Composable
private fun ReminderDebugVerificationSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val probeState by com.example.dayseed.data.ReminderDebugProbe.state.collectAsState()

    SettingsGroupCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Debug: delivery verification",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Visible in debug builds only. Runs a real AlarmManager alarm through the " +
                    "notification receiver, posts a real test notification on the " +
                    "dayseed_reminders channel, and cancels it automatically after ~5 seconds. " +
                    "This does not change the reminder preference, habit data, or the delivery " +
                    "ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { com.example.dayseed.data.ReminderDebugProbe.startProbe(context) },
                modifier = Modifier.testTag("debug_probe_run_btn")
            ) {
                Text("Run delivery verification", style = MaterialTheme.typography.labelMedium)
            }
            probeState?.let { state ->
                Spacer(modifier = Modifier.height(10.dp))
                state.steps.forEach { step ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (step.ok) "✓" else "✗",
                            color = if (step.ok) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = step.name, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (!state.finished) {
                    Text(
                        text = if (state.alarmFired) {
                            "Alarm received — auto-cancel pending…"
                        } else {
                            "Waiting for the probe alarm…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun readReminderAvailability(
    context: android.content.Context,
    appPreferenceEnabled: Boolean
): SettingsSupport.ReminderAvailability {
    val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
        as? android.app.NotificationManager
    val channel = manager?.getNotificationChannel(ReminderNotifier.CHANNEL_ID)
    return SettingsSupport.reminderAvailability(
        appPreferenceEnabled = appPreferenceEnabled,
        // Only API 33+ has a runtime notification permission. Without this guard the
        // availability panel reports "permission not granted" on API 26-32 devices that never
        // had to grant anything, which reads as a broken feature.
        runtimePermissionGranted = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        appNotificationsEnabled = manager?.areNotificationsEnabled() == true,
        reminderChannelCreated = channel != null,
        reminderChannelEnabled = channel != null &&
            channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
    )
}

private fun openSystemNotificationSettings(context: android.content.Context) {
    try {
        context.startActivity(android.content.Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        })
    } catch (_: Exception) {
        try {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            // No settings app reachable; nothing to do from here.
        }
    }
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
                    IconButton(
                        onClick = { pendingDelete = h },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("perm_delete_habit_${h.habitId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${h.name} permanently",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
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
    Spacer(modifier = Modifier.height(12.dp))
    com.example.dayseed.ui.components.DayseedOpeningBrandLockup(
        emblemSize = 68.dp,
        showMotto = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Dayseed ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.testTag("about_version_text")
    )
    Text(
        text = "Build: ${BuildConfig.BUILD_TYPE} · Commit: ${BuildConfig.GIT_COMMIT_SHA}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("about_build_provenance_text")
    )
    Spacer(modifier = Modifier.height(4.dp))
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
    // Pre-restore metadata runs off the UI thread (Dispatchers.IO) so password-based
    // PBKDF2 decryption never blocks composition, even on every keystroke while typing.
    var restoreMetadataState by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(restoreJsonInput, backupPassword) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val metadata = SettingsSupport.inspectBackupMetadata(
                    restoreJsonInput,
                    backupPassword.takeIf { it.isNotBlank() }
                )
                val line = SettingsSupport.backupMetadataLine(metadata)
                restoreMetadataState = line
            }
        }
    }
    val restoreMetadata: String? = restoreMetadataState
    var saveFileMessage by remember { mutableStateOf<String?>(null) }
    var saveFileError by remember { mutableStateOf<String?>(null) }
    var filePickerError by remember { mutableStateOf<String?>(null) }
    // Actual SAF document names are resolved via ContentResolver/OpenableColumns and shown to
    // the user, because Android may rename the created document (numbered duplicate).
    var saveFileName by remember { mutableStateOf<String?>(null) }
    var restoreFileName by remember { mutableStateOf<String?>(null) }

    // ---- Save a backup via the Android Storage Access Framework (user-selected destination) ----
    // Writes the complete backup to the chosen document, then verifies the produced file can be
    // read back identically. Success is never reported before the write and read-back finished.
    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            filePickerError = "No destination chosen — nothing was saved."
            return@rememberLauncherForActivityResult
        }
        filePickerError = null
        saveFileError = null
        saveFileMessage = null
        // Ask the provider for the real document name: Android may adjust the requested name
        // (e.g. a numbered duplicate like "dayseed-backup-2026-01-01 (1).dayseed.json") instead
        // of overwriting an existing document, so the user should see what was actually created.
        saveFileName = SettingsSupport.resolveDocumentDisplayName(context.contentResolver, uri)
        coroutineScope.launch {
            try {
                val json = viewModel.repository.exportDataAsJson(
                    password = backupPassword.takeIf { it.isNotBlank() }
                )
                val bytes = json.toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("Could not open the chosen destination for writing")

                // Mandatory read-back verification: the document must be usable, not just writable.
                val readBack = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("Could not read the written backup back for verification")
                if (readBack != json) {
                    throw IllegalStateException("The saved backup could not be verified after writing")
                }
                saveFileMessage = "Backup saved and verified as " +
                    (saveFileName ?: "the document you chose") + ". " +
                    "If an existing file had the same name, Android may have created a numbered " +
                    "copy such as \"" + (saveFileName?.substringBeforeLast(".") ?: "file") + " (1)…" +
                    "\" instead of overwriting it — the newest document is the one just written. " +
                    "Dayseed does not track or update this file later."
            } catch (e: Exception) {
                saveFileError = "That backup could not be written or verified there. " +
                    "Nothing was reported as saved. (${e.message ?: "storage error"})"
            }
        }
    }

    // ---- Restore a backup via the system document picker (user-selected source) ----
    // Reads and validates the chosen document; the existing pre-restore inspection,
    // password handling, Merge/Replace choice and safety backup stay unchanged.
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            filePickerError = "No backup selected — nothing was opened."
            return@rememberLauncherForActivityResult
        }
        filePickerError = null
        // Show the real selected filename in the restore dialog where it matters.
        restoreFileName = SettingsSupport.resolveDocumentDisplayName(context.contentResolver, uri)
        try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBoundedBytes(BackupLimits.MAX_BACKUP_SIZE_BYTES)
                    .toString(Charsets.UTF_8)
            } ?: throw IllegalStateException("Could not read the selected backup")
            if (text.isBlank()) throw IllegalStateException("The selected backup is empty")
            val summary = SettingsSupport.inspectBackupPayloadSummary(text)
            if (summary == null && !text.trimStart().startsWith("{")) {
                throw IllegalStateException("Doesn't look like a Dayseed backup file")
            }
            restoreError = null
            restoreJsonInput = text
            showRestoreDialog = true
        } catch (e: Exception) {
            filePickerError = "That file could not be read as a Dayseed backup. " +
                "Nothing was changed. (${e.message ?: "could not open file"})"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
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
                        .bringIntoViewOnFocus()
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
                            .bringIntoViewOnFocus()
                            .testTag("backup_password_confirm_input")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (saveFileMessage != null || saveFileError != null || filePickerError != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (saveFileError != null) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    saveFileError != null -> Icons.Default.VisibilityOff
                                    saveFileMessage != null -> Icons.Default.CheckCircle
                                    else -> Icons.Default.ContentPaste
                                },
                                contentDescription = null,
                                tint = if (saveFileError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = saveFileError
                                    ?: saveFileMessage
                                    ?: filePickerError
                                    ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        saveFileMessage = null
                        saveFileError = null
                        filePickerError = null
                        saveBackupLauncher.launch("dayseed-backup-${java.time.LocalDate.now()}.dayseed.json")
                    },
                    enabled = passwordReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_backup_file_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save backup to a file (choose location)")
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
                        filePickerError = null
                        restoreFileName = null
                        showRestoreDialog = true
                    },
                    modifier = Modifier.testTag("open_restore_dialog_btn")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore from pasted contents")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        restoreError = null
                        filePickerError = null
                        openBackupLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_backup_file_btn")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose a backup file…")
                }
                if (filePickerError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = filePickerError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
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
                    if (restoreFileName != null) {
                        Text(
                            text = "Selected file: $restoreFileName",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                                restoreError != null -> {
                                    Text(
                                        "That didn't restore. Check the file contents" +
                                            (if (backupPassword.isNotBlank()) " and the password" else "") +
                                            ".",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                restoreMetadata != null -> Text(restoreMetadata!!)
                                payloadInspection != null -> Text(payloadInspection)
                            }
                        },
                        isError = restoreError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
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
                                restoreFileName = null
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
