package com.gabriel.minimal.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/**
 * Per-app foreground time for the current day.
 *
 * Built from the event stream rather than [UsageStatsManager.queryUsageStats],
 * because the aggregated buckets are coarse and reset on their own schedule.
 * Reading events is cheap and on-demand — there is no polling loop and no
 * background service, so this costs nothing when the launcher is not visible.
 */
class UsageTracker(private val context: Context) {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun todayForegroundMillis(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()

        val now = System.currentTimeMillis()
        val totals = mutableMapOf<String, Long>()
        val resumedAt = mutableMapOf<String, Long>()

        val events = usageStats.queryEvents(startOfToday(), now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumedAt[event.packageName] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val from = resumedAt.remove(event.packageName)
                    if (from != null) {
                        totals[event.packageName] =
                            (totals[event.packageName] ?: 0L) + (event.timeStamp - from)
                    }
                }
            }
        }

        // Whatever is on screen right now has no closing event yet.
        resumedAt.forEach { (pkg, from) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (now - from)
        }
        return totals
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
