package dev.anantpandey.shunya.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.data.Tier
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.Type

/**
 * The only screen in the app that is a list, and it is deliberately unpleasant
 * to browse: no icons, no search, no categories. You come here to sort apps
 * once and then not return.
 *
 * Tapping a row cycles its tier. Cycling rather than opening a picker keeps the
 * whole model visible — three states, one gesture — and there is no save
 * button, because a settings screen you have to commit is a settings screen
 * people abandon halfway through.
 */
@Composable
fun SettingsScreen(
    apps: List<AppEntry>,
    usageGranted: Boolean,
    watcherEnabled: Boolean,
    onCycleTier: (AppEntry) -> Unit,
    onGrantUsage: () -> Unit,
    onEnableWatcher: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Void)
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Spacer(Modifier.height(56.dp))
            Text("sort", style = Type.Label, color = Palette.Ash)
            Spacer(Modifier.height(28.dp))
        }

        // Permissions first: the app is largely inert without them, and burying
        // that at the bottom of a list would be a quiet way of misleading.
        if (!usageGranted) {
            item {
                Prompt(
                    title = "grant usage access",
                    detail = "so the gate can show what today already looks like",
                    onClick = onGrantUsage,
                )
            }
        }

        if (!watcherEnabled) {
            item {
                Prompt(
                    title = "enable the feed watcher",
                    detail = "so reels and shorts are caught wherever you enter them",
                    onClick = onEnableWatcher,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        items(apps, key = { it.packageName }) { app ->
            AppRow(app = app, onClick = { onCycleTier(app) })
        }

        item {
            Spacer(Modifier.height(40.dp))
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
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = app.display,
            style = Type.Echo,
            color = when (app.tier) {
                Tier.ZERO -> Palette.Ink
                Tier.ONE -> Palette.Ink.copy(alpha = 0.55f)
                Tier.TWO -> Palette.Ember
            },
        )

        Text(
            text = when (app.tier) {
                Tier.ZERO -> "home"
                Tier.ONE -> "typed"
                Tier.TWO -> "gated"
            },
            style = Type.Label,
            color = if (app.tier == Tier.TWO) Palette.Ember else Palette.Ash,
        )
    }
}

@Composable
private fun Prompt(title: String, detail: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = Type.Echo, color = Palette.Ember)
        Text(text = detail, style = Type.Body, color = Palette.Ash)
    }
}

/** ZERO → ONE → TWO → ZERO. */
fun Tier.next(): Tier = when (this) {
    Tier.ZERO -> Tier.ONE
    Tier.ONE -> Tier.TWO
    Tier.TWO -> Tier.ZERO
}
