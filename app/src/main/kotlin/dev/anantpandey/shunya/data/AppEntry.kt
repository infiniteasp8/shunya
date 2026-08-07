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
}
