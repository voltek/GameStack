package com.gamestack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

// For previews: in the app the Scaffold paints the background, and a preview has none.
@Composable
fun GameStackPreviewTheme(content: @Composable () -> Unit) {
    GameStackTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
