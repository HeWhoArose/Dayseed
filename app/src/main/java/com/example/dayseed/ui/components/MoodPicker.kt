package com.example.dayseed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dayseed.data.MoodCatalog
import androidx.compose.foundation.clickable

/** A single tappable row showing the chosen mood as emoji, or a prompt when none is chosen. */
@Composable
fun MoodSelectorRow(
    primaryMood: String?,
    secondaryMood: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MoodCatalog.findByCode(primaryMood)
    val secondary = MoodCatalog.findByCode(secondaryMood)
    val spoken = when {
        primary == null -> "Mood, not set. Opens the mood picker."
        secondary == null -> "Mood, ${primary.label}. Opens the mood picker."
        else -> "Mood, ${primary.label} and ${secondary.label}. Opens the mood picker."
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = spoken }
    ) {
        Row(
            modifier = Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (primary == null) {
                Text("🙂", fontSize = 22.sp)
                Text(
                    text = "How are you feeling?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(primary.symbol, fontSize = 26.sp)
                if (secondary != null) Text(secondary.symbol, fontSize = 26.sp)
                Text(
                    text = if (secondary == null) primary.label else "${primary.label} · ${secondary.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The mood pop-up: a grid of emoji. Pick a main feeling first; a second, optional feeling then appears.
 * Tapping the chosen main feeling again clears the mood. Selection is applied with Done.
 */
@Composable
fun MoodPickerDialog(
    initialPrimary: String?,
    initialSecondary: String?,
    onConfirm: (primary: String?, secondary: String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            MoodPickerContent(initialPrimary, initialSecondary, onConfirm)
        }
    }
}

/** The pop-up's content, separate from the window so it can be rendered and tested directly. */
@Composable
fun MoodPickerContent(
    initialPrimary: String?,
    initialSecondary: String?,
    onConfirm: (primary: String?, secondary: String?) -> Unit
) {
    var primary by rememberSaveable { mutableStateOf(initialPrimary) }
    var secondary by rememberSaveable { mutableStateOf(initialSecondary) }
    Column(
        modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()).testTag("mood_picker_dialog"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (primary == null) "How are you feeling?" else "Main feeling",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.dayseedHeading()
        )
        MoodGrid(
            selected = primary,
            exclude = null,
            tagPrefix = "primary_mood_",
            onPick = { code ->
                if (primary.equals(code, ignoreCase = true)) {
                    primary = null; secondary = null
                } else {
                    primary = code
                    if (secondary.equals(code, ignoreCase = true)) secondary = null
                }
            }
        )
        if (primary != null) {
            Text(
                text = "Also feeling (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoodGrid(
                selected = secondary,
                exclude = primary,
                tagPrefix = "secondary_mood_",
                onPick = { code -> secondary = if (secondary.equals(code, ignoreCase = true)) null else code }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (primary != null) {
                TextButton(
                    onClick = { primary = null; secondary = null },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("mood_picker_clear_btn")
                ) { Text("Clear") }
            }
            TextButton(
                onClick = { onConfirm(primary, secondary) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("mood_picker_done_btn")
            ) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MoodGrid(
    selected: String?,
    exclude: String?,
    tagPrefix: String,
    onPick: (String) -> Unit
) {
    val moods = MoodCatalog.pickerMoods.filter { !it.code.equals(exclude, ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        moods.chunked(4).forEach { rowMoods ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowMoods.forEach { mood ->
                    val isSelected = selected.equals(mood.code, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onPick(mood.code) })
                            .semantics { contentDescription = mood.label }
                            .testTag("$tagPrefix${mood.code}")
                    ) {
                        Column(
                            modifier = Modifier.heightIn(min = 64.dp).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(mood.symbol, fontSize = 28.sp, textAlign = TextAlign.Center)
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, fontSize = if (mood.label.length > 9) 11.sp else 12.sp),
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Keep the last short row's cells the same width as the full rows.
                repeat(4 - rowMoods.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
