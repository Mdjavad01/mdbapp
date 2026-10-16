package com.mrdastbaaf.mdbapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.ui.MdbApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single Activity hosting the whole Compose tree.
 *
 * Screen-capture protection (`FLAG_SECURE`) is **not** applied here. It is
 * toggled per screen through [com.mrdastbaaf.mdbapp.core.security.SecureScreen],
 * because only paid-content and player screens must be protected — forcing it
 * app-wide would black out screenshots on the public catalogue too, which the
 * product does not want.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Injected at the Activity level so the session outlives every screen and
     * survives configuration changes. A ViewModel would be recreated on a
     * process restart, which is the wrong lifetime for a credential holder.
     */
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MdbApp(sessionManager = sessionManager)
        }
    }
}
