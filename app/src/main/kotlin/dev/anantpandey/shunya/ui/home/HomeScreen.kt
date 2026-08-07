package dev.anantpandey.shunya.ui.home

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.Type
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * The home screen, which is mostly nothing.
 *
 * A clock, a date, and at most six words sitting low in the thumb zone. The
 * large empty region in the middle is not a layout accident — it is the whole
 * point. The instant after you unlock, your reach has to land on a surface that
 * offers you nothing, or the reach never gets interrupted.
 */
@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Void)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swipe up anywhere on the void opens search.
                    if (dragAmount < -SwipeThreshold) onOpenSearch()
                }
            }
            .padding(horizontal = 30.dp, vertical = 34.dp),
    ) {
        Spacer(Modifier.height(72.dp))
        Clock()

        // The void. Everything above sits high, everything below sits in reach.
        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(21.dp)) {
            apps.forEach { app ->
                Text(
                    text = app.display,
                    style = Type.AppName,
                    color = Palette.Ink.copy(alpha = 0.70f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        // No ripple. A tap that flashes is a tap that rewards.
                        indication = null,
                    ) { onOpen(app) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Grip(
            onClick = onOpenSearch,
            onLongClick = onOpenSettings,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            // Wake on the minute boundary rather than every second. Nothing on
            // this screen changes in between, and a launcher has no business
            // spending a wakeup a second on a display that is usually off.
            val millisPastMinute =
                now.get(Calendar.SECOND) * 1000L + now.get(Calendar.MILLISECOND)
            delay(60_000L - millisPastMinute)
        }
    }

    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)

    Text(
        text = "%02d:%02d".format(hour, minute),
        style = Type.Clock,
        color = Palette.Ink,
    )

    Spacer(Modifier.height(18.dp))

    Text(
        text = buildString {
            append(Days[now.get(Calendar.DAY_OF_WEEK) - 1])
            append("  ")
            append("%02d".format(now.get(Calendar.DAY_OF_MONTH)))
            append("  ")
            append(Months[now.get(Calendar.MONTH)])
        },
        style = Type.Date,
        color = Palette.Ash,
    )
}

/**
 * The only affordance on the screen, and it is one hairline tall. Long-pressing
 * it reaches settings — undiscoverable on purpose, because a visible settings
 * entry is one more thing to look at on a screen whose job is to have nothing
 * to look at.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Grip(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 14.dp),
    ) {
        Box(
            Modifier
                .width(34.dp)
                .height(1.dp)
                .background(Palette.Hair),
        )
    }
}

private const val SwipeThreshold = 6f

private val Days = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
private val Months = listOf(
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec",
)
