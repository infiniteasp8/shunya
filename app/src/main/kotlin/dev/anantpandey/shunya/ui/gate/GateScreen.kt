package dev.anantpandey.shunya.ui.gate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.data.DailyUsage
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * The gate.
 *
 * Three things happen here, in this order, and the order is the design:
 *
 *  1. **You are told what today already looks like** — before you are asked to
 *     decide, not after. A count you read at the door is a count that can still
 *     change your mind.
 *  2. **You have to say what you came for.** Not stored, not judged, not scored.
 *     It exists to make you articulate an intention, because an intention you
 *     cannot finish typing is usually not one you had.
 *  3. **You hold, for ten seconds.** Release early and it resets to zero.
 *
 * The asymmetry at the bottom is deliberate: *not now* is a wide, easy target,
 * and the way in is a small circle you must keep a thumb on. Every gate that
 * makes leaving as hard as entering ends up training people to push through.
 */
@Composable
fun GateScreen(
    app: AppEntry,
    usage: DailyUsage,
    usageAvailable: Boolean,
    holdSeconds: Int,
    onPass: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Non-zero when this gate is a mid-scroll interruption rather than a door.
     * It changes what the screen says, and it removes the reason prompt —
     * asking someone to justify themselves twenty reels deep is moralising, and
     * moralising is what gets an app deleted.
     */
    itemsSeen: Int = 0,
) {
    val interrupted = itemsSeen > 0
    var reason by remember { mutableStateOf("") }
    val armed = interrupted || reason.trim().length >= MinReasonLength

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Void)
            .imePadding()
            .padding(horizontal = 30.dp, vertical = 34.dp),
    ) {
        Spacer(Modifier.height(96.dp))

        Text(text = app.display, style = Type.GateName, color = Palette.Ember)
        Spacer(Modifier.height(16.dp))

        Text(
            text = when {
                interrupted -> "$itemsSeen in a row\nstill here?"
                usageAvailable ->
                    "opened ${usage.opens} times today\n${usage.minutes} minutes since midnight"
                // Honest about not knowing, rather than showing a confident zero.
                else -> "usage access is off\nthis gate is flying blind"
            },
            style = Type.GateStat,
            color = Palette.Ash,
        )

        if (!interrupted) {
            Spacer(Modifier.height(40.dp))

            Text(text = "what for?", style = Type.Label, color = Palette.Ash)
            Spacer(Modifier.height(12.dp))

            BasicTextField(
                value = reason,
                onValueChange = { reason = it.filterNot { c -> c == '\n' } },
                singleLine = true,
                textStyle = Type.Echo.copy(color = Palette.Ink),
                cursorBrush = SolidColor(Palette.Ember),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = if (armed) Palette.Ember else Palette.Hair,
                            start = Offset(0f, size.height + 10.dp.toPx()),
                            end = Offset(size.width, size.height + 10.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            HoldRing(
                enabled = armed,
                seconds = holdSeconds,
                onComplete = onPass,
            )

            Text(
                text = if (interrupted) "enough" else "not now",
                style = Type.Label,
                color = Palette.Ash,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    // A generous target. Leaving should never be the fiddly option.
                    .padding(horizontal = 48.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * Hold for [seconds] to pass. Lifting resets to zero rather than pausing —
 * a progress bar you can chip away at in four goes is not a ten-second wait.
 */
@Composable
private fun HoldRing(
    enabled: Boolean,
    seconds: Int,
    onComplete: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var holding by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) progress.snapTo(0f)
    }

    LaunchedEffect(holding, enabled) {
        if (holding && enabled) {
            val remaining = ((1f - progress.value) * seconds * 1000).toInt()
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            if (progress.value >= 1f) onComplete()
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(108.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    holding = true
                    waitForUpOrCancellation()
                    holding = false
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)

            drawArc(
                color = Palette.Hair,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )

            if (progress.value > 0f) {
                drawArc(
                    color = Palette.Ember,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        val remaining = seconds - (progress.value * seconds).toInt()
        Text(
            text = if (holding && enabled) "${remaining}s" else "hold",
            style = Type.Label,
            color = when {
                !enabled -> Palette.Hair
                else -> Palette.Ember
            },
        )
    }
}

/**
 * Three characters. Enough to require an actual word, not so much that it
 * becomes an essay you resent — the prompt is a speed bump, not a confession.
 */
private const val MinReasonLength = 3
