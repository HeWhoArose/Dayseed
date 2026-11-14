package com.example.dayseed.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Ensures a focused text input and its supporting helper/error text remain visible above the
 * soft keyboard (`ODR-004` / `P2-H13`), both immediately on focus and after the Android IME
 * window resize animation (`adjustResize` / `WindowInsets.ime`) completes.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bringIntoViewOnFocus(extraBottomClearance: Dp = 28.dp): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val extraBottomPx = with(density) { extraBottomClearance.toPx() }

    LaunchedEffect(isFocused, imeBottomPx) {
        if (isFocused) {
            val targetRect = if (fieldSize.width > 0 && fieldSize.height > 0) {
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fieldSize.width.toFloat(),
                    bottom = fieldSize.height.toFloat() + extraBottomPx
                )
            } else {
                null
            }
            runCatching { requester.bringIntoView(targetRect) }
            delay(140L)
            runCatching { requester.bringIntoView(targetRect) }
            delay(160L)
            runCatching { requester.bringIntoView(targetRect) }
        }
    }

    this
        .bringIntoViewRequester(requester)
        .onSizeChanged { fieldSize = it }
        .onFocusEvent { focusState ->
            isFocused = focusState.isFocused || focusState.hasFocus
        }
}
