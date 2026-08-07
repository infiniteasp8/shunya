package dev.anantpandey.shunya.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** What the gate shows you about today, before you decide. */
data class DailyUsage(val opens: Int, val minutes: Long) {
    companion object {
        val Unknown = DailyUsage(opens = 0, minutes = 0)
    }
}

/**
 * Reads today's usage straight from Android.
 *
 * Nothing is cached and nothing is written to disk. The gate asks the system
 * what happened since midnight at the moment it needs to know, which keeps this
 * app from building the one asset it should never have: a longitudinal record
 * of someone's worst hours.
 */
class UsageReader(context: Context) {

    private val appContext = context.applicationContext

    private val usageStats: UsageStatsManager? =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * PACKAGE_USAGE_STATS is an appop, not a runtime permission — it cannot be
     * requested with a dialog, only granted by the user in system settings.
     */
    fun hasPermission(): Boolean {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false

        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun today(packageName: String): DailyUsage {
        val manager = usageStats ?: return DailyUsage.Unknown
        if (!hasPermission()) return DailyUsage.Unknown

        val start = startOfToday()
        val now = System.currentTimeMillis()

        var opens = 0
        var foregroundMillis = 0L
        var resumedAt = 0L

        val events = runCatching { manager.queryEvents(start, now) }.getOrNull()
            ?: return DailyUsage.Unknown
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Only count a fresh entry, not a resume within the same visit.
                    if (resumedAt == 0L) opens++
                    resumedAt = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (resumedAt != 0L) {
                        foregroundMillis += event.timeStamp - resumedAt
                        resumedAt = 0L
                    }
                }
            }
        }

        // Still in the foreground right now — count up to this moment.
        if (resumedAt != 0L) foregroundMillis += now - resumedAt

        return DailyUsage(
            opens = opens,
            minutes = TimeUnit.MILLISECONDS.toMinutes(foregroundMillis),
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
