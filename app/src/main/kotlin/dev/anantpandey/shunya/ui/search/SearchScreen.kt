package dev.anantpandey.shunya.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.SolidColor
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Void)
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

        Spacer(Modifier.height(34.dp))
        Echo(resolution)
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
 * Nothing here is tappable. If you want the app you finish typing its name and
 * press go — reintroducing a target to aim at would restore exactly the
 * recognise-and-tap loop the search field exists to break.
 */
@Composable
private fun Echo(resolution: Resolution) {
    Column(
        modifier = Modifier.heightIn(min = 76.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (resolution) {
            // Silence. Not "no results" — nothing at all.
            is Resolution.Nothing -> Unit

            is Resolution.Unique -> {
                Match(resolution.app)
                Note("go to open", Palette.Ash)
            }

            is Resolution.Several -> {
                resolution.apps.forEach { Match(it) }
                Note("keep typing", Palette.Ash)
            }

            is Resolution.Gated -> {
                Match(resolution.app)
                Note("go to continue", Palette.Ember)
            }
        }
    }
}

@Composable
private fun Match(app: AppEntry) {
    Text(
        text = app.display,
        style = Type.Echo,
        color = Palette.Ink.copy(alpha = 0.46f),
    )
}

@Composable
private fun Note(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = Type.Label, color = color)
}
