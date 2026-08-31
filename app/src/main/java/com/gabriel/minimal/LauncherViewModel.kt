package com.gabriel.minimal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.minimal.data.AppList
import com.gabriel.minimal.data.AppRepository
import com.gabriel.minimal.data.LauncherApp
import com.gabriel.minimal.data.LauncherConfig
import com.gabriel.minimal.data.SettingsStore
import com.gabriel.minimal.data.UsageTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

data class LauncherUiState(
    val apps: List<LauncherApp> = emptyList(),
    val config: LauncherConfig = LauncherConfig(),
    val usageTodayMillis: Map<String, Long> = emptyMap(),
) {
    private val byPackage: Map<String, LauncherApp> get() = apps.associateBy { it.packageName }

    fun app(packageName: String): LauncherApp? = byPackage[packageName]

    fun homeApps(): List<LauncherApp> = config.homePackages.mapNotNull(::app)

    fun appsIn(list: AppList): List<LauncherApp> = list.packages.mapNotNull(::app)

    fun minutesUsed(packageName: String): Int =
        TimeUnit.MILLISECONDS.toMinutes(usageTodayMillis[packageName] ?: 0L).toInt()

    /** null when no limit is set for the package. */
    fun minutesLeft(packageName: String): Int? =
        config.dailyLimitMinutes[packageName]?.let { it - minutesUsed(packageName) }

    fun isOverLimit(packageName: String): Boolean = (minutesLeft(packageName) ?: 1) <= 0
}

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppRepository(app)
    private val settings = SettingsStore(app)
    private val usage = UsageTracker(app)

    private val usageToday = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Set when a launch was stopped by a daily limit; drives the friction dialog. */
    private val _blockedApp = MutableStateFlow<LauncherApp?>(null)
    val blockedApp: StateFlow<LauncherApp?> = _blockedApp.asStateFlow()

    val uiState: StateFlow<LauncherUiState> =
        combine(
            repository.appsFlow().flowOn(Dispatchers.IO),
            settings.config,
            usageToday,
        ) { apps, config, usageMap ->
            LauncherUiState(apps, config, usageMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    fun hasUsageAccess(): Boolean = usage.hasUsageAccess()

    /** Called from onResume — usage only changes while we are *not* on screen. */
    fun refreshUsage() {
        viewModelScope.launch {
            usageToday.value = withContext(Dispatchers.IO) { usage.todayForegroundMillis() }
        }
    }

    fun requestLaunch(app: LauncherApp) {
        if (uiState.value.isOverLimit(app.packageName)) {
            _blockedApp.value = app
        } else {
            repository.launch(app)
        }
    }

    fun launchAnyway(app: LauncherApp) {
        _blockedApp.value = null
        repository.launch(app)
    }

    fun dismissBlock() { _blockedApp.value = null }

    fun openAppInfo(app: LauncherApp) = repository.openAppInfo(app)

    // ---- configuration edits -------------------------------------------------

    fun toggleHome(packageName: String) = edit { config ->
        val pinned = config.homePackages
        config.copy(
            homePackages = if (packageName in pinned) pinned - packageName else pinned + packageName,
        )
    }

    fun moveHomeApp(from: Int, to: Int) = edit { config ->
        val items = config.homePackages.toMutableList()
        if (from !in items.indices || to !in items.indices) return@edit config
        items.add(to, items.removeAt(from))
        config.copy(homePackages = items)
    }

    fun createList(name: String) = edit { config ->
        config.copy(lists = config.lists + AppList(id = UUID.randomUUID().toString(), name = name))
    }

    fun deleteList(listId: String) = edit { config ->
        config.copy(lists = config.lists.filterNot { it.id == listId })
    }

    fun renameList(listId: String, name: String) = edit { config ->
        config.copy(lists = config.lists.map { if (it.id == listId) it.copy(name = name) else it })
    }

    fun toggleInList(listId: String, packageName: String) = edit { config ->
        config.copy(
            lists = config.lists.map { list ->
                if (list.id != listId) list
                else list.copy(
                    packages = if (packageName in list.packages) list.packages - packageName
                    else list.packages + packageName,
                )
            },
        )
    }

    /** [minutes] of null clears the limit. */
    fun setDailyLimit(packageName: String, minutes: Int?) = edit { config ->
        val limits = config.dailyLimitMinutes.toMutableMap()
        if (minutes == null || minutes <= 0) limits.remove(packageName) else limits[packageName] = minutes
        config.copy(dailyLimitMinutes = limits)
    }

    fun setShowClock(show: Boolean) = edit { it.copy(showClock = show) }

    fun setFrictionSeconds(seconds: Int) = edit { it.copy(frictionSeconds = seconds.coerceIn(0, 60)) }

    private fun edit(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch { settings.update(transform) }
    }
}
