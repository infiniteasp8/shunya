package dev.anantpandey.shunya

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.ui.LauncherViewModel
import dev.anantpandey.shunya.ui.Screen
import dev.anantpandey.shunya.ui.gate.GateScreen
import dev.anantpandey.shunya.ui.home.HomeScreen
import dev.anantpandey.shunya.ui.search.SearchScreen
import dev.anantpandey.shunya.ui.settings.SettingsScreen
import dev.anantpandey.shunya.ui.theme.Palette
import dev.anantpandey.shunya.ui.theme.ShunyaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Back never leaves the launcher — there is nowhere behind a home
        // screen. From search or the gate it returns to the void instead.
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.screen !is Screen.Home) viewModel.goHome()
        }

        setContent {
            ShunyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Palette.Void),
                    color = Palette.Void,
                ) {
                    when (val screen = viewModel.screen) {
                        is Screen.Home -> HomeScreen(
                            apps = viewModel.homeApps,
                            onOpen = { viewModel.open(it, ::launchApp) },
                            onOpenSearch = viewModel::openSearch,
                            onOpenSettings = viewModel::openSettings,
                        )

                        is Screen.Settings -> SettingsScreen(
                            apps = viewModel.apps,
                            usageGranted = viewModel.hasUsageAccess(),
                            watcherEnabled = isWatcherEnabled(),
                            onCycleTier = viewModel::cycleTier,
                            onGrantUsage = {
                                openSystemSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            },
                            onEnableWatcher = {
                                openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            },
                            onBack = viewModel::goHome,
                        )

                        is Screen.Search -> SearchScreen(
                            query = viewModel.query,
                            resolution = viewModel.resolution,
                            onQueryChange = viewModel::onQueryChange,
                            onSubmit = { viewModel.submit(::launchApp) },
                            onBack = viewModel::goHome,
                        )

                        is Screen.Gate -> GateScreen(
                            app = screen.app,
                            usage = screen.usage,
                            usageAvailable = viewModel.hasUsageAccess(),
                            holdSeconds = viewModel.config.holdSeconds,
                            onPass = { viewModel.passGate(screen.app, ::launchApp) },
                            onDismiss = viewModel::goHome,
                        )
                    }
                }
            }
        }
    }

    /**
     * Pressing home while already home should land you on the void, not on
     * whatever screen you wandered off on. This is the one place a launcher
     * gets to reset someone's state of mind, and it costs nothing.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun launchApp(app: AppEntry) {
        viewModel.launchIntentFor(app)?.let(::startActivity)
    }

    private fun openSystemSettings(action: String) {
        runCatching { startActivity(Intent(action)) }
    }

    /**
     * Read from the system's own list of enabled services rather than from a
     * flag we set ourselves — the user can revoke this in Settings at any time
     * and never tells the app, so any local record of it goes stale silently.
     */
    private fun isWatcherEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        return enabled.split(':').any { it.startsWith("$packageName/") }
    }
}
