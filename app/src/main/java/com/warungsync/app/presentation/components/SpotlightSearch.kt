package com.warungsync.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

fun Modifier.spotlightPullGesture(
    listState: LazyListState,
    enabled: Boolean,
    onOpen: () -> Unit
): Modifier {
    if (!enabled) return this

    return pointerInput(listState, enabled) {
        val openThreshold = 64.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var lastY = down.position.y
            var downwardDistance = 0f
            var pointerPressed = true

            while (pointerPressed) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull()
                    ?: break
                val deltaY = change.position.y - lastY
                lastY = change.position.y
                pointerPressed = change.pressed

                if (!listState.canScrollBackward && deltaY > 0f) {
                    downwardDistance += deltaY
                } else if (deltaY < 0f) {
                    downwardDistance = (downwardDistance + deltaY).coerceAtLeast(0f)
                }

                if (downwardDistance >= openThreshold) {
                    onOpen()
                    break
                }
            }
        }
    }
}

@Composable
fun SpotlightSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "Cari produk…"
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val backgroundInteraction = remember { MutableInteractionSource() }
    val contentInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f))
                .clickable(
                    interactionSource = backgroundInteraction,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(
                        interactionSource = contentInteraction,
                        indication = null,
                        onClick = {}
                ),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = placeholder,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onSearchDone = {
                        keyboardController?.hide()
                        onDismiss()
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(12.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
