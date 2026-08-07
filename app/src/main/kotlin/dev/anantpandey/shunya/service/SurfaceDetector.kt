package dev.anantpandey.shunya.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides whether the screen currently showing is an infinite short-video feed.
 *
 * This is the part that makes the product survivable. Blocking whole apps is
 * the obvious design and it is why these tools get uninstalled in week three:
 * Instagram is also how you answer a friend, YouTube is also how you find out
 * why your tap is leaking. Take those away and the user does not become
 * focused, they just remove the launcher.
 *
 * So the unit of interception is the *surface*, not the app. Reels is gated;
 * Instagram DMs are not. Shorts is gated; a YouTube search is not.
 *
 * Detection works off view-id resource names, which is the only signal an
 * accessibility service can read cheaply and reliably. These IDs belong to
 * other people's apps and will drift when they redesign — [FeedSurfaces] is
 * therefore treated as a maintained table, not as fact, and every failure mode
 * is "let the user through" rather than "block the wrong thing".
 */
object SurfaceDetector {

    /**
     * Per package, the view ids that only exist while a short-video feed is on
     * screen. Matching is by suffix so the caller does not have to repeat the
     * package prefix, and so a renamed package still matches.
     */
    private val FeedSurfaces: Map<String, List<String>> = mapOf(
        "com.instagram.android" to listOf(
            "clips_viewer_view_pager",   // the Reels pager itself
            "clips_viewer_root",
            "clips_tab",
        ),
        "com.google.android.youtube" to listOf(
            "reel_recycler",             // the Shorts pager
            "reel_watch_fragment_root",
            "shorts_container",
        ),
        "com.facebook.katana" to listOf(
            "reels_viewer_root",
            "video_home_tab",
        ),
        "com.snapchat.android" to listOf(
            "spotlight_view_pager",
        ),
        // TikTok and Moj are feed-first: there is no non-feed surface worth
        // preserving, so the package alone is sufficient evidence.
        "com.zhiliaoapp.musically" to emptyList(),
        "com.ss.android.ugc.trill" to emptyList(),
        "in.mohalla.video" to emptyList(),
    )

    val WatchedPackages: Set<String> = FeedSurfaces.keys

    fun isWatched(packageName: String?): Boolean = packageName in FeedSurfaces

    /**
     * True when [root] is showing the feed for [packageName].
     *
     * An empty id list means the whole app counts. Otherwise we look for any of
     * the known feed containers; if none are present the user is somewhere
     * benign — messages, search, their own profile — and is left alone.
     */
    fun isFeedVisible(packageName: String?, root: AccessibilityNodeInfo?): Boolean {
        val ids = FeedSurfaces[packageName] ?: return false
        if (ids.isEmpty()) return true
        val node = root ?: return false

        // Nodes are reclaimed by the framework; explicit recycle() has been a
        // deprecated no-op since API 33 and is unsafe to call on shared nodes.
        return ids.any { id ->
            runCatching {
                node.findAccessibilityNodeInfosByViewId("$packageName:id/$id")
            }.getOrNull().orEmpty().any { it.isVisibleToUser }
        }
    }
}
