package dev.anantpandey.shunya.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shunya")

/**
 * Everything the user has decided, and nothing they have done.
 *
 * Deliberately absent: any history of which apps were opened when. The counts
 * shown in the gate are read live from Android's own [android.app.usage.UsageStatsManager]
 * and never copied here, so uninstalling the launcher takes the record with it.
 * A wellbeing app that accumulates a behavioural profile has quietly become the
 * thing it was built to oppose.
 */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        /** Ordered, so newline-joined rather than a set. */
        val TierZero = stringPreferencesKey("tier_zero")
        val TierTwo = stringSetPreferencesKey("tier_two")
        val HoldSeconds = intPreferencesKey("hold_seconds")
        val FeedInterruptItems = intPreferencesKey("feed_interrupt_items")
        val Seeded = booleanPreferencesKey("seeded")
    }

    val config: Flow<LauncherConfig> = store.data.map { prefs ->
        LauncherConfig(
            tierZero = prefs[Keys.TierZero]
                ?.split('\n')
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            tierTwo = prefs[Keys.TierTwo].orEmpty(),
            holdSeconds = prefs[Keys.HoldSeconds] ?: LauncherConfig.DefaultHoldSeconds,
            feedInterruptItems = prefs[Keys.FeedInterruptItems]
                ?: LauncherConfig.DefaultFeedInterruptItems,
            seeded = prefs[Keys.Seeded] ?: false,
        )
    }

    suspend fun setTierZero(packages: List<String>) = store.edit {
        it[Keys.TierZero] = packages.joinToString("\n")
    }

    suspend fun setTier(packageName: String, tier: Tier) = store.edit { prefs ->
        val zero = prefs[Keys.TierZero]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        val two = prefs[Keys.TierTwo].orEmpty()

        prefs[Keys.TierZero] = when (tier) {
            Tier.ZERO -> (zero + packageName).distinct()
            else -> zero - packageName
        }.joinToString("\n")

        prefs[Keys.TierTwo] = when (tier) {
            Tier.TWO -> two + packageName
            else -> two - packageName
        }
    }

    suspend fun setHoldSeconds(seconds: Int) = store.edit {
        it[Keys.HoldSeconds] = seconds.coerceIn(3, 60)
    }

    suspend fun setFeedInterruptItems(items: Int) = store.edit {
        it[Keys.FeedInterruptItems] = items.coerceIn(3, 200)
    }

    /** Runs once, on first launch, so the app is useful before it is configured. */
    suspend fun seed(tierZero: List<String>, tierTwo: Set<String>) = store.edit { prefs ->
        if (prefs[Keys.Seeded] == true) return@edit
        prefs[Keys.TierZero] = tierZero.joinToString("\n")
        prefs[Keys.TierTwo] = tierTwo
        prefs[Keys.Seeded] = true
    }
}

data class LauncherConfig(
    val tierZero: List<String> = emptyList(),
    val tierTwo: Set<String> = emptySet(),
    val holdSeconds: Int = DefaultHoldSeconds,
    val feedInterruptItems: Int = DefaultFeedInterruptItems,
    val seeded: Boolean = false,
) {
    fun tierOf(packageName: String): Tier = when (packageName) {
        in tierTwo -> Tier.TWO
        in tierZero -> Tier.ZERO
        else -> Tier.Default
    }

    companion object {
        /**
         * Ten seconds. Long enough that you notice you are waiting, short
         * enough that it is not a punishment — the goal is to interrupt an
         * automatic act, not to win an argument with the user.
         */
        const val DefaultHoldSeconds = 10

        /**
         * Reels before the launcher interrupts. The damage is not done in
         * minute one, it is done in minute twenty-five, so the interrupt is
         * mid-scroll rather than only at the door.
         */
        const val DefaultFeedInterruptItems = 20
    }
}
