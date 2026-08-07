package dev.anantpandey.shunya.data

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers that you just came through a gate, so you are not gated again on
 * the way back from a photo picker or a share sheet.
 *
 * Without this the launcher becomes unusable in exactly the situation where it
 * should get out of the way: you passed the gate, decided honestly to reply to
 * a message, tapped an image, and came back to a ten-second hold. That is the
 * kind of friction that gets an app uninstalled, and it protects nothing.
 *
 * Deliberately in-memory and deliberately on [SystemClock.elapsedRealtime]:
 *
 *  - **In-memory**, so a reboot revokes every grace. Losing them is the safe
 *    failure — it costs a hold you already earned, rather than silently opening
 *    a door that should be shut.
 *  - **elapsedRealtime**, because wall-clock time can be moved. A stored
 *    `System.currentTimeMillis()` deadline is defeated by changing the system
 *    clock, and the person motivated to try that is the person this is for.
 *
 * Shared between the launcher and the interception service, which run in the
 * same process — passing the gate at the door must also satisfy the watcher
 * inside the app.
 */
object GraceTracker {

    private val grantedUntil = ConcurrentHashMap<String, Long>()

    /** Feed items seen since the current grace began, keyed by package. */
    private val feedItemsSeen = ConcurrentHashMap<String, Int>()

    fun grant(packageName: String, minutes: Int = DefaultGraceMinutes) {
        grantedUntil[packageName] = SystemClock.elapsedRealtime() + minutes * 60_000L
        feedItemsSeen[packageName] = 0
    }

    fun isWithinGrace(packageName: String): Boolean {
        val until = grantedUntil[packageName] ?: return false
        if (SystemClock.elapsedRealtime() >= until) {
            revoke(packageName)
            return false
        }
        return true
    }

    fun revoke(packageName: String) {
        grantedUntil.remove(packageName)
        feedItemsSeen.remove(packageName)
    }

    fun revokeAll() {
        grantedUntil.clear()
        feedItemsSeen.clear()
    }

    /** Returns the running count after recording one more feed item. */
    fun recordFeedItem(packageName: String): Int =
        feedItemsSeen.merge(packageName, 1, Int::plus) ?: 1

    fun feedItems(packageName: String): Int = feedItemsSeen[packageName] ?: 0

    fun resetFeedItems(packageName: String) {
        feedItemsSeen[packageName] = 0
    }

    /**
     * Five minutes. Long enough to cover a genuine errand inside the app,
     * short enough that a session which drifted into scrolling meets the gate
     * again rather than running unchecked to the end of the evening.
     */
    const val DefaultGraceMinutes = 5
}
