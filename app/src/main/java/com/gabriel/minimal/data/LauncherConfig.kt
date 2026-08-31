package com.gabriel.minimal.data

import kotlinx.serialization.Serializable

@Serializable
data class AppList(
    val id: String,
    val name: String,
    val packages: List<String> = emptyList(),
)

/**
 * Everything the user can change. Persisted as one JSON blob — the whole config
 * is a few kB, so a document store buys nothing over a single DataStore key.
 */
@Serializable
data class LauncherConfig(
    /** Packages pinned to the home screen, in display order. */
    val homePackages: List<String> = emptyList(),
    val lists: List<AppList> = emptyList(),
    /** package -> minutes of foreground time allowed per day. */
    val dailyLimitMinutes: Map<String, Int> = emptyMap(),
    val showClock: Boolean = true,
    /** Seconds the "open anyway" button stays disabled once a limit is hit. */
    val frictionSeconds: Int = 8,
)
