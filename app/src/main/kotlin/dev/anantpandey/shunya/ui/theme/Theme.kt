package dev.anantpandey.shunya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ShunyaColorScheme = darkColorScheme(
    primary = Palette.Ember,
    onPrimary = Palette.Void,
    background = Palette.Void,
    onBackground = Palette.Ink,
    surface = Palette.Void,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Void,
    onSurfaceVariant = Palette.Ash,
    outline = Palette.Hair,
    error = Palette.Ember,
)

/**
 * There is no light theme and there will not be one. The launcher committing to
 * a single visual world is the product, not an oversight: a home screen that
 * changes appearance with the ambient light is a home screen you look at.
 */
@Composable
fun ShunyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShunyaColorScheme,
        content = content,
    )
}
