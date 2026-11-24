package com.example.dayseed.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.dayseed.DayseedViewModel
import com.example.dayseed.data.UndoEntityType
import com.example.dayseed.data.UndoToken
import kotlinx.coroutines.delay

/**
 * The undo token a screen may offer, or null (D-237).
 *
 * Undo is offered only right after a registered completion or a deleted session, never for habit
 * configuration changes, and the token is retired when its short window ends so the banner hides itself.
 */
@Composable
fun rememberCompletionUndoToken(viewModel: DayseedViewModel): UndoToken? {
    val token by viewModel.activeUndoToken.collectAsState()
    LaunchedEffect(token?.tokenId) {
        val current = token ?: return@LaunchedEffect
        val remaining = current.expiresAtUtcMs - viewModel.repository.clock.nowUtcMs()
        if (remaining > 0) delay(remaining)
        if (viewModel.activeUndoToken.value?.tokenId == current.tokenId) viewModel.repository.clearActiveUndoToken()
    }
    return token?.takeIf {
        it.entityType == UndoEntityType.HABIT_ENTRY || it.entityType == UndoEntityType.HABIT_DAY_OUTCOME
    }
}
