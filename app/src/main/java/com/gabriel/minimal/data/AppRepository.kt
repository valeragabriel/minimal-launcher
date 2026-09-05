package com.gabriel.minimal.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
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

    private val appContext = context.applicationContext
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
                canUninstall = info.applicationInfo.isUninstallable() &&
                    info.componentName.packageName != appContext.packageName,
            )
        }

    /**
     * A plain system app cannot be removed at all; one that has been updated can be
     * reverted to its factory version, which the uninstall dialog presents as
     * "uninstall updates". Offering the action anywhere else just fails silently.
     */
    private fun ApplicationInfo.isUninstallable(): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

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

    /**
     * Hands the package to the system uninstaller, which shows its own confirmation.
     * An ordinary app cannot remove another package itself — DELETE_PACKAGES is a
     * signature permission — so this is the only route available.
     */
    fun uninstall(app: LauncherApp) {
        appContext.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Emits the current app list, then again whenever packages change.
     *
     * The callback gets its own [HandlerThread]: [LauncherApps.registerCallback]
     * builds a [Handler] on the calling thread when none is supplied, and this flow
     * is collected on Dispatchers.IO, whose workers have no Looper — that throws.
     * A dedicated Looper also keeps the [loadApps] PackageManager query off the
     * main thread, which a main-looper Handler would not.
     */
    fun appsFlow(): Flow<List<LauncherApp>> = callbackFlow {
        trySend(loadApps())

        val callbackThread = HandlerThread("launcher-apps-callback").apply { start() }

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

        launcherApps.registerCallback(callback, Handler(callbackThread.looper))
        awaitClose {
            launcherApps.unregisterCallback(callback)
            callbackThread.quitSafely()
        }
    }
}
