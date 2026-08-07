package dev.anantpandey.shunya.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.anantpandey.shunya.AppContainer
import dev.anantpandey.shunya.container
import dev.anantpandey.shunya.data.AppEntry
import dev.anantpandey.shunya.data.DailyUsage
import dev.anantpandey.shunya.data.GraceTracker
import dev.anantpandey.shunya.data.LauncherConfig
import dev.anantpandey.shunya.data.Resolution
import dev.anantpandey.shunya.data.Tier
import dev.anantpandey.shunya.data.resolve
import dev.anantpandey.shunya.ui.settings.next
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the launcher currently is. There are only three places it can be. */
sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Settings : Screen
    data class Gate(val app: AppEntry, val usage: DailyUsage) : Screen
}

class LauncherViewModel(private val container: AppContainer) : ViewModel() {

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    var query by mutableStateOf("")
        private set

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    var config by mutableStateOf(LauncherConfig())
        private set

    /** Tier-zero apps, in the order the user put them. */
    val homeApps: List<AppEntry>
        get() = config.tierZero.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }

    val resolution: Resolution
        get() = apps.resolve(query)

    init {
        viewModelScope.launch {
            seedIfFirstRun()
            container.settings.config.collect { latest ->
                config = latest
                apps = withContext(Dispatchers.IO) { container.apps.entries(latest) }
            }
        }
    }

    private suspend fun seedIfFirstRun() {
        if (container.settings.config.first().seeded) return
        withContext(Dispatchers.IO) {
            container.settings.seed(
                tierZero = container.apps.defaultTierZero(),
                tierTwo = container.apps.defaultTierTwo(),
            )
        }
    }

    /** Re-read installed apps. Called on resume, since apps come and go. */
    fun refresh() {
        viewModelScope.launch {
            apps = withContext(Dispatchers.IO) { container.apps.entries(config) }
        }
    }

    fun openSearch() {
        query = ""
        screen = Screen.Search
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    fun cycleTier(app: AppEntry) {
        viewModelScope.launch {
            container.settings.setTier(app.packageName, app.tier.next())
        }
    }

    fun goHome() {
        query = ""
        screen = Screen.Home
    }

    fun onQueryChange(value: String) {
        // Newlines would arrive from an IME "done" action rather than a keystroke.
        query = value.filterNot { it == '\n' }
    }

    /** Enter on the search field. Does nothing unless the query resolves cleanly. */
    fun submit(launch: (AppEntry) -> Unit) {
        when (val r = resolution) {
            is Resolution.Unique -> open(r.app, launch)
            is Resolution.Gated -> openGate(r.app)
            else -> Unit
        }
    }

    fun open(app: AppEntry, launch: (AppEntry) -> Unit) {
        if (app.tier == Tier.TWO && !GraceTracker.isWithinGrace(app.packageName)) {
            openGate(app)
            return
        }
        launch(app)
        goHome()
    }

    private fun openGate(app: AppEntry) {
        viewModelScope.launch {
            val usage = withContext(Dispatchers.IO) { container.usage.today(app.packageName) }
            screen = Screen.Gate(app, usage)
        }
    }

    /** The hold completed. Grants a grace window, then opens. */
    fun passGate(app: AppEntry, launch: (AppEntry) -> Unit) {
        GraceTracker.grant(app.packageName)
        launch(app)
        goHome()
    }

    fun launchIntentFor(app: AppEntry) = container.apps.launchIntent(app.packageName)

    fun hasUsageAccess(): Boolean = container.usage.hasPermission()

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val container = context.container
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = LauncherViewModel(container) as T
            }
        }
    }
}
