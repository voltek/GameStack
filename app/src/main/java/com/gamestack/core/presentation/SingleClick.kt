package com.gamestack.core.presentation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val SingleClickWindowMillis = 700L

// One-shot UiEffects are delivered over a Channel (CLAUDE.md, MVI Contract
// conventions), which queues anything sent while the screen is not collecting.
// So a repeated tap does not merely navigate twice: the second effect waits in
// the queue and fires again when the user returns, bouncing them straight back
// out of the screen they just came back to.
//
// The fix belongs here rather than in the Channel: one user intent must produce
// one effect. Wrap a navigation callback with this so repeats inside the window
// are dropped. Share a single wrapper across a whole list — wrapping each item
// separately would still let two different items fire back to back.
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

// Same guard for a callback that carries no argument, e.g. an empty-state CTA.
// Without it those call sites read `rememberSingleClick<Unit>` and `wrapped(Unit)`,
// which is noise at exactly the places that are easiest to forget to guard.
@Composable
fun rememberSingleClick(onClick: () -> Unit): () -> Unit {
    val guarded = rememberSingleClick<Unit> { onClick() }
    return { guarded(Unit) }
}
