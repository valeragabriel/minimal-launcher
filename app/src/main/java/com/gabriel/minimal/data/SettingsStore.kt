package com.gabriel.minimal.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "launcher")

class SettingsStore(private val context: Context) {

    private val configKey = stringPreferencesKey("config")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val config: Flow<LauncherConfig> = context.dataStore.data.map { prefs ->
        prefs[configKey].decodeOrDefault()
    }

    suspend fun update(transform: (LauncherConfig) -> LauncherConfig) {
        context.dataStore.edit { prefs ->
            prefs[configKey] = json.encodeToString(
                LauncherConfig.serializer(),
                transform(prefs[configKey].decodeOrDefault()),
            )
        }
    }

    /** A corrupt or older blob falls back to defaults rather than crashing the home screen. */
    private fun String?.decodeOrDefault(): LauncherConfig =
        this?.let { runCatching { json.decodeFromString(LauncherConfig.serializer(), it) }.getOrNull() }
            ?: LauncherConfig()
}
