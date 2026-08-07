package dev.anantpandey.shunya.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/** A launchable app as the system reports it, before tiers are applied. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Reads the set of launchable apps off the device.
 *
 * Uses a scoped `<queries>` declaration rather than `QUERY_ALL_PACKAGES`, which
 * needs a Play Store justification form and is turned down for most requests.
 * A launcher only needs to see things that declare themselves launchable, so
 * that is all it asks for.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager get() = appContext.packageManager
    private val selfPackage = appContext.packageName

    fun launchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            // The launcher is not an app you launch. Hiding it from itself also
            // means there is no way to accidentally recurse into the home screen.
            .filter { (pkg, _) -> pkg != selfPackage }
            .distinctBy { (pkg, _) -> pkg }
            .map { (pkg, label) -> InstalledApp(pkg, label) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Applies the user's tier assignments to what is actually installed. */
    fun entries(config: LauncherConfig): List<AppEntry> =
        launchableApps().map { AppEntry(it.packageName, it.label, config.tierOf(it.packageName)) }

    fun launchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    /**
     * Which apps should be on the home screen before the user has said.
     *
     * Resolved by role — whichever app currently answers "dial a number" or
     * "take a photo" — rather than by hardcoded package name, so it is correct
     * on a Motorola, a Pixel, or a Samsung without a lookup table per OEM.
     */
    fun defaultTierZero(): List<String> {
        val installed = launchableApps().map { it.packageName }.toSet()

        val byRole = KnownApps.HomeScreenRoles.mapNotNull { action ->
            packageManager
                .resolveActivity(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
                ?.takeIf { it.isRealHandler() }
                ?.activityInfo
                ?.packageName
                ?.takeIf { it in installed }
        }

        return byRole.distinct().take(MaxTierZero)
    }

    /** Short-video and feed apps that are actually present on this device. */
    fun defaultTierTwo(): Set<String> {
        val installed = launchableApps().map { it.packageName }.toSet()
        return KnownApps.Gated intersect installed
    }

    /** The system's "no default chosen" placeholder is not a real handler. */
    private fun ResolveInfo.isRealHandler(): Boolean =
        activityInfo?.packageName?.let { it != "android" } == true

    companion object {
        /**
         * Six is the ceiling, and it is a design constraint rather than a
         * layout one. A home screen you scan is a home screen you browse.
         */
        const val MaxTierZero = 6
    }
}
