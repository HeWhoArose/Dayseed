package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.ComebackInfo
import com.example.dayseed.data.ComebackTier
import com.example.dayseed.data.Habit
import com.example.dayseed.data.TrackingMode

@Composable
fun ComebackCard(
    info: ComebackInfo,
    habits: List<Habit> = emptyList(),
    onApplyAdaptiveRestart: ((resumeIds: Set<String>, pauseIds: Set<String>, loweredTargets: Map<String, Int>, reflection: String?) -> Unit)? = null,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdaptiveSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("comeback_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = "Welcome back seed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_comeback_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss welcome message",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (onApplyAdaptiveRestart != null && habits.isNotEmpty()) {
                            showAdaptiveSheet = true
                        } else {
                            onPrimaryAction()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comeback_primary_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = info.primaryActionLabel, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comeback_secondary_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = info.secondaryActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showAdaptiveSheet && onApplyAdaptiveRestart != null) {
        AdaptiveComebackDialog(
            info = info,
            habits = habits,
            onDismiss = { showAdaptiveSheet = false },
            onConfirm = { resumeIds, pauseIds, lowered, reflection ->
                showAdaptiveSheet = false
                onApplyAdaptiveRestart(resumeIds, pauseIds, lowered, reflection)
            }
        )
    }
}

@Composable
private fun AdaptiveComebackDialog(
    info: ComebackInfo,
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>, Set<String>, Map<String, Int>, String?) -> Unit
) {
    // Map of habitId -> "RESUME", "LOWER", or "PAUSE"
    val choices = remember(habits) {
        mutableStateMapOf<String, String>().apply {
            habits.forEach { put(it.habitId, "RESUME") }
        }
    }
    var reflectionBody by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("adaptive_comeback_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (info.tier == ComebackTier.DAYS_30) "30-Day Adaptive Restart" else "14-Day Gentle Return",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose which habits to resume as-is, soften with a smaller goal, or pause for now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                for (h in habits) {
                    val currentChoice = choices[h.habitId] ?: "RESUME"
                    val hasNumericTarget = h.trackingMode.canonical() in listOf(TrackingMode.QUANTITY, TrackingMode.DURATION) &&
                        (h.targetValue ?: 1) > 1
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = h.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = currentChoice == "RESUME",
                                    onClick = { choices[h.habitId] = "RESUME" },
                                    label = { Text("Resume") }
                                )
                                if (hasNumericTarget) {
                                    val halfTarget = ((h.targetValue ?: 2) / 2).coerceAtLeast(1)
                                    FilterChip(
                                        selected = currentChoice == "LOWER",
                                        onClick = { choices[h.habitId] = "LOWER" },
                                        label = { Text("Lower ($halfTarget ${h.unitCode ?: ""})") }
                                    )
                                }
                                FilterChip(
                                    selected = currentChoice == "PAUSE",
                                    onClick = { choices[h.habitId] = "PAUSE" },
                                    label = { Text("Pause") }
                                )
                            }
                        }
                    }
                }

                if (info.tier == ComebackTier.DAYS_30) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reflectionBody,
                        onValueChange = { reflectionBody = it },
                        label = { Text("Optional returning note (saved to Journal)") },
                        placeholder = { Text("What feels realistic and supportive right now?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comeback_reflection_input"),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val resumeIds = mutableSetOf<String>()
                            val pauseIds = mutableSetOf<String>()
                            val loweredMap = mutableMapOf<String, Int>()
                            for (h in habits) {
                                when (choices[h.habitId]) {
                                    "PAUSE" -> pauseIds.add(h.habitId)
                                    "LOWER" -> {
                                        resumeIds.add(h.habitId)
                                        loweredMap[h.habitId] = ((h.targetValue ?: 2) / 2).coerceAtLeast(1)
                                    }
                                    else -> resumeIds.add(h.habitId)
                                }
                            }
                            onConfirm(resumeIds, pauseIds, loweredMap, reflectionBody.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_comeback_restart_btn")
                    ) {
                        Text("Apply Plan")
                    }
                }
            }
        }
    }
}
