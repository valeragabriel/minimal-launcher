package com.gabriel.minimal.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reads the installed-app inventory.
 *
 * Uses [LauncherApps] rather than PackageManager.queryIntentActivities: it is the
 * API meant for home apps, it covers work-profile users, and it gives us install /
 * uninstall callbacks instead of broadcast receivers.
 */
class AppRepository(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    fun loadApps(): List<LauncherApp> =
        userManager.userProfiles.flatMap { user -> activitiesFor(user) }
            .sortedBy { it.label.lowercase() }

    private fun activitiesFor(user: UserHandle): List<LauncherApp> =
        launcherApps.getActivityList(null, user).map { info ->
            LauncherApp(
                packageName = info.componentName.packageName,
                activityName = info.componentName.className,
                label = info.label.toString(),
                user = user,
            )
        }

    fun launch(app: LauncherApp) {
        launcherApps.startMainActivity(
            ComponentName(app.packageName, app.activityName),
            app.user,
            null,
            null,
        )
    }

    fun openAppInfo(app: LauncherApp) {
        launcherApps.startAppDetailsActivity(
            ComponentName(app.packageName, app.activityName),
            app.user,
            null,
            null,
        )
    }

    /** Emits the current app list, then again whenever packages change. */
    fun appsFlow(): Flow<List<LauncherApp>> = callbackFlow {
        trySend(loadApps())

        val callback = object : LauncherApps.Callback() {
            private fun refresh() { trySend(loadApps()) }
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = refresh()
            override fun onPackagesUnavailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = refresh()
        }

        launcherApps.registerCallback(callback)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
}
