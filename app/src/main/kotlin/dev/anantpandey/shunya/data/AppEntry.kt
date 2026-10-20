package dev.anantpandey.shunya.data

/**
 * One launchable app, as the launcher thinks about it.
 *
 * Note what is absent: there is no icon. Nothing in this model can carry a
 * colour or a shape, because recognising a logo is pre-conscious in a way that
 * reading a word is not — that asymmetry is the entire mechanism, so it is
 * enforced in the data layer rather than left to the discipline of whoever
 * writes the next screen.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val tier: Tier,
) {
    /**
     * What the user actually types against. Lowercased and stripped of spaces
     * and punctuation, so "Google Maps" is reached by typing `googlemaps` and
     * "X (formerly Twitter)" does not require parentheses.
     */
    val matchKey: String = label.lowercase().filter { it.isLetterOrDigit() }

    /** How the name is rendered. Lowercase throughout; the app never shouts. */
    val display: String = label.lowercase()

    /**
     * Tells two apps with the same label apart.
     *
     * The publisher-ish segment of the package name: `google` for
     * `com.google.android.apps.authenticator2`, `azure` for
     * `com.azure.authenticator`. It is only ever shown beside a name that
     * another visible row shares, and the search screen falls back to the whole
     * package name when even this collides. Still no icon.
     */
    val hint: String = packageName
        .split('.')
        .dropWhile { it in GenericPackageSegments }
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: packageName
}

/** Leading package segments that say where a developer registered, not who they are. */
private val GenericPackageSegments = setOf(
    "com", "org", "net", "io", "app", "dev", "me", "co", "in", "uk", "de", "fr",
    "jp", "cn", "ru", "br", "tv", "ai", "xyz", "info", "biz", "android",
)
