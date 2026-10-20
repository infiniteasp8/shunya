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

    /** Field is empty, too short to have earned an answer, or matches nothing. */
    data object Nothing : Resolution

    /**
     * One or more tier-zero or tier-one matches, exact matches first.
     *
     * Every shown row can be tapped. [primary] is what *go* opens: the sole
     * match, or the sole exact match when the others merely share its prefix.
     * It is null when nothing short of a tap or another character can settle
     * it — two apps with the same name, or a prefix that is still open.
     */
    data class Matches(
        val shown: List<AppEntry>,
        /** Matches beyond [MAX_SHOWN], counted rather than listed. */
        val hidden: Int,
        val primary: AppEntry?,
    ) : Resolution

    /** A tier-two app, matched in full and exactly. Go takes you to the gate. */
    data class Gated(val app: AppEntry) : Resolution
}

/**
 * Resolves what the user has typed against the installed apps.
 *
 * Three rules do all the work:
 *
 *  1. **Tier two requires an exact, complete match.** No prefixes. Typing eight
 *     of the nine letters of `instagram` resolves to [Resolution.Nothing].
 *  2. **Everything else needs three characters and matches by prefix only** —
 *     never substring, never fuzzy. Fuzzy matching is a convenience feature
 *     that quietly hands back every keystroke this design is trying to charge.
 *  3. **A complete name always wins.** `phone` opens *Phone* even though
 *     *Phone Link* shares the prefix, and `x` reaches *X* even though it is
 *     shorter than the prefix minimum. Typing the whole name is the most
 *     deliberate thing the field can be asked for; it must never be a dead end.
 *
 * What this does not do is pretend ambiguity away. Two apps that are both
 * called *Authenticator* match together on every keystroke, and no amount of
 * typing separates them. They are returned side by side with [Resolution.Matches.primary]
 * unset, and the screen lets the user tap one.
 */
fun List<AppEntry>.resolve(rawQuery: String): Resolution {
    val query = rawQuery.lowercase().filter { it.isLetterOrDigit() }
    if (query.isEmpty()) return Resolution.Nothing

    // Tier two first, so a gated app is never reachable through the open path.
    firstOrNull { it.tier == Tier.TWO && it.matchKey == query }
        ?.let { return Resolution.Gated(it) }

    val open = filter { it.tier != Tier.TWO }
    val exact = open.filter { it.matchKey == query }.sortedBy { it.hint }

    // Prefix matching has to earn three characters. A complete name, however
    // short, has already earned more than that.
    val prefix = if (query.length < MIN_PREFIX) {
        emptyList()
    } else {
        open.filter { it.matchKey != query && it.matchKey.startsWith(query) }
            .sortedBy { it.matchKey }
    }

    val hits = exact + prefix
    if (hits.isEmpty()) return Resolution.Nothing

    val primary = when {
        hits.size == 1 -> hits.single()
        exact.size == 1 -> exact.single()
        else -> null
    }

    return Resolution.Matches(
        shown = hits.take(MAX_SHOWN),
        hidden = (hits.size - MAX_SHOWN).coerceAtLeast(0),
        primary = primary,
    )
}

private const val MIN_PREFIX = 3

/**
 * Three rows. Enough to tell *calendar* from *calculator*; not enough to
 * browse. Anything past this is counted, not listed — "+4 more" is a nudge to
 * keep typing, and a list of seven is a menu.
 */
private const val MAX_SHOWN = 3
