package com.gabriel.minimal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val CONFIRM_WORD = "confirm"

/**
 * The friction screen. It never hard-blocks: an adult who really wants the app
 * gets it. What stands in the way is deliberate effort — a wait, then typing a
 * word — so that opening the app has to be a decision rather than a reflex.
 *
 * "Open anyway" is not shown at all until the word is typed. A disabled button
 * still advertises the way out; an absent one makes you read the sentence.
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
    var typed by remember { mutableStateOf("") }
    val confirmed = typed.trim().equals(CONFIRM_WORD, ignoreCase = true)

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
            Column {
                Text("${formatDuration(minutesUsed)} used, limit is ${formatDuration(limitMinutes)}.")
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    label = { Text("Type \"$CONFIRM_WORD\" to open it anyway") },
                    keyboardOptions = KeyboardOptions(
                        // The word is lower case; autocapitalising it invites a
                        // mismatch the user cannot see the cause of.
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        },
        confirmButton = {
            if (confirmed) {
                TextButton(onClick = onOpenAnyway, enabled = remaining <= 0) {
                    Text(if (remaining > 0) "Open anyway ($remaining)" else "Open anyway")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = MaterialTheme.colorScheme.onSurface)
            }
        },
    )
}
