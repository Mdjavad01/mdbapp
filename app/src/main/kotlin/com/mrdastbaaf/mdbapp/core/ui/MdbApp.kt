package com.mrdastbaaf.mdbapp.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.mrdastbaaf.mdbapp.core.navigation.MdbNavHost
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.settings.ThemePreference
import com.mrdastbaaf.mdbapp.core.settings.ThemeViewModel
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme

/**
 * The application root.
 *
 * A thin composition of theme + navigation. Anything that needs a lifecycle
 * above a single screen (the session, the offline banner, an update prompt)
 * belongs here, not inside a screen.
 *
 * The theme is resolved here because it is the only place that owns the whole
 * window: [ThemePreference.SYSTEM] follows the device live, while an explicit
 * choice overrides it until the user changes it again.
 *
 * [sessionManager] is injected into `MainActivity` and passed down rather than
 * obtained with `hiltViewModel()`: there is no screen-level ViewModel at this
 * level, and inventing one just to reach the Hilt graph would be noise.
 */
@Composable
fun MdbApp(
    sessionManager: SessionManager,
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val preference by themeViewModel.preference.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    MdbTheme(darkTheme = darkTheme) {
        MdbNavHost(
            sessionManager = sessionManager,
            navController = rememberNavController(),
        )
    }
}
