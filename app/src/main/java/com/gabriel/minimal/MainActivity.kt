package com.gabriel.minimal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.gabriel.minimal.data.LauncherApp
import com.gabriel.minimal.ui.AppActionsSheet
import com.gabriel.minimal.ui.DrawerScreen
import com.gabriel.minimal.ui.HomeScreen
import com.gabriel.minimal.ui.LimitReachedDialog
import com.gabriel.minimal.ui.MinimalTheme
import com.gabriel.minimal.ui.ScreenTimeScreen
import com.gabriel.minimal.ui.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

private enum class Screen { Home, Drawer, Settings, ScreenTime }

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Emits when HOME is pressed while this activity is already resumed. */
    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme is always dark, so pin the system bar icons light rather than
        // letting them follow the system setting and vanish against black.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            MinimalTheme {
                LauncherRoot(viewModel, homePressed)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homePressed.tryEmit(Unit)
    }
}

@Composable
private fun LauncherRoot(
    viewModel: LauncherViewModel,
    homePressed: MutableSharedFlow<Unit>,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blockedApp by viewModel.blockedApp.collectAsStateWithLifecycle()

    // Recount while visible rather than only on resume. The launcher's own span is
    // still open while the home screen is up, so a resume-only refresh leaves the
    // screen-time figure frozen at whatever it was when you last came back to it.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshUsage()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    var screen by remember { mutableStateOf(Screen.Home) }
    var sheetApp by remember { mutableStateOf<LauncherApp?>(null) }

    // Pressing HOME collapses whatever you were doing back to the home screen.
    LaunchedEffect(Unit) {
        homePressed.collect {
            sheetApp = null
            screen = Screen.Home
        }
    }

    // Screen time is reached from Settings, so back should return there, not home.
    BackHandler(enabled = screen != Screen.Home) {
        screen = if (screen == Screen.ScreenTime) Screen.Settings else Screen.Home
    }

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)

    androidx.compose.foundation.layout.Box(rootModifier) {
        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onLaunch = viewModel::requestLaunch,
                onLongPress = { sheetApp = it },
                onOpenDrawer = { screen = Screen.Drawer },
                onOpenSettings = { screen = Screen.Settings },
            )

            Screen.Drawer -> DrawerScreen(
                state = state,
                onLaunch = viewModel::requestLaunch,
                onLongPress = { sheetApp = it },
            )

            Screen.Settings -> SettingsScreen(
                state = state,
                hasUsageAccess = viewModel.hasUsageAccess(),
                onCreateList = viewModel::createList,
                onDeleteList = viewModel::deleteList,
                onSetShowClock = viewModel::setShowClock,
                onSetFrictionSeconds = viewModel::setFrictionSeconds,
                onSetClockAlign = viewModel::setClockAlign,
                onPickBackground = viewModel::setBackgroundImage,
                onClearBackground = viewModel::clearBackgroundImage,
                onSetSwapBottomActions = viewModel::setSwapBottomActions,
                onOpenScreenTime = { screen = Screen.ScreenTime },
            )

            Screen.ScreenTime -> ScreenTimeScreen(
                state = state,
                onLongPress = { sheetApp = it },
            )
        }
    }

    sheetApp?.let { app ->
        AppActionsSheet(
            app = app,
            state = state,
            onDismiss = { sheetApp = null },
            onToggleHome = { viewModel.toggleHome(app.packageName) },
            onToggleInList = { listId -> viewModel.toggleInList(listId, app.packageName) },
            onSetLimit = { minutes -> viewModel.setDailyLimit(app.packageName, minutes) },
            onUninstall = { viewModel.uninstall(app) },
            onAppInfo = { viewModel.openAppInfo(app) },
        )
    }

    blockedApp?.let { app ->
        LimitReachedDialog(
            appLabel = app.label,
            minutesUsed = state.minutesUsed(app.packageName),
            limitMinutes = state.config.dailyLimitMinutes[app.packageName] ?: 0,
            frictionSeconds = state.config.frictionSeconds,
            onDismiss = viewModel::dismissBlock,
            onOpenAnyway = { viewModel.launchAnyway(app) },
        )
    }
}

private const val REFRESH_INTERVAL_MS = 20_000L
