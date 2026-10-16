package com.mrdastbaaf.mdbapp

import android.app.Application
import com.mrdastbaaf.mdbapp.core.network.ConnectivityObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * The only work done here is starting the connectivity observer, which must run
 * for the whole process lifetime rather than for any one screen: every ViewModel
 * reads `ConnectivityObserver.isOnline` to decide whether to show the offline
 * banner, and that flow is only live while somebody is collecting it.
 *
 * Deliberately minimal otherwise — all wiring lives in Hilt modules. No
 * analytics, no crash reporter and no third-party SDK is initialised: the
 * security brief rules out anything that could exfiltrate a token, a playback
 * ticket or a video URL.
 */
@HiltAndroidApp
class MdbApplication : Application() {

    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    /**
     * A process-scoped scope, deliberately never cancelled.
     *
     * The connectivity registration must outlive every Activity — if it were
     * tied to a screen, the banner would freeze on whatever the value happened
     * to be when that screen was destroyed.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { connectivityObserver.track() }
    }
}
