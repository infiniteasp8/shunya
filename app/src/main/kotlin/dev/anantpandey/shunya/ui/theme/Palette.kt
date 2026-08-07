package dev.anantpandey.shunya.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Five values, and only five.
 *
 * [Ember] is the single chromatic value in the entire app and it is spent
 * exclusively on friction — the caret you are typing into, and the gate. If
 * colour ever appears anywhere else, something is being sold to the user.
 */
object Palette {
    /** True black. On an OLED panel these pixels are switched off. */
    val Void = Color(0xFF000000)

    /** Warm off-white. Pure white glares at midnight; this is biased to paper. */
    val Ink = Color(0xFFE8E6E1)

    /** Secondary text. Warm-biased grey, so it reads as chosen rather than default. */
    val Ash = Color(0xFF4A4844)

    /** Hairlines, placeholders, disabled states. */
    val Hair = Color(0xFF2A2925)

    /** Burnt orange. Reserved for friction. Never decorative. */
    val Ember = Color(0xFFC4633A)
}
