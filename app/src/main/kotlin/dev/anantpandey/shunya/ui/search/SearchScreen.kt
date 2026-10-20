package dev.anantpandey.shunya.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.data.Resolution
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.Type

/**
 * Type-to-open.
 *
 * The critical detail is not on screen — it is in the keyboard options below.
 * `KeyboardCapitalization.None`, `autoCorrect = false` and a password-adjacent
 * IME type are what stop Gboard from finishing the word for you. Without them
 * the phone's own autocomplete hands back all nine keystrokes of "instagram"
 * after three, and the mechanism this entire app is built on evaporates.
 */
@Composable
fun SearchScreen(
    query: String,
    resolution: Resolution,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPick: (AppEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Void)
            // The back gesture closes the keyboard before the launcher ever
            // sees it, which leaves a black screen whose only live target is
            // one line of text. Tapping the void brings the keyboard back.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            .imePadding()
            .padding(horizontal = 30.dp, vertical = 34.dp),
    ) {
        Spacer(Modifier.weight(0.8f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // The one place ember appears outside the gate: what you type into.
            Box(
                Modifier
                    .width(9.dp)
                    .height(30.dp)
                    .background(Palette.Ember),
            )
            Spacer(Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = Type.Entry.copy(color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Ember.copy(alpha = 0f)),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }

        Spacer(Modifier.height(24.dp))
        Echo(resolution, onPick)
        Spacer(Modifier.weight(1.2f))

        Text(
            text = "← back",
            style = Type.Label,
            color = Palette.Ash,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                )
                .padding(vertical = 12.dp),
        )
    }
}

/**
 * What the query resolved to.
 *
 * The rows are words, not icons, and they exist only because you typed. That
 * is the whole distinction: recognising a logo on a grid is pre-conscious,
 * reading a word you just summoned is not. So a row can be tapped — it has to
 * be, because two apps called *authenticator* are never separated by another
 * keystroke — without reopening the loop the field exists to break.
 *
 * *Go* still does the deliberate thing. It opens the one row that is clearly
 * meant, which is the sole match or the sole exact match, and otherwise does
 * nothing while the note underneath says why.
 */
@Composable
private fun Echo(resolution: Resolution, onPick: (AppEntry) -> Unit) {
    Column(modifier = Modifier.heightIn(min = 76.dp)) {
        when (resolution) {
            // Silence. Not "no results" — nothing at all.
            is Resolution.Nothing -> Unit

            is Resolution.Matches -> {
                val hints = resolution.hints()
                resolution.shown.forEach { app ->
                    Match(
                        app = app,
                        hint = hints[app.packageName],
                        primary = app == resolution.primary,
                        onClick = { onPick(app) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Note(resolution.note(), Palette.Ash)
            }

            is Resolution.Gated -> {
                Match(
                    app = resolution.app,
                    hint = null,
                    primary = true,
                    onClick = { onPick(resolution.app) },
                )
                Spacer(Modifier.height(10.dp))
                Note("go to continue", Palette.Ember)
            }
        }
    }
}

/**
 * A hint beside every row whose name another visible row shares. The package's
 * publisher segment usually separates them; when even that collides, the whole
 * package name does. Rows with a name of their own get nothing — the hint is a
 * tiebreaker, not a subtitle.
 */
private fun Resolution.Matches.hints(): Map<String, String> {
    val hints = mutableMapOf<String, String>()
    shown.groupBy { it.display }.values
        .filter { group -> group.size > 1 }
        .forEach { group ->
            val distinct = group.map { it.hint }.toSet().size == group.size
            group.forEach { app ->
                hints[app.packageName] = if (distinct) app.hint else app.packageName
            }
        }
    return hints
}

private fun Resolution.Matches.note(): String = when {
    primary != null -> "go to open"
    // Every row is the same name: no keystroke will ever separate them.
    hidden == 0 && shown.distinctBy { it.matchKey }.size == 1 -> "same name · tap one"
    hidden > 0 -> "+$hidden more · keep typing"
    else -> "keep typing · or tap"
}

@Composable
private fun Match(
    app: AppEntry,
    hint: String?,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // No ripple. A tap that flashes is a tap that rewards.
                indication = null,
                onClick = onClick,
            )
            // Tall enough for a thumb; the text itself is only 17sp.
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = app.display,
            style = Type.Echo,
            // The row go would open is the only one at full ink.
            color = Palette.Ink.copy(alpha = if (primary) 1f else 0.46f),
            modifier = Modifier.alignByBaseline(),
        )

        if (hint != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "· $hint",
                style = Type.Label,
                color = Palette.Ash,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun Note(text: String, color: Color) {
    Text(
        text = text,
        style = Type.Label,
        color = color,
        // Announced when it changes, so "go to open" is heard, not only seen.
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
