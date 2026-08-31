package com.gabriel.minimal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gabriel.minimal.data.LauncherApp

/**
 * A single text row. No icons anywhere in this launcher — icons are the thing that
 * makes a home screen scannable, and scannable is what we are trying to avoid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: LauncherApp,
    minutesLeft: Int?,
    overLimit: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (overLimit) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.onBackground,
            textDecoration = if (overLimit) TextDecoration.LineThrough else null,
        )
        if (minutesLeft != null) {
            Text(
                text = if (minutesLeft > 0) "$minutesLeft min left today" else "limit reached",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
