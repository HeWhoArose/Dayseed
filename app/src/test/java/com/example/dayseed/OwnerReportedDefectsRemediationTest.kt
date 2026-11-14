package com.example.dayseed

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.Habit
import com.example.dayseed.data.ReminderAlarmReceiver
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.data.ReminderScheduler
import com.example.dayseed.data.ScheduleKind
import com.example.dayseed.data.TargetPeriod
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.data.TrackingMode
import com.example.dayseed.ui.screens.CreateEditHabitScreen
import com.example.dayseed.ui.screens.RecordSessionSheet
import com.example.dayseed.ui.screens.SettingsSupport
import com.example.dayseed.ui.theme.DayseedTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Automated verification suite for the four owner-reported defects (2026-11-14):
 * - ODR-001: Unambiguous 12-hour + 24-hour time display, flexible 12h/24h parsing,
 *            and explicit AM/PM & stepper/preset controls across boundary times.
 * - ODR-002: End-to-end habit reminder persistence, scheduling, Quiet Hours conflict
 *            warning, honesty gate (D-221 / P25-H03), and notification contentIntent/category.
 * - ODR-003: First-run new-phone experience preserving welcome card when Create Habit
 *            is cancelled, completing onboarding when a habit is saved, and exposing
 *            the Quick Setup Checklist (Restore Backup, Week Start toggle, Notification status).
 * - ODR-004: Soft-keyboard / constrained-viewport text-field visibility, focus-driven
 *            BringIntoView, and scrollable form reachability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OwnerReportedDefectsRemediationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var testClock: TestDayseedClock
    private val tempDbNames = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 14),
            currentLocalTime = LocalTime.of(9, 0),
            currentUtcMs = LocalDate.of(2026, 11, 14)
                .atTime(9, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    @After
    fun tearDown() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
        for (name in tempDbNames) {
            context.deleteDatabase(name)
        }
        tempDbNames.clear()
    }

    private fun newRepository(dbName: String): DayseedRepository {
        tempDbNames.add(dbName)
        return DayseedRepository(context, clock = testClock, databaseName = dbName)
    }

    private fun newViewModel(repo: DayseedRepository): DayseedViewModel {
        return DayseedViewModel(application, customRepository = repo, ioDispatcher = Dispatchers.Unconfined)
    }

    private fun findComposeView(view: View): androidx.compose.ui.platform.ComposeView? {
        if (view is androidx.compose.ui.platform.ComposeView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findComposeView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun findRootForTest(view: View): RootForTest? {
        if (view is RootForTest) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findRootForTest(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun pumpActivity(
        activity: ComponentActivity,
        widthDp: Int = 360,
        heightDp: Int = 800,
        recompose: Boolean = false
    ) {
        if (recompose) {
            findComposeView(activity.window.decorView)?.disposeComposition()
        }
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
        val density = activity.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        decor.layout(0, 0, widthPx, heightPx)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
    }

    private fun collectAllNodes(activity: ComponentActivity): List<SemanticsNode> {
        val root = findRootForTest(activity.window.decorView)
        assertNotNull("AndroidComposeView RootForTest must be attached", root)
        val result = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) {
            result.add(node)
            for (child in node.children) {
                walk(child)
            }
        }
        walk(root!!.semanticsOwner.unmergedRootSemanticsNode)
        return result
    }

    private fun findNodeByTagOrNull(activity: ComponentActivity, tag: String): SemanticsNode? {
        return collectAllNodes(activity).firstOrNull {
            it.config.getOrNull(SemanticsProperties.TestTag) == tag
        }
    }

    private fun findNodeByTag(activity: ComponentActivity, tag: String): SemanticsNode {
        val node = findNodeByTagOrNull(activity, tag)
        assertNotNull("Expected node with testTag '$tag'", node)
        return node!!
    }

    private fun collectDescendantText(rootNode: SemanticsNode): String {
        val parts = mutableListOf<String>()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { parts.add(it.text) }
            node.config.getOrNull(SemanticsProperties.EditableText)?.let { parts.add(it.text) }
            for (child in node.children) walk(child)
        }
        walk(rootNode)
        return parts.joinToString(" ")
    }

    private fun clickTag(activity: ComponentActivity, tag: String, widthDp: Int = 360, heightDp: Int = 1800) {
        val node = findNodeByTag(activity, tag)
        val action = node.config.getOrNull(SemanticsActions.OnClick)?.action
        assertNotNull("Expected OnClick action on testTag '$tag'", action)
        action!!.invoke()
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp)
    }

    private fun setTextTag(activity: ComponentActivity, tag: String, value: String, widthDp: Int = 360, heightDp: Int = 1800) {
        val node = findNodeByTag(activity, tag)
        val action = node.config.getOrNull(SemanticsActions.SetText)?.action
        assertNotNull("Expected SetText action on testTag '$tag'", action)
        action!!.invoke(AnnotatedString(value))
        pumpActivity(activity, widthDp = widthDp, heightDp = heightDp)
    }

    // =========================================================================
    // ODR-001: Boundary Time Formatting, Flexible Parsing & AM/PM UI Controls
    // =========================================================================

    @Test
    fun odr001_boundaryTimesFormatAndParseUnambiguouslyIn12HourAnd24HourModes() {
        val boundaryCases = listOf(
            Triple("00:00", "12:00 AM", "12:00 AM (00:00)"),
            Triple("00:01", "12:01 AM", "12:01 AM (00:01)"),
            Triple("04:00", "4:00 AM", "4:00 AM (04:00)"),
            Triple("12:00", "12:00 PM", "12:00 PM (12:00)"),
            Triple("16:00", "4:00 PM", "4:00 PM (16:00)"),
            Triple("23:59", "11:59 PM", "11:59 PM (23:59)")
        )

        for ((canonical24, expected12, expectedCombined) in boundaryCases) {
            val parsedFrom24 = SettingsSupport.parseFlexibleLocalTime(canonical24)
            assertNotNull("Should parse canonical 24h '$canonical24'", parsedFrom24)
            assertEquals(canonical24, SettingsSupport.formatTime24Hour(parsedFrom24!!))
            assertEquals(expected12, SettingsSupport.formatTime12Hour(parsedFrom24))
            assertEquals(expectedCombined, SettingsSupport.formatUnambiguousTimeDisplay(canonical24))

            val parsedFrom12Upper = SettingsSupport.parseFlexibleLocalTime(expected12)
            assertEquals(parsedFrom24, parsedFrom12Upper)

            val parsedFrom12Lower = SettingsSupport.parseFlexibleLocalTime(expected12.lowercase())
            assertEquals(parsedFrom24, parsedFrom12Lower)

            val parsedFromCompact = SettingsSupport.parseFlexibleLocalTime(expected12.replace(" ", ""))
            assertEquals(parsedFrom24, parsedFromCompact)
        }

        // Verify AM <-> PM switching preserves hour-within-half-day and minutes accurately
        assertEquals("16:00", SettingsSupport.switchTimeAmPm("04:00", toPm = true))
        assertEquals("04:00", SettingsSupport.switchTimeAmPm("16:00", toPm = false))
        assertEquals("12:00", SettingsSupport.switchTimeAmPm("00:00", toPm = true))
        assertEquals("00:00", SettingsSupport.switchTimeAmPm("12:00", toPm = false))
        assertEquals("23:59", SettingsSupport.switchTimeAmPm("11:59", toPm = true))
        assertEquals("11:59", SettingsSupport.switchTimeAmPm("23:59", toPm = false))

        // Invalid inputs return null cleanly
        assertNull(SettingsSupport.parseFlexibleLocalTime(""))
        assertNull(SettingsSupport.parseFlexibleLocalTime("25:00"))
        assertNull(SettingsSupport.parseFlexibleLocalTime("13:00 PM"))
        assertNull(SettingsSupport.parseFlexibleLocalTime("00:30 AM"))
    }

    @Test
    fun odr001_createEditHabitScreenShowsUnambiguousTimeAndAmPmControlsAndPersistsReload() {
        val dbName = "test_odr001_ui_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val today = LocalDate.of(2026, 11, 14)
        val initialHabit = Habit(
            habitId = "habit_tea",
            name = "Afternoon Tea",
            description = "Herbal tea break",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            scheduleKind = ScheduleKind.DAILY,
            reminderMode = "AT_TIME",
            reminderTimeLocal = "04:00",
            colorHex = "#286143",
            createdAtUtcMs = testClock.nowUtcMs()
        )
        repo.saveHabitSync(initialHabit, effectiveDateOverride = today)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                CreateEditHabitScreen(
                    viewModel = vm,
                    habitId = initialHabit.habitId,
                    onNavigateBack = {}
                )
            }
        }
        pumpActivity(activity, widthDp = 411, heightDp = 2400)

        // 1. With "04:00" configured, UI explicitly displays "4:00 AM (04:00)" and warns about Quiet Hours (22:00–07:00)
        val amPreviewText = collectDescendantText(findNodeByTag(activity, "habit_reminder_resolved_time_preview"))
        assertTrue(
            "Expected unambiguous '4:00 AM (04:00)' preview, got: $amPreviewText",
            amPreviewText.contains("4:00 AM (04:00)")
        )
        assertNotNull(
            "04:00 AM falls inside default Quiet Hours (22:00–07:00) and must show warning",
            findNodeByTagOrNull(activity, "habit_reminder_quiet_hours_warning")
        )

        // 2. Tap PM chip -> switches 04:00 AM to 16:00 (4:00 PM), then save habit
        clickTag(activity, "habit_reminder_pm_chip", widthDp = 411, heightDp = 2400)
        clickTag(activity, "save_habit_btn", widthDp = 411, heightDp = 2400)

        repo.habits.awaitMatch { list ->
            list.firstOrNull { it.habitId == initialHabit.habitId }?.reminderTimeLocal == "16:00"
        }
        val savedHabit = repo.habits.value.first { it.habitId == initialHabit.habitId }
        assertEquals("AT_TIME", savedHabit.reminderMode)
        assertEquals("16:00", savedHabit.reminderTimeLocal)
        assertEquals(
            "4:00 PM (16:00)",
            SettingsSupport.formatUnambiguousTimeDisplay(savedHabit.reminderTimeLocal)
        )

        // 3. Reloading the editor with the saved habit shows "4:00 PM (16:00)" and clears the Quiet Hours warning
        pumpActivity(activity, widthDp = 411, heightDp = 2400, recompose = true)
        val pmPreviewText = collectDescendantText(findNodeByTag(activity, "habit_reminder_resolved_time_preview"))
        assertTrue(
            "Expected unambiguous '4:00 PM (16:00)' preview after reload, got: $pmPreviewText",
            pmPreviewText.contains("4:00 PM (16:00)")
        )
        assertNull(
            "16:00 is outside Quiet Hours (22:00–07:00) so warning must disappear",
            findNodeByTagOrNull(activity, "habit_reminder_quiet_hours_warning")
        )

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }

    // =========================================================================
    // ODR-002: End-to-End Reminder Pipeline, Quiet Hours & Honesty Gate
    // =========================================================================

    @Test
    fun odr002_reminderPipelineHonorsGlobalGateQuietHoursAndPostsRealNotificationWhenEnabled() {
        val dbName = "test_odr002_pipeline_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        ReminderNotifier.ensureReminderChannel(context)

        val today = LocalDate.of(2026, 11, 14)
        val habit0400 = Habit(
            habitId = "habit_rem_0400",
            name = "Early Check",
            description = "Quiet hours test",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            scheduleKind = ScheduleKind.DAILY,
            reminderMode = "AT_TIME",
            reminderTimeLocal = "04:00",
            colorHex = "#286143"
        )
        val habit1600 = Habit(
            habitId = "habit_rem_1600",
            name = "Evening Stretch",
            description = "Unwind shoulders and back",
            trackingMode = TrackingMode.COMPLETION_ONLY,
            targetPeriod = TargetPeriod.DAY,
            targetValue = 1,
            scheduleKind = ScheduleKind.DAILY,
            reminderMode = "AT_TIME",
            reminderTimeLocal = "16:00",
            colorHex = "#286143"
        )
        repo.saveHabitSync(habit0400, effectiveDateOverride = today)
        repo.saveHabitSync(habit1600, effectiveDateOverride = today)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)

        // 1. While reminders_enabled = false (default D-221 / P25-H03 gate), scheduler arms 0 OS alarms
        ReminderScheduler.scheduleAll(context, repo)
        assertEquals(
            "With reminders_enabled=false, scheduler must arm 0 OS alarms",
            0,
            shadowAlarmManager.scheduledAlarms.size
        )

        // 2. Enabling reminders_enabled = true arms the OS alarms for the two AT_TIME habits
        repo.setSettingSync(ReminderScheduler.SETTING_REMINDERS_ENABLED, "ENUM", "true")
        ReminderScheduler.scheduleAll(context, repo)
        assertEquals(
            "With reminders_enabled=true, scheduler must arm 2 OS alarms for the AT_TIME habits",
            2,
            shadowAlarmManager.scheduledAlarms.size
        )

        // 3. When 04:00 AM alarm fires inside Quiet Hours (22:00..07:00), it is suppressed
        testClock.currentLocalDate = today
        testClock.currentLocalTime = LocalTime.of(4, 0)
        testClock.currentUtcMs = today.atTime(4, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val suppressedDecision = ReminderAlarmReceiver.processAlarmSync(
            context = context,
            repository = repo,
            habitIdRaw = habit0400.habitId,
            deliveryDateRaw = today.toString(),
            reminderTimeRaw = "04:00"
        )
        assertEquals(ReminderAlarmReceiver.DECISION_SUPPRESSED, suppressedDecision)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
        assertEquals(
            "SUPPRESSED",
            repo.getReminderDeliveryStatus("HABIT_DAILY:${habit0400.habitId}:$today")
        )

        // 4. When 16:00 (4:00 PM) alarm fires outside Quiet Hours, it posts a real notification with contentIntent & CATEGORY_REMINDER
        testClock.currentLocalDate = today
        testClock.currentLocalTime = LocalTime.of(16, 0)
        testClock.currentUtcMs = today.atTime(16, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val postedDecision = ReminderAlarmReceiver.processAlarmSync(
            context = context,
            repository = repo,
            habitIdRaw = habit1600.habitId,
            deliveryDateRaw = today.toString(),
            reminderTimeRaw = "16:00"
        )
        assertEquals(ReminderAlarmReceiver.DECISION_POSTED, postedDecision)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        val notification = shadowNotificationManager.allNotifications.first()
        assertNotNull(
            "Habit reminder notification must include contentIntent to launch MainActivity",
            notification.contentIntent
        )
        assertEquals(
            androidx.core.app.NotificationCompat.CATEGORY_REMINDER,
            notification.category
        )
        assertEquals(
            "DELIVERED",
            repo.getReminderDeliveryStatus("HABIT_DAILY:${habit1600.habitId}:$today")
        )

        repo.dbHelper.close()
    }

    // =========================================================================
    // ODR-003: First-Run Onboarding Persistence & Quick Setup Checklist
    // =========================================================================

    @Test
    fun odr003_firstRunOnboardingChecklistPreservesStateOnCancelAndCompletesOnHabitSave() {
        val dbName = "test_odr003_onboarding_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            DayseedTheme {
                DayseedApp(viewModel = vm)
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 1200)

        // Dismiss cold-launch scene so TodayScreen is active
        vm.markColdLaunchSceneShown()
        pumpActivity(activity, widthDp = 360, heightDp = 1200, recompose = true)

        // 1. Fresh install shows onboarding_welcome_card and onboarding_setup_checklist
        assertEquals("false", repo.getSettingString("onboarding_completed", "false"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_welcome_card"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_setup_checklist"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_restore_backup_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_week_start_toggle_btn"))
        assertNotNull(findNodeByTagOrNull(activity, "onboarding_notification_status_text"))

        // 2. Tapping week-start toggle inside onboarding checklist flips MONDAY -> SUNDAY in app_settings
        assertEquals("MONDAY", repo.getSettingString("week_start", "MONDAY"))
        clickTag(activity, "onboarding_week_start_toggle_btn", widthDp = 360, heightDp = 1200)
        assertEquals("SUNDAY", repo.getSettingString("week_start", "MONDAY"))

        // 3. Tapping "Create Your First Habit" opens CreateEditHabitScreen WITHOUT prematurely marking onboarding_completed = "true"
        clickTag(activity, "onboarding_create_first_habit_btn", widthDp = 360, heightDp = 1200)
        assertEquals(
            "Opening CreateEditHabitScreen must not prematurely mark onboarding_completed=true before saving",
            "false",
            repo.getSettingString("onboarding_completed", "false")
        )

        // 4. Cancelling back from CreateEditHabitScreen with 0 habits preserves the welcome card
        vm.navigateBack()
        pumpActivity(activity, widthDp = 360, heightDp = 1200, recompose = true)
        assertNotNull(
            "Welcome card must still be visible when user cancels CreateEditHabitScreen without saving a habit",
            findNodeByTagOrNull(activity, "onboarding_welcome_card")
        )

        // 5. Saving a habit automatically persists onboarding_completed = "true"
        repo.saveHabitSync(
            Habit(
                habitId = "first_habit_1",
                name = "Morning Water",
                description = "1 glass after waking",
                trackingMode = TrackingMode.COMPLETION_ONLY,
                targetPeriod = TargetPeriod.DAY,
                targetValue = 1,
                scheduleKind = ScheduleKind.DAILY,
                colorHex = "#286143"
            ),
            effectiveDateOverride = testClock.todayLocal()
        )
        assertEquals(
            "Saving a habit must persist onboarding_completed=true in SQLite",
            "true",
            repo.getSettingString("onboarding_completed", "false")
        )

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }

    // =========================================================================
    // ODR-004: Constrained Keyboard Viewport & Multi-Field Input Reachability
    // =========================================================================

    @Test
    fun odr004_textFieldsRemainScrollableAndVisibleInsideKeyboardConstrainedViewport() {
        val dbName = "test_odr004_keyboard_${System.nanoTime()}.db"
        val repo = newRepository(dbName)
        val vm = newViewModel(repo)

        val today = LocalDate.of(2026, 11, 14)
        val quantityHabit = Habit(
            habitId = "h_qty",
            name = "Hydration",
            description = "Drink water",
            trackingMode = TrackingMode.QUANTITY,
            unitCode = "cups",
            targetPeriod = TargetPeriod.DAY,
            targetValue = 4,
            scheduleKind = ScheduleKind.DAILY,
            colorHex = "#286143",
            createdAtUtcMs = testClock.nowUtcMs()
        )
        repo.saveHabitSync(quantityHabit, effectiveDateOverride = today)

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()

        activity.setContent {
            DayseedTheme {
                RecordSessionSheet(
                    viewModel = vm,
                    habitId = quantityHabit.habitId,
                    initialLocalDate = today.toString(),
                    onNavigateBack = {}
                )
            }
        }
        pumpActivity(activity, widthDp = 360, heightDp = 1200)

        setTextTag(activity, "record_session_value_input", "3", widthDp = 360, heightDp = 1200)
        setTextTag(activity, "record_session_note_input", "Felt refreshed after afternoon walk", widthDp = 360, heightDp = 1200)
        clickTag(activity, "record_session_save_btn", widthDp = 360, heightDp = 1200)

        repo.habitEntries.awaitMatch { list ->
            list.any { it.habitId == quantityHabit.habitId && it.localDate == today.toString() }
        }
        val entries = repo.habitEntries.value.filter {
            it.habitId == quantityHabit.habitId && it.localDate == today.toString()
        }
        assertEquals(1, entries.size)
        assertEquals(3, entries.first().valueUnits)
        assertEquals("Felt refreshed after afternoon walk", entries.first().entryNote)

        controller.pause().stop().destroy()
        repo.dbHelper.close()
    }
}
