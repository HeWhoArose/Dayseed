package com.example.dayseed.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.example.dayseed.data.JournalEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorDialog(
    initialEntry: JournalEntry?,
    selectedDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (JournalEntry) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val isEditing = initialEntry != null
    val targetDate = initialEntry?.date ?: selectedDate

    val prompts = listOf(
        "What went better than expected today?",
        "A small moment of calm I noticed",
        "What gave me energy today?",
        "Something I'm letting go of tonight"
    )

    var prompt by remember { mutableStateOf(initialEntry?.prompt ?: prompts[0]) }
    var content by remember { mutableStateOf(initialEntry?.content ?: "") }
    var contentError by remember { mutableStateOf(false) }

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
                            text = targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prompt selection chips
                Text(
                    text = "Suggested Prompts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (p in prompts) {
                        SuggestionChip(
                            onClick = { prompt = p },
                            label = {
                                Text(
                                    text = p,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Active prompt display / custom prompt
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Reflection Theme / Prompt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("journal_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Journal content
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (it.isNotBlank()) contentError = false
                    },
                    label = { Text("Write your thoughts...") },
                    placeholder = { Text("Even one sentence is meaningful. There are no word counts or rules.") },
                    isError = contentError,
                    supportingText = {
                        val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                        Text(if (contentError) "Please write something before saving" else "$wordCount words")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("journal_content_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (content.isBlank()) {
                                contentError = true
                            } else {
                                val entry = JournalEntry(
                                    id = initialEntry?.id ?: UUID.randomUUID().toString(),
                                    date = targetDate,
                                    prompt = prompt.trim(),
                                    content = content.trim(),
                                    createdAt = initialEntry?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
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
                        Text("Save")
                    }
                }
            }
        }
    }
}
