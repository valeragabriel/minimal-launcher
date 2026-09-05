package com.gabriel.minimal.ui

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gabriel.minimal.LauncherUiState
import com.gabriel.minimal.data.ClockAlign

@Composable
fun SettingsScreen(
    state: LauncherUiState,
    hasUsageAccess: Boolean,
    onCreateList: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onSetShowClock: (Boolean) -> Unit,
    onSetFrictionSeconds: (Int) -> Unit,
    onSetClockAlign: (ClockAlign) -> Unit,
    onPickBackground: (Uri) -> Unit,
    onClearBackground: () -> Unit,
    onSetSwapBottomActions: (Boolean) -> Unit,
    onOpenScreenTime: () -> Unit,
) {
    val context = LocalContext.current
    var showNewListDialog by remember { mutableStateOf(false) }

    // The photo picker needs no storage permission and returns a temporary
    // content:// URI, which the ViewModel copies into internal storage.
    // HyperOS opens the ROLE_HOME dialog and closes it immediately without granting
    // anything, so the request silently does nothing. Check whether the role actually
    // landed once the dialog returns, and send the user to the system launcher picker
    // when it did not.
    val requestHomeRole = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val held = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) == true
        if (!held) {
            runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onPickBackground) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Header("Today")

        InfoRow(
            title = if (hasUsageAccess) formatDuration(state.totalMinutesToday()) else "—",
            subtitle = if (hasUsageAccess) "Screen time today — tap for the breakdown"
            else "Grant usage access to track this",
            onClick = onOpenScreenTime.takeIf { hasUsageAccess },
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Header("Setup")

        SettingRow(
            title = "Set as default home app",
            subtitle = "Required for this to replace your launcher",
            onClick = {
                val roleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.getSystemService(RoleManager::class.java)
                        ?.createRequestRoleIntent(RoleManager.ROLE_HOME)
                } else null

                if (roleIntent != null) requestHomeRole.launch(roleIntent)
                else context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            },
        )

        SettingRow(
            title = "Usage access",
            subtitle = if (hasUsageAccess) "Granted — daily limits are active"
            else "Not granted — daily limits will not work",
            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Header("Home screen")

        ToggleRow("Show clock", state.config.showClock, onSetShowClock)

        if (state.config.showClock) {
            SettingRow(
                title = "Clock position",
                subtitle = when (state.config.clockAlign) {
                    ClockAlign.Start -> "Left"
                    ClockAlign.Center -> "Centre"
                    ClockAlign.End -> "Right"
                },
                onClick = {
                    onSetClockAlign(
                        when (state.config.clockAlign) {
                            ClockAlign.Start -> ClockAlign.Center
                            ClockAlign.Center -> ClockAlign.End
                            ClockAlign.End -> ClockAlign.Start
                        },
                    )
                },
            )
        }

        SettingRow(
            title = if (state.config.backgroundStamp == 0L) "Add a background photo"
            else "Change background photo",
            subtitle = "Shown behind the home screen, dimmed for readability",
            onClick = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
        val hasPhoto = state.config.backgroundStamp != 0L
        SettingRow(
            title = "Remove background photo",
            subtitle = if (hasPhoto) "Go back to plain black" else "No photo set",
            enabled = hasPhoto,
            onClick = onClearBackground,
        )

        ToggleRow(
            "Swap All apps / Settings",
            state.config.swapBottomActions,
            onSetSwapBottomActions,
        )

        SettingRow(
            title = "Delay before \"open anyway\"",
            subtitle = "${state.config.frictionSeconds} seconds",
            onClick = {
                // Cycle through sensible values rather than shipping a slider.
                val next = when (state.config.frictionSeconds) {
                    0 -> 5
                    5 -> 8
                    8 -> 15
                    15 -> 30
                    else -> 0
                }
                onSetFrictionSeconds(next)
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Header("Lists")

        state.config.lists.forEach { list ->
            SettingRow(
                title = list.name,
                subtitle = "${list.packages.size} apps — tap to delete",
                onClick = { onDeleteList(list.id) },
            )
        }
        SettingRow(title = "New list…", subtitle = null, onClick = { showNewListDialog = true })

        Text(
            "Add apps to a list by long-pressing them in All apps.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    if (showNewListDialog) {
        NewListDialog(
            onDismiss = { showNewListDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) onCreateList(name.trim())
                showNewListDialog = false
            },
        )
    }
}

@Composable
private fun SettingsTitleColor() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)

@Composable
private fun Header(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Reads as a value, not a disabled action — so it keeps full contrast. */
@Composable
private fun InfoRow(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) SettingsTitleColor()
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = SettingsTitleColor(),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NewListDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New list") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
