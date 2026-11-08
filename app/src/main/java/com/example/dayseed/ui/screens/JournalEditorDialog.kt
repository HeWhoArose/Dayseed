package com.example.dayseed.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.DailyPrompts
import com.example.dayseed.data.Habit
import com.example.dayseed.data.JournalDraft
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEditorDialog(
    initialEntry: JournalEntry?,
    initialDraft: JournalDraft? = null,
    selectedDate: LocalDate,
    habits: List<Habit>,
    initialLinkedHabitId: String? = null,
    initialPromptId: String? = null,
    initialCursorSelection: Pair<Int, Int>? = null,
    saveErrorMessage: String? = null,
    clockNowMs: () -> Long,
    onClearSaveError: () -> Unit = {},
    onAutosaveDraft: (
        draftId: String,
        title: String?,
        body: String,
        primaryMood: String?,
        secondaryMood: String?,
        promptId: String?,
        linkedHabitId: String?,
        date: LocalDate
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSaveDraftCursor: (draftId: String, start: Int, end: Int) -> Unit = { _, _, _ -> },
    onDiscardDraft: (draftId: String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (JournalEntry, String?) -> Unit,
    onDelete: (String) -> Unit
) {
    var entryDate by remember(initialEntry, initialDraft, selectedDate) {
        mutableStateOf(
            initialEntry?.date
                ?: initialDraft?.let { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
                ?: selectedDate
        )
    }

    val activeDraftId = remember(initialEntry, initialDraft, entryDate) {
        if (initialEntry != null) null else (initialDraft?.draftId ?: "active_draft_$entryDate")
    }

    val defaultPromptPair = remember(entryDate) { DailyPrompts.promptForDate(entryDate) }
    val resolvedPromptId = initialEntry?.promptId ?: initialDraft?.promptId ?: initialPromptId ?: defaultPromptPair.first
    val resolvedPromptText = remember(resolvedPromptId, defaultPromptPair) {
        val text = DailyPrompts.promptTextForId(resolvedPromptId)
        if (text.isNotBlank()) text else defaultPromptPair.second
    }

    val initialTitleText = initialEntry?.title ?: initialDraft?.title ?: ""
    val initialBodyText = initialEntry?.body ?: initialDraft?.body ?: ""
    val initialPrimaryMood = initialEntry?.primaryMoodCode ?: initialDraft?.primaryMoodCode
    val initialSecondaryMood = initialEntry?.secondaryMoodCode ?: initialDraft?.secondaryMoodCode
    val initialHabitLink = initialEntry?.linkedHabitId ?: initialDraft?.linkedHabitId ?: initialLinkedHabitId

    var title by rememberSaveable(initialEntry?.journalEntryId, initialDraft?.draftId) {
        mutableStateOf(initialTitleText)
    }

    var bodyFieldValue by rememberSaveable(
        initialEntry?.journalEntryId,
        initialDraft?.draftId,
        stateSaver = TextFieldValue.Saver
    ) {
        val len = initialBodyText.length
        val selStart = (initialCursorSelection?.first ?: len).coerceIn(0, len)
        val selEnd = (initialCursorSelection?.second ?: selStart).coerceIn(0, len)
        mutableStateOf(
            TextFieldValue(
                text = initialBodyText,
                selection = TextRange(selStart, selEnd)
            )
        )
    }

    var primaryMoodCode by rememberSaveable(initialEntry?.journalEntryId, initialDraft?.draftId) {
        mutableStateOf(initialPrimaryMood)
    }
    var secondaryMoodCode by rememberSaveable(initialEntry?.journalEntryId, initialDraft?.draftId) {
        mutableStateOf(initialSecondaryMood)
    }
    var linkedHabitId by rememberSaveable(initialEntry?.journalEntryId, initialDraft?.draftId) {
        mutableStateOf(initialHabitLink)
    }

    var contentError by remember { mutableStateOf(false) }
    var showDeleteEntryConfirm by remember { mutableStateOf(false) }
    var showDiscardDraftConfirm by remember { mutableStateOf(false) }
    var showUnsavedEditsCloseConfirm by remember { mutableStateOf(false) }

    val bodyText = bodyFieldValue.text

    // Autosave draft & cursor position whenever creating/editing a draft (not a saved entry)
    LaunchedEffect(
        title,
        bodyFieldValue.text,
        bodyFieldValue.selection.start,
        bodyFieldValue.selection.end,
        primaryMoodCode,
        secondaryMoodCode,
        linkedHabitId,
        entryDate
    ) {
        if (initialEntry == null && activeDraftId != null) {
            if (bodyText.isNotBlank() || title.isNotBlank() || primaryMoodCode != null) {
                onAutosaveDraft(
                    activeDraftId,
                    title.ifBlank { null },
                    bodyText,
                    primaryMoodCode,
                    secondaryMoodCode,
                    resolvedPromptId,
                    linkedHabitId,
                    entryDate
                )
                onSaveDraftCursor(
                    activeDraftId,
                    bodyFieldValue.selection.start,
                    bodyFieldValue.selection.end
                )
            }
        }
    }

    val hasUnsavedSavedEntryEdits = initialEntry != null && (
        title != (initialEntry.title ?: "") ||
            bodyText != initialEntry.body ||
            primaryMoodCode != initialEntry.primaryMoodCode ||
            secondaryMoodCode != initialEntry.secondaryMoodCode ||
            linkedHabitId != initialEntry.linkedHabitId ||
            entryDate.toString() != initialEntry.localDate
        )

    val handleRequestClose: () -> Unit = {
        if (hasUnsavedSavedEntryEdits) {
            showUnsavedEditsCloseConfirm = true
        } else {
            onDismiss()
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

    Dialog(
        onDismissRequest = handleRequestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 700.dp)
                .testTag("journal_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (initialEntry == null) "Daily Reflection" else "Edit Reflection",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (initialEntry != null) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = if (initialEntry != null) "Saved Entry" else "Draft",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("journal_editor_status_badge")
                                )
                            }
                        }

                        // Editable Date Selector (D-191)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { entryDate = entryDate.minusDays(1) },
                                modifier = Modifier.testTag("journal_date_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous date"
                                )
                            }
                            Text(
                                text = entryDate.format(dateFormatter),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("journal_date_label")
                            )
                            IconButton(
                                onClick = { entryDate = entryDate.plusDays(1) },
                                modifier = Modifier.testTag("journal_date_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next date"
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = handleRequestClose,
                        modifier = Modifier.testTag("journal_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Save Error Banner (Review 1 Amendment 1 & Section 4.1)
                if (!saveErrorMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_save_error_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = saveErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearSaveError) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                // Gentle Prompt Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "GENTLE PROMPT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resolvedPromptText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Optional Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("Give this moment a short title...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_title_input")
                )

                // Body Input with Cursor Restoration
                OutlinedTextField(
                    value = bodyFieldValue,
                    onValueChange = {
                        bodyFieldValue = it
                        if (it.text.isNotBlank()) {
                            contentError = false
                        }
                    },
                    isError = contentError,
                    label = { Text("Your Reflection") },
                    placeholder = {
                        Text("Write freely — even a single sentence is worth keeping...")
                    },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_content_input")
                )

                if (contentError) {
                    Text(
                        text = "Reflection body cannot be empty. Enter at least one non-whitespace character to save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("journal_body_error_text")
                    )
                }

                // Primary Mood Selector (D-096)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Primary Mood (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MoodCatalog.allMoods.forEach { mood ->
                            val isSelected = primaryMoodCode.equals(mood.code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        primaryMoodCode = null
                                        secondaryMoodCode = null
                                    } else {
                                        primaryMoodCode = mood.code
                                        if (secondaryMoodCode.equals(mood.code, ignoreCase = true)) {
                                            secondaryMoodCode = null
                                        }
                                    }
                                },
                                label = { Text("${mood.symbol} ${mood.label}") },
                                modifier = Modifier.testTag("primary_mood_${mood.code}")
                            )
                        }
                    }
                }

                // Secondary Mood Selector (Only enabled when Primary Mood is selected)
                if (primaryMoodCode != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Secondary Mood (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MoodCatalog.allMoods
                                .filter { !it.code.equals(primaryMoodCode, ignoreCase = true) }
                                .forEach { mood ->
                                    val isSelected = secondaryMoodCode.equals(mood.code, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            secondaryMoodCode = if (isSelected) null else mood.code
                                        },
                                        label = { Text("${mood.symbol} ${mood.label}") },
                                        modifier = Modifier.testTag("secondary_mood_${mood.code}")
                                    )
                                }
                        }
                    }
                }

                // Linked Habit Selector (Optional)
                if (habits.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Linked Habit (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = linkedHabitId == null,
                                onClick = { linkedHabitId = null },
                                label = { Text("None") },
                                modifier = Modifier.testTag("journal_linked_habit_NONE")
                            )
                            habits.filter { !it.isArchived }.forEach { h ->
                                FilterChip(
                                    selected = linkedHabitId == h.habitId,
                                    onClick = {
                                        linkedHabitId = if (linkedHabitId == h.habitId) null else h.habitId
                                    },
                                    label = { Text(h.name) },
                                    modifier = Modifier.testTag("journal_linked_habit_${h.habitId}")
                                )
                            }
                        }
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialEntry != null) {
                        TextButton(
                            onClick = { showDeleteEntryConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("journal_delete_entry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    } else if (activeDraftId != null && (initialDraft != null || bodyText.isNotBlank() || title.isNotBlank())) {
                        TextButton(
                            onClick = { showDiscardDraftConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("discard_journal_draft_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discard Draft")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = handleRequestClose,
                            modifier = Modifier.testTag("journal_cancel_button")
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = {
                                if (bodyText.isBlank()) {
                                    contentError = true
                                    return@Button
                                }
                                contentError = false
                                val nowMs = clockNowMs()
                                if (initialEntry == null && activeDraftId != null) {
                                    // Ensure latest draft content is flushed before promotion
                                    onAutosaveDraft(
                                        activeDraftId,
                                        title.trim().ifBlank { null },
                                        bodyText,
                                        primaryMoodCode,
                                        secondaryMoodCode,
                                        resolvedPromptId,
                                        linkedHabitId,
                                        entryDate
                                    )
                                }
                                val entry = JournalEntry(
                                    journalEntryId = initialEntry?.journalEntryId
                                        ?: initialDraft?.intendedJournalEntryId
                                        ?: UUID.randomUUID().toString(),
                                    localDate = entryDate.toString(),
                                    body = bodyText.trim(),
                                    title = title.trim().ifBlank { null },
                                    primaryMoodCode = primaryMoodCode,
                                    secondaryMoodCode = if (primaryMoodCode != null && secondaryMoodCode != primaryMoodCode) secondaryMoodCode else null,
                                    promptId = resolvedPromptId,
                                    linkedHabitId = linkedHabitId,
                                    createdAtUtcMs = initialEntry?.createdAtUtcMs ?: nowMs,
                                    updatedAtUtcMs = nowMs
                                )
                                onSave(entry, activeDraftId)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("journal_save_button")
                        ) {
                            Text("Save Reflection")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteEntryConfirm && initialEntry != null) {
        AlertDialog(
            onDismissRequest = { showDeleteEntryConfirm = false },
            modifier = Modifier.testTag("confirm_delete_journal_dialog"),
            title = { Text("Delete Reflection?") },
            text = {
                Text("Delete this entry? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteEntryConfirm = false
                        onDelete(initialEntry.journalEntryId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_journal_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteEntryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardDraftConfirm && activeDraftId != null) {
        AlertDialog(
            onDismissRequest = { showDiscardDraftConfirm = false },
            modifier = Modifier.testTag("confirm_discard_draft_dialog"),
            title = { Text("Discard Draft?") },
            text = {
                Text("Discard unsaved draft? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDraftConfirm = false
                        onDiscardDraft(activeDraftId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_discard_draft_btn")
                ) {
                    Text("Discard Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDraftConfirm = false }) {
                    Text("Keep Writing")
                }
            }
        )
    }

    if (showUnsavedEditsCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedEditsCloseConfirm = false },
            title = { Text("Discard Unsaved Edits?") },
            text = {
                Text("You have unsaved changes to this saved reflection. Closing now will keep the previously saved version unchanged.")
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showUnsavedEditsCloseConfirm = false
                        onDismiss()
                    },
                    modifier = Modifier.testTag("journal_confirm_discard_edits_button")
                ) {
                    Text("Discard Changes")
                }
            },
            dismissButton = {
                Button(onClick = { showUnsavedEditsCloseConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
