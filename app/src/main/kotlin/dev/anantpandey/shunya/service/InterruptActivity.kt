package dev.anantpandey.shunya.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import dev.anantpandey.shunya.container
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.data.DailyUsage
import dev.anantpandey.shunya.data.GraceTracker
import dev.anantpandey.shunya.data.LauncherConfig
import dev.anantpandey.shunya.data.Tier
import dev.anantpandey.shunya.ui.gate.GateScreen
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.ShunyaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The gate, thrown over whatever app you are currently inside.
 *
 * Launched as an activity rather than a `SYSTEM_ALERT_WINDOW` overlay. That
 * choice drops an entire permission the user would otherwise have to grant, and
 * a permission Play reviews with suspicion — an accessibility service is
 * already exempt from background activity launch limits, so the overlay would
 * buy nothing but scrutiny.
 *
 * Reuses [GateScreen] exactly as the launcher does. Meeting the identical
 * screen whichever door you came through is the point: no route into the feed
 * is quietly cheaper than the others.
 */
class InterruptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val target = intent.getStringExtra(ExtraPackage)
        val itemsSeen = intent.getIntExtra(ExtraItemsSeen, 0)

        if (target == null) {
            finish()
            return
        }

        // Back is not an escape hatch out of the gate — it means "not now".
        onBackPressedDispatcher.addCallback(this) { leave(target) }

        setContent {
            ShunyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Palette.Void),
                    color = Palette.Void,
                ) {
                    val config by container.settings.config
                        .collectAsState(initial = LauncherConfig())

                    val usage by produceState(initialValue = DailyUsage.Unknown, target) {
                        value = withContext(Dispatchers.IO) { container.usage.today(target) }
                    }

                    GateScreen(
                        app = AppEntry(target, labelFor(target), Tier.TWO),
                        usage = usage,
                        usageAvailable = container.usage.hasPermission(),
                        holdSeconds = config.holdSeconds,
                        itemsSeen = itemsSeen,
                        onPass = { resume(target) },
                        onDismiss = { leave(target) },
                    )
                }
            }
        }
    }

    private fun labelFor(packageName: String): String =
        runCatching {
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
        }.getOrDefault(packageName.substringAfterLast('.'))

    /** Hold completed. Grant grace and get out of the way. */
    private fun resume(packageName: String) {
        GraceTracker.grant(packageName)
        finish()
    }

    /**
     * "Not now" goes to the home screen rather than merely closing this
     * activity, which would drop you straight back into the feed you were just
     * asked to leave.
     */
    private fun leave(packageName: String) {
        GraceTracker.revoke(packageName)
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        finish()
    }

    companion object {
        const val ExtraPackage = "package"
        const val ExtraItemsSeen = "items_seen"
    }
}
