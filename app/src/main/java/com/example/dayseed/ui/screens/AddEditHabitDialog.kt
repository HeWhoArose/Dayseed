package com.example.dayseed.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dayseed.data.Habit

@Composable
fun AddEditHabitDialog(
    initialHabit: Habit?,
    alreadyLoggedToday: Boolean = false,
    suggestedReminderTime: String? = null,
    onDismiss: () -> Unit,
    onSave: (habit: Habit, applyTodayWhenLogged: Boolean) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            CreateEditHabitFormContent(
                initialHabit = initialHabit,
                alreadyLoggedToday = alreadyLoggedToday,
                suggestedReminderTime = suggestedReminderTime,
                isFullScreen = false,
                onDismiss = onDismiss,
                onSave = { habit, applyTodayWhenLogged ->
                    onSave(habit, applyTodayWhenLogged)
                    true
                },
                onDelete = onDelete
            )
        }
    }
}
