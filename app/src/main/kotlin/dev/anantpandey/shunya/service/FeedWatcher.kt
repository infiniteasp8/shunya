package dev.anantpandey.shunya.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import dev.anantpandey.shunya.container
import dev.anantpandey.shunya.data.GraceTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Watches for short-video feeds and interrupts them.
 *
 * This exists because the launcher alone cannot work. The home screen only
 * governs `unlock → home → tap`, and that is very likely not how anyone
 * actually ends up in Reels at midnight — the real doors are a notification, a
 * shared link, and the recents switcher, none of which a launcher ever sees.
 * An accessibility service sees all of them.
 *
 * Two interventions, deliberately different in kind:
 *
 *  - **At the door.** Landing on the feed without a grace window throws the
 *    gate, exactly as the launcher would have.
 *  - **Mid-scroll.** After a threshold number of items the gate returns, even
 *    though you were let in legitimately. This is the one that matters: the
 *    damage is not done in minute one, when you still had a reason. It is done
 *    in minute twenty-five, when the reason is long gone and nothing in the
 *    interface has any interest in telling you.
 *
 * Scope is constrained to [SurfaceDetector.WatchedPackages] in both the XML
 * config and at runtime. The service is never delivered an event from a
 * banking app, a password manager, or a keyboard — it cannot read what it is
 * not sent, which is a stronger privacy guarantee than a promise not to look.
 */
class FeedWatcher : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var lastScrollAt = 0L
    private var lastInterruptAt = 0L
    private var currentPackage: String? = null

    private var interruptAfterItems = 20

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
            packageNames = SurfaceDetector.WatchedPackages.toTypedArray()
        }

        scope.launch {
            interruptAfterItems = container.settings.config.first().feedInterruptItems
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        if (!SurfaceDetector.isWatched(pkg)) return

        // Leaving a watched app ends its scroll count. Coming back starts over.
        if (pkg != currentPackage) {
            currentPackage = pkg
            GraceTracker.resetFeedItems(pkg)
        }

        val onFeed = SurfaceDetector.isFeedVisible(pkg, rootInActiveWindow)
        if (!onFeed) return

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            -> {
                if (!GraceTracker.isWithinGrace(pkg)) interrupt(pkg, Reason.Entered)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrolled(pkg)
        }
    }

    private fun onScrolled(pkg: String) {
        val now = SystemClock.elapsedRealtime()

        // A single swipe emits a burst of scroll events. Debounce so one flick
        // counts as one reel rather than fifteen.
        if (now - lastScrollAt < ScrollDebounceMillis) return
        lastScrollAt = now

        val seen = GraceTracker.recordFeedItem(pkg)
        if (seen < interruptAfterItems) return

        GraceTracker.resetFeedItems(pkg)
        interrupt(pkg, Reason.Threshold(seen))
    }

    private fun interrupt(pkg: String, reason: Reason) {
        val now = SystemClock.elapsedRealtime()
        // Never stack gates. A user buried under three overlays force-quits the
        // app and then disables the service, and we have lost them for good.
        if (now - lastInterruptAt < InterruptCooldownMillis) return
        lastInterruptAt = now

        GraceTracker.revoke(pkg)

        startActivity(
            Intent(this, InterruptActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
                putExtra(InterruptActivity.ExtraPackage, pkg)
                putExtra(InterruptActivity.ExtraItemsSeen, (reason as? Reason.Threshold)?.count ?: 0)
            },
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private sealed interface Reason {
        data object Entered : Reason
        data class Threshold(val count: Int) : Reason
    }

    private companion object {
        /** One deliberate swipe, roughly. Below this it is the same gesture. */
        const val ScrollDebounceMillis = 450L

        /** Space between gates, so an interruption never becomes a pile-up. */
        const val InterruptCooldownMillis = 3_000L
    }
}
