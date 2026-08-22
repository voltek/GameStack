package com.gamestack.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamestack.ui.theme.FullyRoundedShape
import com.gamestack.ui.theme.GameStackTheme

// Shared full-screen message layout for empty/error states, matching the shape
// approved in docs/project/design/states/ (icon badge + title + description +
// optional CTA). Screens supply their own icon/copy/action — this owns only layout.
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 24.dp),
                shape = FullyRoundedShape
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(start = if (actionIcon != null) 8.dp else 0.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageStateWithActionPreview() {
    GameStackTheme {
        MessageState(
            icon = Icons.Filled.SearchOff,
            title = "No Results Found",
            description = "Try adjusting your search terms to find what you're looking for.",
            actionLabel = "Go To Library",
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageStateWithoutActionPreview() {
    GameStackTheme {
        MessageState(
            icon = Icons.Filled.Bookmark,
            title = "Want to Play",
            description = "No games added yet"
        )
    }
}
