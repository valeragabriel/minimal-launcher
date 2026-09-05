package com.gabriel.minimal.data

import android.os.UserHandle

/** One launchable activity, as reported by [android.content.pm.LauncherApps]. */
data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val user: UserHandle,
    /** False for system apps, which the uninstall intent silently refuses. */
    val canUninstall: Boolean,
) {
    /** Stable identity across reboots; [UserHandle] is not serializable. */
    val key: String get() = "$packageName/$activityName"
}
