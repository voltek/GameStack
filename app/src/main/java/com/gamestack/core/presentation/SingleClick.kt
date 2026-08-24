package com.gamestack.core.presentation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// internal so a screen test asserts against the real window, not a copy of it.
internal const val SingleClickWindowMillis = 700L

// A duplicate navigation effect is not merely handled twice: the Channel it
// travels on queues it, so it fires again when the user returns to the screen.
// Share one wrapper per list, or two different items can still fire back to
// back. Why the fix lives here and not in the Channel: CLAUDE.md, MVI contract.
@Composable
fun <T> rememberSingleClick(onClick: (T) -> Unit): (T) -> Unit {
    // Null, not 0L: zero is a real uptime, so it would claim a click had happened
    // at boot and swallow the first one for the next 700ms.
    var lastClickMillis by remember { mutableStateOf<Long?>(null) }
    return { argument ->
        val now = SystemClock.uptimeMillis()
        val elapsed = lastClickMillis?.let { now - it }
        if (elapsed == null || elapsed >= SingleClickWindowMillis) {
            lastClickMillis = now
            onClick(argument)
        }
    }
}

@Composable
fun rememberSingleClick(onClick: () -> Unit): () -> Unit {
    val guarded = rememberSingleClick<Unit> { onClick() }
    return { guarded(Unit) }
}
