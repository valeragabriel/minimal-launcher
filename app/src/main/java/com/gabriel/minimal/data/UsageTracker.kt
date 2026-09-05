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

        // Exactly one package is foreground at a time, and it changes only when a
        // *different* package resumes. Do not track spans per activity: a single app
        // hands off between its own activities constantly (Chrome -> FirstRunActivity),
        // and one activity's STOPPED would close the span another one owns, silently
        // discarding the real foreground time.
        var foreground: String? = null
        var since = 0L

        fun close(at: Long) {
            val pkg = foreground ?: return
            totals[pkg] = (totals[pkg] ?: 0L) + (at - since)
            foreground = null
        }

        val events = usageStats.queryEvents(startOfToday(), now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (foreground != event.packageName) {
                        close(event.timeStamp)
                        foreground = event.packageName
                        since = event.timeStamp
                    }
                    // Same package resuming another of its activities: span continues.
                }

                // The foreground app pausing ends the span. Relying only on the next
                // package resuming misses screen-off, and SCREEN_NON_INTERACTIVE cannot
                // be depended on — some devices never emit it. An app handing off
                // between its own activities pauses and resumes within the same
                // package, which costs a sub-second gap rather than a whole span.
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (foreground == event.packageName) close(event.timeStamp)
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> close(event.timeStamp)
            }
        }

        // Whatever is on screen right now has no closing event yet.
        close(now)
        return totals
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
