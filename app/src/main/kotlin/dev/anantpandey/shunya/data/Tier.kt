package dev.anantpandey.shunya.data

/**
 * How much deliberation an app costs to reach.
 *
 * The tiers exist because uniform friction does not work. A launcher that makes
 * everything hard gets uninstalled inside a fortnight — you need the camera in
 * two seconds when something is happening in front of you. Friction only
 * changes behaviour when it is spent precisely, on the handful of apps you open
 * without having decided to.
 */
enum class Tier {
    /**
     * Visible on the home screen, one tap. Reserved for apps you reach for
     * because something in the world demanded it: phone, camera, maps.
     */
    ZERO,

    /**
     * Invisible, but freely searchable. Type three characters, prefix-match,
     * press enter. Costs a couple of seconds of intent and nothing more.
     */
    ONE,

    /**
     * Invisible *and* unsearchable by prefix. Nothing appears until the name is
     * typed in full, exactly — no autocomplete, no partial reward, no
     * near-miss. Then the gate.
     */
    TWO,
    ;

    companion object {
        /**
         * Anything the user has not explicitly sorted lands here. Defaulting to
         * ONE rather than TWO matters: a launcher that gates apps you never
         * asked it to gate is a launcher you fight, and you only get one chance
         * at that impression.
         */
        val Default = ONE
    }
}
