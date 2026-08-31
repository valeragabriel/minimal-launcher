package com.gabriel.minimal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gabriel.minimal.LauncherUiState
import com.gabriel.minimal.data.LauncherApp

@Composable
fun DrawerScreen(
    state: LauncherUiState,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(state.apps, query) {
        if (query.isBlank()) state.apps
        else state.apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    val searching = query.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Lists are hidden while searching — a filter over a filter reads badly.
            if (!searching) {
                state.config.lists.forEach { list ->
                    val appsInList = state.appsIn(list)
                    if (appsInList.isEmpty()) return@forEach

                    item(key = "header-${list.id}") { SectionHeader(list.name) }
                    items(appsInList, key = { "${list.id}-${it.key}" }) { app ->
                        AppRow(
                            app = app,
                            minutesLeft = state.minutesLeft(app.packageName),
                            overLimit = state.isOverLimit(app.packageName),
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                        )
                    }
                }
                item(key = "header-all") { SectionHeader("All apps") }
            }

            items(filtered, key = { "all-${it.key}" }) { app ->
                AppRow(
                    app = app,
                    minutesLeft = state.minutesLeft(app.packageName),
                    overLimit = state.isOverLimit(app.packageName),
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
    )
}
