package com.example.dayseed.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.DurationNormalizer
import com.example.dayseed.data.Habit
import com.example.dayseed.data.HabitLifecycleState
import com.example.dayseed.data.HabitMeasurementKind
import com.example.dayseed.data.HabitNaturalLanguageParser
import com.example.dayseed.data.ReminderEngine
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.SmartHabitInterpretation
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.data.UnitFormatter
import com.example.dayseed.data.WeekdayMask
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

fun weeklyTargetSemanticsExplanation(trackingMode: TrackingMode, unitCode: String?): String {
    val canonicalMode = trackingMode.canonical()
    return when {
        canonicalMode == TrackingMode.COMPLETION_ONLY || canonicalMode == TrackingMode.EFFORT_AWARE ->
            "Days/week counts distinct qualifying calendar dates (at most 1 day per date, 1–7 days/week)."
        canonicalMode == TrackingMode.QUANTITY && unitCode?.trim().equals("sessions", ignoreCase = true) ->
            "Sessions/week counts each logged session (value > 0) toward your weekly goal — multiple sessions on the same day count separately (unlike days/week)."
        else ->
            "Weekly target progress accumulates across all sessions logged during your configured week."
    }
}

@Composable
fun CreateEditHabitScreen(
    viewModel: DayseedViewModel,
    habitId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val habitEntries by viewModel.habitEntries.collectAsState()
    val habitDayOutcomes by viewModel.habitDayOutcomes.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val saveError by viewModel.lastSaveError.collectAsState()
    val initialHabit = remember(habits, habitId) {
        habitId?.let { id -> habits.find { it.habitId == id } }
    }
    val today = viewModel.repository.clock.todayLocal()
    val alreadyLoggedToday = remember(initialHabit, today, habitEntries, habitDayOutcomes) {
        initialHabit?.let { viewModel.repository.hasRecordedActivityForVersionLockOnDate(it.habitId, today) } ?: false
    }
    val suggestedReminder = remember(initialHabit) {
        initialHabit?.let { viewModel.repository.getSuggestedReminderTimeForHabit(it.habitId) }
    }

    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshTick++
    }

    val is24HourFormat = remember(context, permissionRefreshTick) {
        DateFormat.is24HourFormat(context)
    }
    val globalRemindersEnabled = remember(appSettings) {
        val raw = appSettings["reminders_enabled"]?.valueText
        raw.equals("true", ignoreCase = true) || raw == "1"
    }
    val quietHoursStart = remember(appSettings) {
        appSettings["quiet_hours_start"]?.valueText ?: "22:00"
    }
    val quietHoursEnd = remember(appSettings) {
        appSettings["quiet_hours_end"]?.valueText ?: "07:00"
    }
    val hasPostNotificationsPermission = remember(context, permissionRefreshTick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val isNotificationDeliveryAvailable = remember(context, permissionRefreshTick) {
        ReminderNotifier.isNotificationDeliveryAvailable(context)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_edit_habit_screen"),
        color = Color.Transparent
    ) {
        CreateEditHabitFormContent(
            initialHabit = initialHabit,
            alreadyLoggedToday = alreadyLoggedToday,
            suggestedReminderTime = suggestedReminder,
            isFullScreen = true,
            is24HourFormat = is24HourFormat,
            globalRemindersEnabled = globalRemindersEnabled,
            hasPostNotificationsPermission = hasPostNotificationsPermission,
            isNotificationDeliveryAvailable = isNotificationDeliveryAvailable,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= 33 && !hasPostNotificationsPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            },
            saveErrorMessage = saveError,
            onClearSaveError = { viewModel.clearSaveError() },
            onDismiss = onNavigateBack,
            onSave = { habit, applyTodayWhenLogged ->
                viewModel.saveHabit(habit, applyTodayWhenLogged)
                true
            },
            onDelete = if (initialHabit != null) {
                { id ->
                    viewModel.deleteHabit(id)
                }
            } else {
                null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CreateEditHabitFormContent(
    initialHabit: Habit?,
    alreadyLoggedToday: Boolean = false,
    suggestedReminderTime: String? = null,
    isFullScreen: Boolean = false,
    is24HourFormat: Boolean = false,
    globalRemindersEnabled: Boolean = false,
    hasPostNotificationsPermission: Boolean = true,
    isNotificationDeliveryAvailable: Boolean = true,
    quietHoursStart: String = "22:00",
    quietHoursEnd: String = "07:00",
    onRequestNotificationPermission: (() -> Unit)? = null,
    saveErrorMessage: String? = null,
    onClearSaveError: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (habit: Habit, applyTodayWhenLogged: Boolean) -> Boolean,
    onDelete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEditing = initialHabit != null

    val initialMeasurementKind = remember(initialHabit) {
        when {
            initialHabit == null -> HabitMeasurementKind.SIMPLE_CHECK_OFF
            initialHabit.trackingMode.canonical() == TrackingMode.DURATION -> HabitMeasurementKind.DURATION
            initialHabit.trackingMode.canonical() == TrackingMode.QUANTITY &&
                UnitFormatter.isCurrencyUnit(initialHabit.unitCode) -> HabitMeasurementKind.AMOUNT_CURRENCY
            initialHabit.trackingMode.canonical() == TrackingMode.QUANTITY -> HabitMeasurementKind.QUANTITY
            else -> HabitMeasurementKind.SIMPLE_CHECK_OFF
        }
    }

    var name by remember(initialHabit) { mutableStateOf(initialHabit?.name ?: "") }
    var interpretedActivityName by remember(initialHabit) { mutableStateOf(initialHabit?.name ?: "") }
    var userEditedActivityOverride by remember(initialHabit) { mutableStateOf(isEditing) }
    var userOverrodeMeasurement by remember(initialHabit) { mutableStateOf(isEditing) }
    var userOverrodeSchedule by remember(initialHabit) { mutableStateOf(isEditing) }

    var description by remember(initialHabit) { mutableStateOf(initialHabit?.description ?: "") }
    var category by remember(initialHabit) { mutableStateOf(initialHabit?.category ?: "") }
    var lifetimeGoalText by remember(initialHabit) {
        mutableStateOf(initialHabit?.lifetimeGoalDays?.toString() ?: "")
    }

    var selectedMeasurementKind by remember(initialHabit) {
        mutableStateOf(initialMeasurementKind)
    }
    var trackingMode by remember(initialHabit) {
        mutableStateOf((initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical())
    }
    var targetPeriod by remember(initialHabit) {
        mutableStateOf(
            if (initialHabit?.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                TargetPeriod.WEEK
            } else {
                initialHabit?.targetPeriod ?: TargetPeriod.DAY
            }
        )
    }
    var targetValueText by remember(initialHabit) {
        mutableStateOf(
            (initialHabit?.targetValue ?: when {
                (initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical() == TrackingMode.DURATION -> 15
                (initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical() == TrackingMode.QUANTITY -> 10
                else -> 3
            }).toString()
        )
    }
    var unitCode by remember(initialHabit) {
        mutableStateOf(
            initialHabit?.unitCode ?: when ((initialHabit?.trackingMode ?: TrackingMode.COMPLETION_ONLY).canonical()) {
                TrackingMode.DURATION -> "min"
                TrackingMode.QUANTITY -> "pages"
                else -> ""
            }
        )
    }
    var scheduleKind by remember(initialHabit) {
        mutableStateOf(initialHabit?.scheduleKind ?: ScheduleKind.DAILY)
    }
    var tinyDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.tinyDescription ?: "Deliberately small action")
    }
    var usualDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.usualDescription ?: "The ordinary intended action")
    }
    var extraDesc by remember(initialHabit) {
        mutableStateOf(initialHabit?.extraDescription ?: "Optional stretch action")
    }
    var selectedDays by remember(initialHabit) {
        val initDays = when {
            initialHabit == null -> DayOfWeek.values().toSet()
            initialHabit.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
                val prefMask = initialHabit.preferredWeekdaysMask
                if (prefMask == null || prefMask == 0 || prefMask == WeekdayMask.ALL_DAYS_MASK) {
                    emptySet()
                } else {
                    WeekdayMask.fromMask(prefMask)
                }
            }
            else -> initialHabit.scheduleDays
        }
        mutableStateOf(initDays)
    }
    var selectedColor by remember(initialHabit) {
        mutableStateOf(initialHabit?.colorHex ?: "#286143")
    }
    var reminderEnabled by remember(initialHabit) {
        mutableStateOf(initialHabit?.reminderMode == "AT_TIME")
    }
    var reminderTimeLocal by remember(initialHabit, suggestedReminderTime) {
        mutableStateOf(initialHabit?.reminderTimeLocal ?: suggestedReminderTime ?: "09:00")
    }
    var selectedPeriodIsPm by remember(initialHabit, suggestedReminderTime) {
        mutableStateOf(SettingsSupport.isPmTime(initialHabit?.reminderTimeLocal ?: suggestedReminderTime ?: "09:00"))
    }
    val reminderSectionBringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(reminderEnabled) {
        if (reminderEnabled) {
            runCatching { reminderSectionBringIntoViewRequester.bringIntoView() }
        }
    }
    var applyTodayWhenLogged by remember(alreadyLoggedToday) {
        mutableStateOf(!alreadyLoggedToday)
    }

    // Collapsible More Options / Advanced Settings (collapsed by default for new habits)
    var showAdvancedOptions by remember(initialHabit) {
        mutableStateOf(
            isEditing && (
                !initialHabit?.description.isNullOrBlank() ||
                    !initialHabit?.category.isNullOrBlank() ||
                    initialHabit?.lifetimeGoalDays != null
                )
        )
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var targetValueError by remember { mutableStateOf<String?>(null) }
    var lifetimeGoalError by remember { mutableStateOf<String?>(null) }
    var reminderTimeError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val colorOptions = listOf("#286143", "#3B6955", "#4A7C59", "#708D81", "#8D6B94", "#C08552")
    val categorySuggestions = listOf("Mindfulness", "Movement", "Reading", "Health", "Focus")
    val quantityUnitPresets = listOf("pages", "seeds", "words", "reps", "glasses", "km", "sessions")
    val currencyUnitPresets = listOf("naira", "dollars", "pounds", "euros", "cedis", "shillings")
    val promptExamples = listOf(
        "Read 10 pages of a book",
        "Walk for 30 minutes",
        "Meditate daily",
        "Save ₦5,000"
    )

    val parsedInterpretation = remember(name) {
        HabitNaturalLanguageParser.parse(name)
    }

    fun applyNaturalLanguageInput(newInput: String) {
        name = newInput
        if (newInput.isNotBlank()) nameError = null
        onClearSaveError()

        val interpretation = HabitNaturalLanguageParser.parse(newInput)
        if (!userEditedActivityOverride) {
            interpretedActivityName = if (interpretation.extractedMeasurementFromPrompt || interpretation.extractedScheduleFromPrompt) {
                interpretation.activityName.ifBlank { newInput.trim() }
            } else {
                newInput.trim()
            }
        }

        if (!userOverrodeMeasurement) {
            if (interpretation.extractedMeasurementFromPrompt) {
                selectedMeasurementKind = interpretation.measurementKind
                trackingMode = interpretation.trackingMode
                targetValueText = (interpretation.targetValue ?: 1).toString()
                unitCode = interpretation.unitCode ?: when (interpretation.trackingMode) {
                    TrackingMode.DURATION -> "min"
                    TrackingMode.QUANTITY -> "pages"
                    else -> ""
                }
                targetValueError = null
            } else {
                selectedMeasurementKind = HabitMeasurementKind.SIMPLE_CHECK_OFF
                trackingMode = TrackingMode.COMPLETION_ONLY
            }
        }

        if (!userOverrodeSchedule && interpretation.extractedScheduleFromPrompt) {
            scheduleKind = interpretation.scheduleKind
            targetPeriod = interpretation.targetPeriod
            if (interpretation.scheduledWeekdaysMask != null) {
                selectedDays = WeekdayMask.fromMask(interpretation.scheduledWeekdaysMask)
            } else if (interpretation.scheduleKind == ScheduleKind.DAILY) {
                selectedDays = DayOfWeek.values().toSet()
            }
        }
    }

    val resolvedHabitName = remember(name, interpretedActivityName, parsedInterpretation, userEditedActivityOverride) {
        if (userEditedActivityOverride && interpretedActivityName.isNotBlank()) {
            interpretedActivityName.trim()
        } else if (parsedInterpretation.extractedMeasurementFromPrompt || parsedInterpretation.extractedScheduleFromPrompt) {
            interpretedActivityName.trim().ifBlank { parsedInterpretation.activityName.ifBlank { name.trim() } }
        } else {
            name.trim()
        }
    }

    val hasMidWeekIncompatibleChange = remember(
        isEditing,
        initialHabit,
        trackingMode,
        unitCode,
        targetPeriod
    ) {
        if (!isEditing || initialHabit == null) {
            false
        } else {
            val wasWeekly = initialHabit.targetPeriod == TargetPeriod.WEEK ||
                initialHabit.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            val isNowWeekly = targetPeriod == TargetPeriod.WEEK ||
                scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET
            (wasWeekly || isNowWeekly) && (
                initialHabit.trackingMode.canonical() != trackingMode ||
                    (initialHabit.unitCode ?: "") != unitCode.trim() ||
                    initialHabit.targetPeriod != targetPeriod
                )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(if (isFullScreen) 18.dp else 20.dp)
            .testTag("add_edit_habit_dialog"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFullScreen) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("secondary_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isEditing) "Edit Habit" else "New Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isFullScreen) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialog")
                }
            }
        }

        // STEP 1: Primary Natural-Language Input ("What would you like to do?")
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) "Habit Name or Goal" else "What would you like to do?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Describe your habit naturally — with or without a number, unit, or schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { applyNaturalLanguageInput(it) },
                    label = { Text(if (isEditing) "Habit name *" else "What would you like to do? *") },
                    placeholder = { Text("e.g. Read 10 pages of a book") },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${name.length}/80 characters")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
                        .testTag("habit_name_input"),
                    singleLine = true
                )

                if (!isEditing && name.isBlank()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (example in promptExamples) {
                            SuggestionChip(
                                onClick = {
                                    userEditedActivityOverride = false
                                    userOverrodeMeasurement = false
                                    userOverrodeSchedule = false
                                    applyNaturalLanguageInput(example)
                                },
                                label = { Text(example, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }

        // STEP 2: "Here's what we understood" Confirmation & Measurement Adjustment Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_habit_interpretation_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Here's what we understood:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Live interpreted summary box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SummaryRow(
                            label = "Activity",
                            value = resolvedHabitName.ifBlank { "—" },
                            testTag = "smart_summary_activity"
                        )
                        SummaryRow(
                            label = "Measurement type",
                            value = selectedMeasurementKind.label,
                            testTag = "smart_summary_measurement_type"
                        )
                        if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) ||
                            targetPeriod == TargetPeriod.WEEK
                        ) {
                            val targetNumeric = targetValueText.trim().toIntOrNull() ?: 1
                            val targetSummaryDisplay = if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                                val effectiveUnit = unitCode.ifBlank {
                                    if (trackingMode == TrackingMode.DURATION) "min" else "units"
                                }
                                UnitFormatter.formatValueWithUnit(targetNumeric, effectiveUnit)
                            } else {
                                targetValueText.ifBlank { "1" }
                            }
                            SummaryRow(
                                label = "Target",
                                value = targetSummaryDisplay,
                                testTag = "smart_summary_target"
                            )
                        }
                        if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                            SummaryRow(
                                label = "Unit",
                                value = unitCode.ifBlank { if (trackingMode == TrackingMode.DURATION) "min" else "units" },
                                testTag = "smart_summary_unit"
                            )
                        }
                        SummaryRow(
                            label = "Schedule",
                            value = when (scheduleKind) {
                                ScheduleKind.DAILY -> "Daily"
                                ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> "Weekly"
                                ScheduleKind.SELECTED_WEEKDAYS ->
                                    SmartHabitInterpretation.formatWeekdaysMask(WeekdayMask.toMask(selectedDays))
                            },
                            testTag = "smart_summary_schedule"
                        )
                    }
                }

                // Ambiguity notice when multiple numbers or decimal conversion occurred
                if (parsedInterpretation.needsUserConfirmation && !parsedInterpretation.ambiguityReason.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_habit_ambiguity_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = parsedInterpretation.ambiguityReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Editable Activity Name field when a natural-language sentence was parsed
                if (parsedInterpretation.extractedMeasurementFromPrompt ||
                    parsedInterpretation.extractedScheduleFromPrompt ||
                    userEditedActivityOverride
                ) {
                    OutlinedTextField(
                        value = interpretedActivityName,
                        onValueChange = {
                            interpretedActivityName = it
                            userEditedActivityOverride = true
                            nameError = null
                            onClearSaveError()
                        },
                        label = { Text("Activity display name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("smart_interpreted_activity_input")
                    )
                }

                // Measurement Mode Options (Simple Check-Off, Quantity, Duration, Amount / Currency, plus Legacy if editing)
                val creatableOptions = listOf(
                    Triple(
                        HabitMeasurementKind.SIMPLE_CHECK_OFF,
                        "Simple Check-Off",
                        "Simple done check-in"
                    ),
                    Triple(
                        HabitMeasurementKind.DURATION,
                        "Duration",
                        "Track minutes or hours toward a goal"
                    ),
                    Triple(
                        HabitMeasurementKind.QUANTITY,
                        "Quantity",
                        "Track any custom unit (pages, seeds, bags, animals, words) toward a goal"
                    ),
                    Triple(
                        HabitMeasurementKind.AMOUNT_CURRENCY,
                        "Amount / Currency",
                        "Track money saved or spent (e.g. ₦5,000 naira, $50)"
                    )
                )

                if (isEditing && initialHabit?.trackingMode?.canonical() == TrackingMode.EFFORT_AWARE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                userOverrodeMeasurement = true
                                trackingMode = TrackingMode.EFFORT_AWARE
                                targetValueError = null
                                onClearSaveError()
                            }
                            .testTag("mode_option_effort_aware"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = trackingMode == TrackingMode.EFFORT_AWARE,
                            onClick = {
                                userOverrodeMeasurement = true
                                trackingMode = TrackingMode.EFFORT_AWARE
                                targetValueError = null
                                onClearSaveError()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                "Effort-Aware (Legacy — convert recommended)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Historical records are preserved; choose Duration, Quantity, or Simple Check-Off to set a target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                for ((kind, title, sub) in creatableOptions) {
                    val tag = when (kind) {
                        HabitMeasurementKind.SIMPLE_CHECK_OFF -> "mode_option_completion_only"
                        HabitMeasurementKind.DURATION -> "mode_option_duration"
                        HabitMeasurementKind.QUANTITY -> "mode_option_quantity"
                        HabitMeasurementKind.AMOUNT_CURRENCY -> "mode_option_amount_currency"
                    }
                    val isSelected = trackingMode != TrackingMode.EFFORT_AWARE && selectedMeasurementKind == kind

                    val selectKindAction = {
                        userOverrodeMeasurement = true
                        selectedMeasurementKind = kind
                        when (kind) {
                            HabitMeasurementKind.SIMPLE_CHECK_OFF -> {
                                trackingMode = TrackingMode.COMPLETION_ONLY
                                if (targetPeriod == TargetPeriod.WEEK) {
                                    val currentVal = targetValueText.toIntOrNull() ?: 3
                                    if (currentVal !in 1..7) targetValueText = "3"
                                }
                            }
                            HabitMeasurementKind.DURATION -> {
                                trackingMode = TrackingMode.DURATION
                                if (!DurationNormalizer.isSupportedDurationUnit(unitCode)) {
                                    unitCode = "min"
                                    if (targetValueText.toIntOrNull() == null || targetValueText == "1") {
                                        targetValueText = "15"
                                    }
                                }
                            }
                            HabitMeasurementKind.QUANTITY -> {
                                trackingMode = TrackingMode.QUANTITY
                                if (unitCode.isBlank() || DurationNormalizer.isSupportedDurationUnit(unitCode) || UnitFormatter.isCurrencyUnit(unitCode)) {
                                    unitCode = "pages"
                                    if (targetValueText.toIntOrNull() == null || targetValueText == "1") {
                                        targetValueText = "10"
                                    }
                                }
                            }
                            HabitMeasurementKind.AMOUNT_CURRENCY -> {
                                trackingMode = TrackingMode.QUANTITY
                                if (!UnitFormatter.isCurrencyUnit(unitCode)) {
                                    unitCode = "naira"
                                    if (targetValueText.toIntOrNull() == null || targetValueText == "1" || targetValueText == "10") {
                                        targetValueText = "5000"
                                    }
                                }
                            }
                        }
                        targetValueError = null
                        onClearSaveError()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = selectKindAction)
                            .testTag(tag),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = selectKindAction
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (trackingMode == TrackingMode.DURATION) {
                    Text(
                        text = "Duration Unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (u in DurationNormalizer.supportedDurationUnits) {
                            FilterChip(
                                selected = unitCode == u,
                                onClick = {
                                    userOverrodeMeasurement = true
                                    unitCode = u
                                    onClearSaveError()
                                },
                                label = { Text(u) },
                                modifier = Modifier.testTag("unit_preset_$u")
                            )
                        }
                    }
                } else if (trackingMode == TrackingMode.QUANTITY) {
                    OutlinedTextField(
                        value = unitCode,
                        onValueChange = {
                            userOverrodeMeasurement = true
                            unitCode = it
                            onClearSaveError()
                        },
                        label = {
                            Text(
                                if (selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY) {
                                    "Currency or Amount Unit"
                                } else {
                                    "Unit (any custom word or phrase)"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                if (selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY) {
                                    "e.g. naira, dollars, pounds"
                                } else {
                                    "e.g. pages, seeds, handmade bags, animals"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("habit_unit_code_input")
                    )
                    val activePresets = if (selectedMeasurementKind == HabitMeasurementKind.AMOUNT_CURRENCY) {
                        currencyUnitPresets
                    } else {
                        quantityUnitPresets
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (preset in activePresets) {
                            FilterChip(
                                selected = unitCode.equals(preset, ignoreCase = true),
                                onClick = {
                                    userOverrodeMeasurement = true
                                    unitCode = preset
                                    onClearSaveError()
                                },
                                label = { Text(preset) },
                                modifier = Modifier.testTag("unit_preset_$preset")
                            )
                        }
                    }
                } else if (trackingMode == TrackingMode.EFFORT_AWARE) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("legacy_effort_conversion_banner")
                    ) {
                        Text(
                            text = "Dayseed now derives Tiny, Partial, Goal Met, Extra, and Huge progress automatically from your recorded Quantity or Duration against your target. Select Duration, Quantity, or Simple Check-Off above to update this habit while keeping all past history intact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                val showTargetValueField = trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) ||
                    targetPeriod == TargetPeriod.WEEK

                if (showTargetValueField) {
                    val targetFieldLabel = when {
                        trackingMode == TrackingMode.COMPLETION_ONLY && targetPeriod == TargetPeriod.WEEK ->
                            "Target Days per Week (1–7) *"
                        targetPeriod == TargetPeriod.WEEK ->
                            "Weekly Target Goal (${unitCode.ifBlank { "units" }}) *"
                        else ->
                            "Target Goal (${unitCode.ifBlank { "units" }}) *"
                    }

                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = {
                            userOverrodeMeasurement = true
                            targetValueText = it.filter { ch -> ch.isDigit() }
                            targetValueError = null
                            onClearSaveError()
                        },
                        label = { Text(targetFieldLabel) },
                        isError = targetValueError != null,
                        supportingText = {
                            if (targetValueError != null) {
                                Text(
                                    text = targetValueError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("habit_target_value_error")
                                )
                            } else if (targetPeriod == TargetPeriod.WEEK) {
                                Text(
                                    text = weeklyTargetSemanticsExplanation(trackingMode, unitCode),
                                    modifier = Modifier.testTag("weekly_target_semantics_helper")
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("habit_target_value_input")
                    )
                }
            }
        }

        // STEP 3: Simple Schedule Choice
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.DAILY,
                        onClick = {
                            userOverrodeSchedule = true
                            scheduleKind = ScheduleKind.DAILY
                            targetPeriod = TargetPeriod.DAY
                            selectedDays = DayOfWeek.values().toSet()
                            onClearSaveError()
                        },
                        label = { Text("Daily") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_daily_chip")
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.SELECTED_WEEKDAYS,
                        onClick = {
                            userOverrodeSchedule = true
                            scheduleKind = ScheduleKind.SELECTED_WEEKDAYS
                            targetPeriod = TargetPeriod.DAY
                            if (selectedDays.isEmpty()) {
                                selectedDays = DayOfWeek.values().toSet()
                            }
                            onClearSaveError()
                        },
                        label = { Text("Weekdays") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_weekdays_chip")
                    )
                    FilterChip(
                        selected = scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET,
                        onClick = {
                            userOverrodeSchedule = true
                            scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                            targetPeriod = TargetPeriod.WEEK
                            if (selectedDays.size == 7) {
                                selectedDays = emptySet()
                            }
                            if (trackingMode == TrackingMode.COMPLETION_ONLY) {
                                val v = targetValueText.toIntOrNull() ?: 3
                                if (v !in 1..7) targetValueText = "3"
                            }
                            onClearSaveError()
                        },
                        label = { Text("Flexible Week") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("schedule_kind_flexible_chip")
                    )
                }

                if (scheduleKind != ScheduleKind.DAILY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (scheduleKind == ScheduleKind.SELECTED_WEEKDAYS) {
                                "Select scheduled weekdays (unselected days are neutral rest days):"
                            } else {
                                "Preferred days (optional — leave all unselected for any day of the week):"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET && selectedDays.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    userOverrodeSchedule = true
                                    selectedDays = emptySet()
                                    onClearSaveError()
                                },
                                modifier = Modifier.testTag("clear_preferred_days_btn")
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val days = listOf(
                            DayOfWeek.MONDAY to "M",
                            DayOfWeek.TUESDAY to "T",
                            DayOfWeek.WEDNESDAY to "W",
                            DayOfWeek.THURSDAY to "T",
                            DayOfWeek.FRIDAY to "F",
                            DayOfWeek.SATURDAY to "S",
                            DayOfWeek.SUNDAY to "S"
                        )
                        for ((d, label) in days) {
                            val isSelected = selectedDays.contains(d)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    userOverrodeSchedule = true
                                    selectedDays = if (isSelected) {
                                        if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET || selectedDays.size > 1) {
                                            selectedDays - d
                                        } else {
                                            selectedDays
                                        }
                                    } else {
                                        selectedDays + d
                                    }
                                    onClearSaveError()
                                },
                                label = { Text(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weekday_chip_${d.name}")
                            )
                        }
                    }
                }
            }
        }

        // STEP 4: Simple Reminder Toggle (expands ODR-001/ODR-002 time controls only when enabled)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gentle Reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (suggestedReminderTime != null && !reminderEnabled) {
                            Text(
                                text = "Suggested from your habit rhythm: ${SettingsSupport.formatUnambiguousTimeDisplay(suggestedReminderTime, is24HourFormat)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it
                            onClearSaveError()
                        },
                        modifier = Modifier.testTag("habit_reminder_switch")
                    )
                }

                if (reminderEnabled) {
                    val resolvedReminderTime = remember(reminderTimeLocal, selectedPeriodIsPm) {
                        SettingsSupport.parseFlexibleLocalTime(reminderTimeLocal, fallbackPeriodIsPm = selectedPeriodIsPm)
                    }
                    val resolvedCanonicalTimeStr = remember(resolvedReminderTime, reminderTimeLocal) {
                        resolvedReminderTime?.let { SettingsSupport.formatQuietHours(it) } ?: reminderTimeLocal
                    }
                    val resolvedIsPm = resolvedReminderTime?.let { it.hour >= 12 } ?: selectedPeriodIsPm

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(reminderSectionBringIntoViewRequester),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (resolvedReminderTime != null) {
                                        "Selected time: ${SettingsSupport.formatUnambiguousTimeDisplay(resolvedCanonicalTimeStr, is24HourFormat)}"
                                    } else {
                                        "Enter a valid time (e.g. 8:30 AM, 4:00 PM, or 16:00)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("habit_reminder_resolved_time_preview")
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !resolvedIsPm,
                                onClick = {
                                    selectedPeriodIsPm = false
                                    reminderTimeLocal = SettingsSupport.switchTimeAmPm(
                                        reminderTimeLocal,
                                        toPm = false,
                                        fallback = LocalTime.of(9, 0)
                                    )
                                    reminderTimeError = null
                                    onClearSaveError()
                                },
                                label = { Text("AM (Morning)") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("habit_reminder_am_chip")
                            )
                            FilterChip(
                                selected = resolvedIsPm,
                                onClick = {
                                    selectedPeriodIsPm = true
                                    reminderTimeLocal = SettingsSupport.switchTimeAmPm(
                                        reminderTimeLocal,
                                        toPm = true,
                                        fallback = LocalTime.of(16, 0)
                                    )
                                    reminderTimeError = null
                                    onClearSaveError()
                                },
                                label = { Text("PM (Afternoon/Evening)") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("habit_reminder_pm_chip")
                            )
                            OutlinedButton(
                                onClick = {
                                    val next = SettingsSupport.stepQuietHours(
                                        resolvedCanonicalTimeStr,
                                        -30L,
                                        fallback = LocalTime.of(9, 0)
                                    )
                                    reminderTimeLocal = next
                                    selectedPeriodIsPm = SettingsSupport.isPmTime(next)
                                    reminderTimeError = null
                                    onClearSaveError()
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("habit_reminder_step_minus_30")
                            ) {
                                Text("−30m")
                            }
                            OutlinedButton(
                                onClick = {
                                    val next = SettingsSupport.stepQuietHours(
                                        resolvedCanonicalTimeStr,
                                        30L,
                                        fallback = LocalTime.of(9, 0)
                                    )
                                    reminderTimeLocal = next
                                    selectedPeriodIsPm = SettingsSupport.isPmTime(next)
                                    reminderTimeError = null
                                    onClearSaveError()
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("habit_reminder_step_plus_30")
                            ) {
                                Text("+30m")
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val quickReminderPresets = listOf(
                                Triple("0800", "08:00", "8:00 AM"),
                                Triple("1200", "12:00", "12:00 PM"),
                                Triple("1600", "16:00", "4:00 PM"),
                                Triple("2000", "20:00", "8:00 PM")
                            )
                            for ((tagSuffix, canonical24, label12) in quickReminderPresets) {
                                FilterChip(
                                    selected = resolvedCanonicalTimeStr == canonical24,
                                    onClick = {
                                        reminderTimeLocal = canonical24
                                        selectedPeriodIsPm = SettingsSupport.isPmTime(canonical24)
                                        reminderTimeError = null
                                        onClearSaveError()
                                    },
                                    label = { Text("$label12 ($canonical24)") },
                                    modifier = Modifier.testTag("habit_reminder_preset_$tagSuffix")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reminderTimeLocal,
                            onValueChange = {
                                reminderTimeLocal = it
                                val parsed = SettingsSupport.parseFlexibleLocalTime(it, fallbackPeriodIsPm = null)
                                if (parsed != null) {
                                    val trimmed = it.trim()
                                    val hasExplicitSuffix = trimmed.endsWith("AM", ignoreCase = true) ||
                                        trimmed.endsWith("PM", ignoreCase = true)
                                    val hasTwoDigitHour = trimmed.substringBefore(":").length >= 2
                                    if (hasExplicitSuffix || hasTwoDigitHour || parsed.hour >= 12 || parsed.hour == 0) {
                                        selectedPeriodIsPm = parsed.hour >= 12
                                    }
                                }
                                reminderTimeError = null
                                onClearSaveError()
                            },
                            label = { Text("Reminder Time (12h AM/PM or 24h HH:mm)") },
                            isError = reminderTimeError != null,
                            supportingText = {
                                if (reminderTimeError != null) {
                                    Text(
                                        text = reminderTimeError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("habit_reminder_time_error")
                                    )
                                } else if (resolvedReminderTime != null) {
                                    Text(
                                        text = "Saves as ${SettingsSupport.formatTime12Hour(resolvedReminderTime)} (${SettingsSupport.formatTime24Hour(resolvedReminderTime)} local time)"
                                    )
                                } else {
                                    Text("Examples: 8:30 AM, 4:00 PM, 08:30, or 16:00")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_reminder_time_input")
                        )

                        val fallsInsideQuietHours = resolvedReminderTime != null &&
                            ReminderEngine.isWithinQuietHours(
                                resolvedCanonicalTimeStr,
                                quietHoursStart,
                                quietHoursEnd
                            )

                        if (fallsInsideQuietHours && resolvedReminderTime != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("habit_reminder_quiet_hours_warning")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Falls inside Quiet Hours (${SettingsSupport.quietHoursUnambiguousSummary(quietHoursStart, quietHoursEnd, is24HourFormat)}): " +
                                            "${SettingsSupport.formatUnambiguousTimeDisplay(resolvedCanonicalTimeStr, is24HourFormat)} will be silenced during Quiet Hours.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (resolvedReminderTime.hour in 1..11) {
                                        val pmEquivalent = SettingsSupport.formatQuietHours(resolvedReminderTime.plusHours(12))
                                        OutlinedButton(
                                            onClick = {
                                                selectedPeriodIsPm = true
                                                reminderTimeLocal = pmEquivalent
                                                reminderTimeError = null
                                                onClearSaveError()
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("habit_reminder_switch_out_of_quiet_hours_btn")
                                        ) {
                                            Text("Switch to ${SettingsSupport.formatUnambiguousTimeDisplay(pmEquivalent, is24HourFormat)}")
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("habit_reminder_status_notice")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusMessage = when {
                                    !globalRemindersEnabled ->
                                        "Saved with this habit, but global reminder delivery is currently off in Settings → Gentle Reminders (pending device verification). No notification or sound will fire while global reminders are off."
                                    !hasPostNotificationsPermission || !isNotificationDeliveryAvailable ->
                                        "Android notification permission or the Gentle Reminders channel is currently blocked. Allow notifications so this reminder can alert you with sound/banner."
                                    else ->
                                        "Active for ${SettingsSupport.formatUnambiguousTimeDisplay(resolvedCanonicalTimeStr, is24HourFormat)} on the Gentle Reminders notification channel."
                                }
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if ((!hasPostNotificationsPermission || !isNotificationDeliveryAvailable) &&
                                    onRequestNotificationPermission != null
                                ) {
                                    OutlinedButton(
                                        onClick = onRequestNotificationPermission,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("habit_reminder_request_permission_btn")
                                    ) {
                                        Text(
                                            if (!hasPostNotificationsPermission) {
                                                "Allow Notifications"
                                            } else {
                                                "Open Notification Settings"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (hasMidWeekIncompatibleChange) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("u12_weekly_freeze_notice")
                    ) {
                        Text(
                            text = "Weekly rule protection (U-12): Your current week keeps its start-of-week unit and mode so already-logged sessions are never mixed with incompatible units. This unit/mode change applies cleanly from your next week.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isEditing && alreadyLoggedToday) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("d194_effective_date_prompt")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "You already logged this habit today",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "When should these updated habit settings take effect?",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = applyTodayWhenLogged,
                                    onClick = {
                                        applyTodayWhenLogged = true
                                        onClearSaveError()
                                    },
                                    label = { Text("Apply today") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("apply_today_chip")
                                )
                                FilterChip(
                                    selected = !applyTodayWhenLogged,
                                    onClick = {
                                        applyTodayWhenLogged = false
                                        onClearSaveError()
                                    },
                                    label = { Text("Apply tomorrow") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("apply_tomorrow_chip")
                                )
                            }
                        }
                    }
                }
            }
        }

        // STEP 5: Collapsible More Options / Advanced Settings (Category, Purpose, Lifetime Goal, Color, Target Period)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { showAdvancedOptions = !showAdvancedOptions }
                        .testTag("habit_advanced_options_toggle")
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "More Options",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Category, colour, purpose note & lifetime goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showAdvancedOptions) "Collapse more options" else "Expand more options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showAdvancedOptions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                onClearSaveError()
                            },
                            label = { Text("Purpose or note (optional)") },
                            placeholder = { Text("e.g. For peace of mind and clarity") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_desc_input"),
                            maxLines = 2
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = {
                                category = it
                                onClearSaveError()
                            },
                            label = { Text("Category (optional)") },
                            placeholder = { Text("e.g. Mindfulness, Movement, Reading") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_category_input"),
                            singleLine = true
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (cat in categorySuggestions) {
                                FilterChip(
                                    selected = category.equals(cat, ignoreCase = true),
                                    onClick = {
                                        category = if (category.equals(cat, ignoreCase = true)) "" else cat
                                        onClearSaveError()
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = lifetimeGoalText,
                            onValueChange = {
                                lifetimeGoalText = it.filter { ch -> ch.isDigit() }
                                lifetimeGoalError = null
                                onClearSaveError()
                            },
                            label = { Text("Lifetime goal active days (optional, 1–36500)") },
                            placeholder = { Text("e.g. 90 or 365") },
                            isError = lifetimeGoalError != null,
                            supportingText = {
                                if (lifetimeGoalError != null) {
                                    Text(lifetimeGoalError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("habit_lifetime_goal_input")
                        )

                        Text(
                            text = "Target Period",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = targetPeriod == TargetPeriod.DAY,
                                onClick = {
                                    userOverrodeSchedule = true
                                    targetPeriod = TargetPeriod.DAY
                                    if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                                        scheduleKind = ScheduleKind.DAILY
                                        if (selectedDays.isEmpty()) {
                                            selectedDays = DayOfWeek.values().toSet()
                                        }
                                    }
                                    targetValueError = null
                                    onClearSaveError()
                                },
                                label = { Text("Per Day") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("target_period_day_chip")
                            )
                            FilterChip(
                                selected = targetPeriod == TargetPeriod.WEEK,
                                onClick = {
                                    userOverrodeSchedule = true
                                    targetPeriod = TargetPeriod.WEEK
                                    scheduleKind = ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                                    if (selectedDays.size == 7) {
                                        selectedDays = emptySet()
                                    }
                                    if (trackingMode == TrackingMode.COMPLETION_ONLY) {
                                        val v = targetValueText.toIntOrNull() ?: 3
                                        if (v !in 1..7) targetValueText = "3"
                                    }
                                    targetValueError = null
                                    onClearSaveError()
                                },
                                label = { Text("Per Week") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("target_period_week_chip")
                            )
                        }

                        Text(
                            text = "Habit Color",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (hex in colorOptions) {
                                val c = try {
                                    Color(hex.toColorInt())
                                } catch (_: Exception) {
                                    Color.Green
                                }
                                val isPicked = selectedColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .clickable {
                                            selectedColor = hex
                                            onClearSaveError()
                                        }
                                        .then(
                                            if (isPicked) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!saveErrorMessage.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_save_error_banner")
            ) {
                Text(
                    text = saveErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Actions: Save, Cancel, and optional Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (initialHabit != null && onDelete != null) {
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("delete_habit_btn")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete habit",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    onClearSaveError()
                    val trimmedName = resolvedHabitName.trim()
                    if (trimmedName.isEmpty() || trimmedName.length > 80) {
                        nameError = "Please enter what you would like to do (1–80 characters)."
                        return@Button
                    }

                    val parsedLifetimeGoal = if (lifetimeGoalText.isBlank()) {
                        null
                    } else {
                        val goal = lifetimeGoalText.toIntOrNull()
                        if (goal == null || goal !in 1..36500) {
                            lifetimeGoalError = "Lifetime goal must be between 1 and 36,500 days."
                            showAdvancedOptions = true
                            return@Button
                        }
                        goal
                    }

                    val normalizedReminderTime = if (reminderEnabled) {
                        val parsedTime = SettingsSupport.parseFlexibleLocalTime(
                            reminderTimeLocal,
                            fallbackPeriodIsPm = selectedPeriodIsPm
                        )
                        if (parsedTime == null) {
                            reminderTimeError = "Enter a valid time in 12-hour (e.g. 4:00 PM) or 24-hour HH:mm format (00:00–23:59)."
                            return@Button
                        }
                        SettingsSupport.formatQuietHours(parsedTime)
                    } else {
                        null
                    }

                    val effectiveTargetPeriod = if (scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET) {
                        TargetPeriod.WEEK
                    } else {
                        targetPeriod
                    }

                    val targetVal = if (
                        trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) ||
                        effectiveTargetPeriod == TargetPeriod.WEEK
                    ) {
                        val parsed = targetValueText.toIntOrNull()
                        if (parsed == null || parsed < 1) {
                            targetValueError = "Please enter a positive target of at least 1."
                            return@Button
                        }
                        if (effectiveTargetPeriod == TargetPeriod.WEEK &&
                            (trackingMode == TrackingMode.COMPLETION_ONLY || trackingMode == TrackingMode.EFFORT_AWARE) &&
                            parsed !in 1..7
                        ) {
                            targetValueError = "Weekly check-off target must be between 1 and 7 days per week."
                            return@Button
                        }
                        parsed
                    } else {
                        null
                    }

                    val effectiveScheduleKind = if (effectiveTargetPeriod == TargetPeriod.WEEK) {
                        ScheduleKind.FLEXIBLE_WEEKLY_TARGET
                    } else {
                        scheduleKind
                    }

                    val mask = WeekdayMask.toMask(selectedDays)
                    val preferredMask = if (
                        effectiveScheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET &&
                        selectedDays.isNotEmpty() &&
                        selectedDays.size < 7
                    ) {
                        mask
                    } else {
                        null
                    }
                    val habit = Habit(
                        habitId = initialHabit?.habitId ?: UUID.randomUUID().toString(),
                        name = trimmedName,
                        description = description.trim(),
                        category = category.trim().ifBlank { null },
                        trackingMode = trackingMode,
                        unitCode = if (trackingMode in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION)) {
                            unitCode.trim().ifBlank { if (trackingMode == TrackingMode.DURATION) "min" else "units" }
                        } else {
                            null
                        },
                        targetPeriod = effectiveTargetPeriod,
                        targetValue = targetVal,
                        scheduleKind = effectiveScheduleKind,
                        scheduledWeekdaysMask = if (effectiveScheduleKind == ScheduleKind.SELECTED_WEEKDAYS) mask else null,
                        preferredWeekdaysMask = preferredMask,
                        lifecycleState = initialHabit?.lifecycleState ?: HabitLifecycleState.ACTIVE,
                        lifetimeGoalDays = parsedLifetimeGoal,
                        sortOrder = initialHabit?.sortOrder ?: 0,
                        reminderMode = if (reminderEnabled) "AT_TIME" else "OFF",
                        reminderTimeLocal = normalizedReminderTime,
                        tinyDescription = tinyDesc.trim(),
                        usualDescription = usualDesc.trim(),
                        extraDescription = extraDesc.trim(),
                        scheduleDays = if (effectiveScheduleKind == ScheduleKind.DAILY) {
                            DayOfWeek.values().toSet()
                        } else {
                            selectedDays
                        },
                        colorHex = selectedColor,
                        createdAtUtcMs = initialHabit?.createdAtUtcMs ?: System.currentTimeMillis()
                    )
                    onSave(habit, applyTodayWhenLogged)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("save_habit_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isEditing) "Save" else "Create Habit")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDeleteConfirmDialog && initialHabit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete \"${initialHabit.name}\" permanently?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This removes this habit, its configuration versions, and all logged sessions/outcomes. " +
                        "Any linked journal reflections will be kept safely on your device without the habit link. " +
                        "This action is permanent and cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(initialHabit.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_habit_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_habit_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("create_edit_delete_confirm_dialog")
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
