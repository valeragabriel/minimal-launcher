package com.gabriel.minimal.data

import kotlinx.serialization.Serializable

/** Where the clock sits across the width of the home screen. */
@Serializable
enum class ClockAlign { Start, Center, End }

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
    val clockAlign: ClockAlign = ClockAlign.Center,
    /**
     * Non-zero when a background photo is set. The value is the time it was chosen,
     * which doubles as a cache key so the decoded bitmap is reloaded when it changes.
     * The image itself is copied into internal storage rather than referenced by its
     * content:// URI, which would not survive a reboot.
     */
    val backgroundStamp: Long = 0L,
    /** Puts Settings on the left and All apps on the right. */
    val swapBottomActions: Boolean = false,
    /** Seconds the "open anyway" button stays disabled once a limit is hit. */
    val frictionSeconds: Int = 8,
)
