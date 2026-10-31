package com.example.dayseed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.DailyPrompts
import com.example.dayseed.data.Habit
import com.example.dayseed.data.JournalDraft
import com.example.dayseed.data.JournalEntry
import com.example.dayseed.data.MoodCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalEditorDialog(
    initialEntry: JournalEntry?,
    initialDraft: JournalDraft? = null,
    selectedDate: LocalDate = LocalDate.now(),
    habits: List<Habit> = emptyList(),
    initialLinkedHabitId: String? = null,
    initialPromptId: String? = null,
    onAutosaveDraft: ((title: String?, body: String, primaryMood: String?, secondaryMood: String?, promptId: String?, linkedHabitId: String?, date: LocalDate) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (JournalEntry) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = initialEntry != null
    var targetDateText by remember {
        mutableStateOf((initialEntry?.date ?: selectedDate).toString())
    }
    var title by remember {
        mutableStateOf(initialEntry?.title ?: initialDraft?.title ?: "")
    }
    var promptId by remember {
        mutableStateOf(
            initialEntry?.promptId
                ?: initialDraft?.promptId
                ?: initialPromptId
                ?: DailyPrompts.promptForDate(selectedDate).first
        )
    }
    var content by remember {
        mutableStateOf(initialEntry?.body ?: initialDraft?.body ?: "")
    }
    var primaryMoodCode by remember {
        mutableStateOf(initialEntry?.primaryMoodCode ?: initialDraft?.primaryMoodCode)
    }
    var secondaryMoodCode by remember {
        mutableStateOf(initialEntry?.secondaryMoodCode ?: initialDraft?.secondaryMoodCode)
    }
    var linkedHabitId by remember {
        mutableStateOf(initialEntry?.linkedHabitId ?: initialDraft?.linkedHabitId ?: initialLinkedHabitId)
    }
    var showExpandedMoods by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    val activeDate = remember(targetDateText) {
        try {
            LocalDate.parse(targetDateText)
        } catch (_: Exception) {
            selectedDate
        }
    }

    fun triggerAutosave(
        newTitle: String = title,
        newBody: String = content,
        newPrimary: String? = primaryMoodCode,
        newSecondary: String? = secondaryMoodCode,
        newPromptId: String? = promptId,
        newLinkedHabit: String? = linkedHabitId
    ) {
        if (!isEditing && onAutosaveDraft != null) {
            onAutosaveDraft(
                newTitle.takeIf { it.isNotBlank() },
                newBody,
                newPrimary,
                newSecondary,
                newPromptId,
                newLinkedHabit,
                activeDate
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("journal_editor_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Reflection" else "Daily Reflection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date field (allows date correction without mutating other records)
                OutlinedTextField(
                    value = targetDateText,
                    onValueChange = { targetDateText = it },
                    label = { Text("Entry Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_date_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        triggerAutosave(newTitle = it)
                    },
                    label = { Text("Title (optional)") },
                    placeholder = { Text("A headline for today's reflection") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mood Selection (Primary + Optional Secondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mood (Primary & Optional Secondary)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = { showExpandedMoods = !showExpandedMoods },
                        label = { Text(if (showExpandedMoods) "Starter 10" else "More Moods") }
                    )
                }

                val moodsToShow = if (showExpandedMoods) MoodCatalog.allMoods else MoodCatalog.starterMoods
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (mood in moodsToShow) {
                        val isPrimary = primaryMoodCode == mood.code
                        val isSecondary = secondaryMoodCode == mood.code
                        FilterChip(
                            selected = isPrimary || isSecondary,
                            onClick = {
                                when {
                                    isPrimary -> {
                                        primaryMoodCode = secondaryMoodCode
                                        secondaryMoodCode = null
                                    }
                                    isSecondary -> {
                                        secondaryMoodCode = null
                                    }
                                    primaryMoodCode == null -> {
                                        primaryMoodCode = mood.code
                                    }
                                    else -> {
                                        secondaryMoodCode = mood.code
                                    }
                                }
                                triggerAutosave(newPrimary = primaryMoodCode, newSecondary = secondaryMoodCode)
                            },
                            label = {
                                val suffix = when {
                                    isPrimary -> " (1st)"
                                    isSecondary -> " (2nd)"
                                    else -> ""
                                }
                                Text("${mood.symbol} ${mood.label}$suffix", fontSize = 12.sp)
                            },
                            modifier = Modifier.testTag("mood_chip_${mood.code.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Linked Habit
                if (habits.isNotEmpty()) {
                    Text(
                        text = "Link to Habit (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (h in habits) {
                            val isLinked = linkedHabitId == h.habitId
                            FilterChip(
                                selected = isLinked,
                                onClick = {
                                    linkedHabitId = if (isLinked) null else h.habitId
                                    triggerAutosave(newLinkedHabit = linkedHabitId)
                                },
                                label = { Text(h.name, fontSize = 12.sp) },
                                modifier = Modifier.testTag("link_habit_chip_${h.habitId}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Prompt selection chips
                Text(
                    text = "Optional Reflection Prompt",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((pId, pText) in DailyPrompts.prompts.take(4)) {
                        SuggestionChip(
                            onClick = {
                                promptId = pId
                                triggerAutosave(newPromptId = pId)
                            },
                            label = {
                                Text(
                                    text = pText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp
                                )
                            },
                            icon = {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Journal content
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (it.isNotBlank()) contentError = false
                        triggerAutosave(newBody = it)
                    },
                    label = { Text("Write your thoughts...") },
                    placeholder = { Text("Even one sentence is meaningful. Drafts autosave locally.") },
                    isError = contentError,
                    supportingText = {
                        val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                        Text(if (contentError) "Please write something before saving" else "$wordCount words • Local autosave active")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("journal_content_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialEntry != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialEntry.id) },
                            modifier = Modifier.testTag("delete_journal_btn")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete reflection",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            if (content.isBlank()) {
                                contentError = true
                            } else {
                                val entry = JournalEntry(
                                    journalEntryId = initialEntry?.journalEntryId ?: UUID.randomUUID().toString(),
                                    localDate = activeDate.toString(),
                                    body = content.trim(),
                                    title = title.trim().takeIf { it.isNotEmpty() },
                                    primaryMoodCode = primaryMoodCode,
                                    secondaryMoodCode = if (primaryMoodCode != null && secondaryMoodCode != primaryMoodCode) secondaryMoodCode else null,
                                    promptId = promptId,
                                    linkedHabitId = linkedHabitId,
                                    createdAtUtcMs = initialEntry?.createdAtUtcMs ?: System.currentTimeMillis(),
                                    updatedAtUtcMs = System.currentTimeMillis()
                                )
                                onSave(entry)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_journal_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Reflection")
                    }
                }
            }
        }
    }
}
