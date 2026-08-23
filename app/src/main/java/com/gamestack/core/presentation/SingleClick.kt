package com.gamestack.core.presentation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val SingleClickWindowMillis = 700L

// A duplicate navigation effect is not merely handled twice: the Channel it
// travels on queues it, so it fires again when the user returns to the screen.
// Share one wrapper per list, or two different items can still fire back to
// back. Why the fix lives here and not in the Channel: CLAUDE.md, MVI contract.
@Composable
fun <T> rememberSingleClick(onClick: (T) -> Unit): (T) -> Unit {
    var lastClickMillis by remember { mutableLongStateOf(0L) }
    return { argument ->
        val now = SystemClock.uptimeMillis()
        if (now - lastClickMillis >= SingleClickWindowMillis) {
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
