package dev.anantpandey.shunya.data

/**
 * Package names the launcher recognises out of the box.
 *
 * These are seeds for first run, not a permanent blocklist — every one of them
 * can be moved between tiers by the user. Shipping sensible defaults matters
 * because a launcher that opens on an unsorted list of 140 apps is a chore, and
 * a chore gets abandoned before it can prove itself.
 */
object KnownApps {

    /**
     * Apps whose primary surface is an infinite feed of short video. These are
     * seeded into [Tier.TWO], and they are also what the interception service
     * watches — though it watches for the *feed*, not the app.
     */
    val ShortVideo = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",   // TikTok
        "com.ss.android.ugc.trill",   // TikTok, some regions
        "com.snapchat.android",
        "com.facebook.katana",
        "in.mohalla.sharechat",
        "in.mohalla.video",           // Moj
        "com.google.android.apps.youtube.music",
    )

    /** Feeds without the video, but with the same pull. Also seeded to [Tier.TWO]. */
    val InfiniteFeed = setOf(
        "com.reddit.frontpage",
        "com.twitter.android",
        "com.linkedin.android",
        "com.quora.android",
        "com.pinterest",
    )

    /** Everything that earns a gate on first run. */
    val Gated: Set<String> = ShortVideo + InfiniteFeed

    /**
     * Intents whose default handler should sit on the home screen. Resolved at
     * runtime rather than hardcoded to package names, because the right dialer
     * is whichever one this phone actually uses.
     */
    val HomeScreenRoles = listOf(
        android.content.Intent.ACTION_DIAL,
        android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
    )
}
