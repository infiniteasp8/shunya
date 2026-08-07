package dev.anantpandey.shunya.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Monospace everywhere, no exceptions.
 *
 * Equal-width glyphs are not a stylistic tic here — the whole interaction model
 * is typing a name into darkness, and a fixed advance width makes that act
 * visible. Hierarchy comes from weight and tracking rather than from swapping
 * families, which also means no font files to ship and nothing to fall back to.
 */
object Type {
    private val Mono = FontFamily.Monospace

    val Clock = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.ExtraLight,
        fontSize = 84.sp,
        lineHeight = 84.sp,
        letterSpacing = (-0.05).em,
    )

    val Date = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.3.em,
    )

    /** Tier-0 entries on the home screen. */
    val AppName = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        letterSpacing = 0.015.em,
    )

    /** What you type into. */
    val Entry = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Light,
        fontSize = 29.sp,
        letterSpacing = 0.01.em,
    )

    /** Resolved matches echoed back beneath the entry field. */
    val Echo = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = 0.015.em,
    )

    /** Wide-tracked micro-labels. Lowercase, never shouted. */
    val Label = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.26.em,
    )

    val GateName = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        letterSpacing = (-0.01).em,
    )

    val GateStat = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.em,
    )

    val Body = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Start,
    )
}
