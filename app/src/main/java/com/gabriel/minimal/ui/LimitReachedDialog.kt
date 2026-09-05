package com.gabriel.minimal.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * The friction screen. It never hard-blocks: an adult who really wants the app
 * gets it after a wait. The delay is the product — a hard block just gets the
 * launcher uninstalled.
 */
@Composable
fun LimitReachedDialog(
    appLabel: String,
    minutesUsed: Int,
    limitMinutes: Int,
    frictionSeconds: Int,
    onDismiss: () -> Unit,
    onOpenAnyway: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(frictionSeconds) }

    LaunchedEffect(appLabel) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You are done with $appLabel today") },
        text = {
            Text("${formatDuration(minutesUsed)} used, limit is ${formatDuration(limitMinutes)}.")
        },
        confirmButton = {
            TextButton(onClick = onOpenAnyway, enabled = remaining <= 0) {
                Text(if (remaining > 0) "Open anyway ($remaining)" else "Open anyway")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
