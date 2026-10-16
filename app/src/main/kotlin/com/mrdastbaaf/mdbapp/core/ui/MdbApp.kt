package com.mrdastbaaf.mdbapp.core.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.mrdastbaaf.mdbapp.core.navigation.MdbNavHost
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme

/**
 * The application root.
 *
 * A thin composition of theme + navigation. Anything that needs a lifecycle
 * above a single screen (the session, the offline banner, an update prompt)
 * belongs here, not inside a screen.
 *
 * [sessionManager] is injected into `MainActivity` and passed down rather than
 * obtained with `hiltViewModel()`: there is no screen-level ViewModel at this
 * level, and inventing one just to reach the Hilt graph would be noise.
 */
@Composable
fun MdbApp(sessionManager: SessionManager) {
    MdbTheme {
        MdbNavHost(
            sessionManager = sessionManager,
            navController = rememberNavController(),
        )
    }
}
