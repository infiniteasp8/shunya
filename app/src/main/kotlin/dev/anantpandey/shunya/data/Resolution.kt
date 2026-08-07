package dev.anantpandey.shunya.data

/**
 * The outcome of typing something into the search field.
 *
 * [Nothing] is the load-bearing case. When you type `instagra` the launcher
 * shows a blank screen — not "no results", not a greyed-out suggestion, not the
 * app with a lock icon beside it. Absolutely nothing. A near-miss that displays
 * *something* still pays out, and a partial payout is enough to keep the loop
 * alive.
 */
sealed interface Resolution {

    /** Field is empty, or too short to have earned an answer yet. */
    data object Nothing : Resolution

    /** Exactly one prefix match. Enter opens it. */
    data class Unique(val app: AppEntry) : Resolution

    /** A few prefix matches. Shown, but not tappable — keep typing. */
    data class Several(val apps: List<AppEntry>) : Resolution

    /** A tier-two app, matched in full and exactly. Enter goes to the gate. */
    data class Gated(val app: AppEntry) : Resolution
}

/**
 * Resolves what the user has typed against the installed apps.
 *
 * Two rules do all the work:
 *
 *  1. **Tier two requires an exact, complete match.** No prefixes. Typing eight
 *     of the nine letters of `instagram` resolves to [Resolution.Nothing].
 *  2. **Everything else needs three characters and matches by prefix only** —
 *     never substring, never fuzzy. Fuzzy matching is a convenience feature
 *     that quietly hands back every keystroke this design is trying to charge.
 */
fun List<AppEntry>.resolve(rawQuery: String): Resolution {
    val query = rawQuery.lowercase().filter { it.isLetterOrDigit() }
    if (query.isEmpty()) return Resolution.Nothing

    firstOrNull { it.tier == Tier.TWO && it.matchKey == query }
        ?.let { return Resolution.Gated(it) }

    if (query.length < MIN_PREFIX) return Resolution.Nothing

    val hits = filter { it.tier != Tier.TWO && it.matchKey.startsWith(query) }

    return when {
        hits.isEmpty() -> Resolution.Nothing
        hits.size == 1 -> Resolution.Unique(hits.single())
        hits.size <= MAX_SHOWN -> Resolution.Several(hits.sortedBy { it.matchKey })
        // Too broad to be a decision yet. Say nothing rather than offer a menu.
        else -> Resolution.Nothing
    }
}

private const val MIN_PREFIX = 3
private const val MAX_SHOWN = 3
