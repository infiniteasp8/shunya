package dev.anantpandey.shunya

import android.app.Application
import android.content.Context
import dev.anantpandey.shunya.data.AppRepository
import dev.anantpandey.shunya.data.SettingsStore
import dev.anantpandey.shunya.data.UsageReader

/**
 * Manual dependency wiring.
 *
 * No Hilt, no Koin. This app has three collaborators and one process; a DI
 * framework here would cost a build-time annotation processor and a layer of
 * indirection to solve a problem it does not have.
 */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val apps = AppRepository(context)
    val usage = UsageReader(context)
}

class ShunyaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as ShunyaApp).container
