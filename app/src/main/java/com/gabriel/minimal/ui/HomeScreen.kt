package com.gabriel.minimal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabriel.minimal.LauncherUiState
import com.gabriel.minimal.data.LauncherApp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: LauncherUiState,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        if (state.config.showClock) {
            Spacer(Modifier.height(48.dp))
            Clock()
        }

        Spacer(Modifier.height(32.dp))

        val homeApps = state.homeApps()
        if (homeApps.isEmpty()) {
            EmptyHomeHint(Modifier.weight(1f), onOpenDrawer)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(homeApps, key = { it.key }) { app ->
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onOpenDrawer) { Text("All apps") }
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Re-align to the top of the next minute instead of ticking per second.
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    Column {
        Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun EmptyHomeHint(modifier: Modifier = Modifier, onOpenDrawer: () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No apps pinned yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onOpenDrawer) { Text("Choose apps") }
        }
    }
}
