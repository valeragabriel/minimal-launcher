package com.gabriel.minimal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gabriel.minimal.LauncherUiState
import com.gabriel.minimal.data.LauncherApp

/** Long-press actions. This is the only place app configuration happens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: LauncherApp,
    state: LauncherUiState,
    onDismiss: () -> Unit,
    onToggleHome: () -> Unit,
    onToggleInList: (listId: String) -> Unit,
    onSetLimit: (minutes: Int?) -> Unit,
    onAppInfo: () -> Unit,
) {
    var showLimitDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val pinned = app.packageName in state.config.homePackages
    val currentLimit = state.config.dailyLimitMinutes[app.packageName]

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${state.minutesUsed(app.packageName)} min today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SheetAction(if (pinned) "Remove from home" else "Pin to home") {
                onToggleHome(); onDismiss()
            }
            SheetAction(
                if (currentLimit != null) "Daily limit: $currentLimit min" else "Set daily limit",
            ) { showLimitDialog = true }

            if (state.config.lists.isNotEmpty()) {
                Text(
                    "LISTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                state.config.lists.forEach { list ->
                    val inList = app.packageName in list.packages
                    SheetAction(if (inList) "✓ ${list.name}" else list.name) { onToggleInList(list.id) }
                }
            }

            SheetAction("App info") { onAppInfo(); onDismiss() }
        }
    }

    if (showLimitDialog) {
        LimitEditorDialog(
            appLabel = app.label,
            currentMinutes = currentLimit,
            onDismiss = { showLimitDialog = false },
            onConfirm = { minutes ->
                onSetLimit(minutes)
                showLimitDialog = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun SheetAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun LimitEditorDialog(
    appLabel: String,
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    var text by remember { mutableStateOf(currentMinutes?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily limit for $appLabel") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() }.take(4) },
                    label = { Text("Minutes per day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "Leave empty to remove the limit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
